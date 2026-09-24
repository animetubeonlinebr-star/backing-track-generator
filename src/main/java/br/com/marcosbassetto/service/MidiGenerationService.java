package br.com.marcosbassetto.service;

import br.com.marcosbassetto.model.bass.BassConfig;
import br.com.marcosbassetto.model.drum.DrumConfig;
import br.com.marcosbassetto.model.guitar.GuitarConfig;
import br.com.marcosbassetto.model.keyboard.KeyboardConfig;
import br.com.marcosbassetto.model.music.BackingTrack;
import br.com.marcosbassetto.model.music.TimeSignatureInfo;

import javax.sound.midi.*;
import javax.swing.*;
import java.io.File;
import java.util.List;

public class MidiGenerationService {

    private static final int CHANNEL_HARMONY = 0;
    private static final int PPQ = 480;

    private final BackingTrack backingTrack;
    private final DrumConfig drumConfig;
    private final DrumMidiService drumMidiService;
    private final ChordService chordService;
    private final GuitarConfig guitarConfig;
    private final BassConfig bassConfig;
    private final KeyboardConfig keyboardConfig;
    private final boolean enableDrums;
    private final boolean enableGuitar;
    private final boolean enableBass;
    private final boolean enableKeyboard;

    public MidiGenerationService(BackingTrack backingTrack, DrumConfig drumConfig,
                                 GuitarConfig guitarConfig, BassConfig bassConfig,
                                 KeyboardConfig keyboardConfig,
                                 boolean enableDrums, boolean enableGuitar,
                                 boolean enableBass, boolean enableKeyboard) {
        this.backingTrack = backingTrack;
        this.drumConfig = drumConfig;
        this.drumMidiService = new DrumMidiService();
        this.chordService = new ChordService();
        this.guitarConfig = guitarConfig != null ? guitarConfig : new GuitarConfig();
        this.bassConfig = bassConfig;
        this.keyboardConfig = keyboardConfig;
        this.enableDrums = enableDrums;
        this.enableGuitar = enableGuitar;
        this.enableBass = enableBass;
        this.enableKeyboard = enableKeyboard;
    }

    public void generate() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Salvar Arquivo MIDI");
        fileChooser.setSelectedFile(new File("backing_track.mid"));

        int userSelection = fileChooser.showSaveDialog(null);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File outputFile = fileChooser.getSelectedFile();

            if (!outputFile.getName().toLowerCase().endsWith(".mid") && !outputFile.getName().toLowerCase().endsWith(".midi")) {
                outputFile = new File(outputFile.getAbsolutePath() + ".mid");
            }

            try {
                Sequence sequence = createSequence();
                MidiSystem.write(sequence, 1, outputFile);
                JOptionPane.showMessageDialog(null, "MIDI gerado com sucesso!\nSalvo em: " + outputFile.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Erro ao gerar arquivo MIDI: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public Sequence createSequence() throws InvalidMidiDataException {
        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);

        TimeSignatureInfo timeInfo = TimeSignatureInfo.parse(backingTrack.getMeasure());

        Track controlTrack = sequence.createTrack();
        Track drumTrack = sequence.createTrack();

        int bpm = parseBpm(backingTrack.getBpm());
        int totalSeconds = (parseDuration(backingTrack.getDurationMinutes()) * 60) + parseDuration(backingTrack.getDurationSeconds());

        setTempo(controlTrack, bpm);
        writeTimeSignatureEvent(controlTrack, timeInfo);

        ShortMessage programChange = new ShortMessage();
        programChange.setMessage(ShortMessage.PROGRAM_CHANGE, CHANNEL_HARMONY, 0, 0);
        controlTrack.add(new MidiEvent(programChange, 0));

        double totalMinutes = totalSeconds / 60.0;
        long totalTicks = (long) (totalMinutes * bpm * PPQ);

        addHarmonyTrack(sequence, backingTrack, timeInfo, totalTicks);
        if (enableDrums) {
            drumMidiService.generateDrumTrack(drumTrack, drumConfig, PPQ, totalTicks);
        }
        if (enableGuitar) {
            guitarConfig.syncPatternTo(timeInfo);
            new GuitarMidiService(guitarConfig, timeInfo)
                    .generateGuitarTrack(sequence, backingTrack, totalTicks, PPQ);
        }
        if (enableBass) {
            BassConfig effectiveBassConfig = bassConfig != null ? bassConfig : new BassConfig(timeInfo);
            effectiveBassConfig.syncToTimeSignature(timeInfo);
            new BassMidiService(effectiveBassConfig, timeInfo)
                    .generateBassTrack(sequence, backingTrack, totalTicks, PPQ);
        }
        if (enableKeyboard) {
            KeyboardConfig effectiveKeyboardConfig =
                    keyboardConfig != null ? keyboardConfig : new KeyboardConfig(timeInfo);
            effectiveKeyboardConfig.syncToTimeSignature(timeInfo);
            new KeyboardMidiService(effectiveKeyboardConfig, timeInfo)
                    .generateKeyboardTrack(sequence, backingTrack, totalTicks, PPQ);
        }

        return sequence;
    }

    private void addHarmonyTrack(Sequence sequence, BackingTrack backingTrack, TimeSignatureInfo timeInfo, long totalTicks) {
        Track track = sequence.createTrack();
        String[] chords = backingTrack.getProgression().split("\\s*-\\s*");
        long measureTicks = timeInfo.getMeasureTicks(PPQ);
        long currentTick = 0;

        int chordIndex = 0;
        while (currentTick < totalTicks) {
            String chordSymbol = chords[chordIndex % chords.length];
            List<Integer> notes = chordService.getMidiNotes(chordSymbol);

            for (int note : notes) {
                addNoteEvent(track, ShortMessage.NOTE_ON, CHANNEL_HARMONY, note, 70, currentTick);
                addNoteEvent(track, ShortMessage.NOTE_OFF, CHANNEL_HARMONY, note, 0, currentTick + measureTicks - 10);
            }

            currentTick += measureTicks;
            chordIndex++;
        }
    }

    private void addNoteEvent(Track track, int command, int channel, int note, int velocity, long tick) {
        try {
            ShortMessage message = new ShortMessage(command, channel, note, velocity);
            track.add(new MidiEvent(message, tick));
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }
    }

    public void writeTimeSignatureEvent(Track track, TimeSignatureInfo timeInfo) {
        try {
            int numerator = timeInfo.numerator();
            int denominator = timeInfo.denominator();

            int denominatorPower = (int) (Math.log(denominator) / Math.log(2));

            byte[] data = new byte[] {
                    (byte) numerator,
                    (byte) denominatorPower,
                    24,
                    8
            };

            MetaMessage metaMessage = new MetaMessage(0x58, data, data.length);
            track.add(new MidiEvent(metaMessage, 0));
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }
    }

    private void setTempo(Track track, int bpm) throws InvalidMidiDataException {
        int mpqn = 60_000_000 / bpm;
        byte[] data = new byte[] {
                (byte) ((mpqn >> 16) & 0xFF),
                (byte) ((mpqn >> 8) & 0xFF),
                (byte) (mpqn & 0xFF)
        };
        MetaMessage tempoMsg = new MetaMessage(0x51, data, 3);
        track.add(new MidiEvent(tempoMsg, 0));
    }

    private int parseBpm(String bpmStr) {
        try { return Integer.parseInt(bpmStr.trim()); }
        catch (Exception e) { return 120; }
    }

    private int parseDuration(String durStr) {
        try { return Integer.parseInt(durStr.trim()); }
        catch (Exception e) { return 0; }
    }
}
