package br.com.marcosbassetto.service;

import br.com.marcosbassetto.model.bass.BassConfig;
import br.com.marcosbassetto.model.drum.DrumConfig;
import br.com.marcosbassetto.model.guitar.GuitarConfig;
import br.com.marcosbassetto.model.keyboard.KeyboardConfig;
import br.com.marcosbassetto.model.music.BackingTrack;
import br.com.marcosbassetto.model.music.Instrument;
import br.com.marcosbassetto.model.music.TimeSignatureInfo;

import javax.sound.midi.*;
import javax.swing.*;
import java.io.File;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MidiGenerationService {

    private static final int CHANNEL_HARMONY = 0;
    private static final int PPQ = 480;
    private static final String FULL_MIX_FILE_NAME = "backing_track.mid";

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
        fileChooser.setSelectedFile(new File(FULL_MIX_FILE_NAME));

        int userSelection = fileChooser.showSaveDialog(null);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File outputFile = fileChooser.getSelectedFile();

            if (!outputFile.getName().toLowerCase().endsWith(".mid") && !outputFile.getName().toLowerCase().endsWith(".midi")) {
                outputFile = new File(outputFile.getAbsolutePath() + ".mid");
            }

            try {
                Sequence sequence = createSequence();
                MidiSystem.write(sequence, 1, outputFile);

                Map<Instrument, File> instrumentFiles = writeInstrumentFiles(outputFile);

                StringBuilder message = new StringBuilder("MIDI gerado com sucesso!")
                        .append("\nArquivo completo: ").append(outputFile.getAbsolutePath());
                for (Map.Entry<Instrument, File> entry : instrumentFiles.entrySet()) {
                    message.append("\n").append(entry.getKey().getLabel())
                            .append(": ").append(entry.getValue().getAbsolutePath());
                }
                JOptionPane.showMessageDialog(null, message.toString());
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Erro ao gerar arquivo MIDI: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /** Grava um MIDI por instrumento na mesma pasta do arquivo completo. */
    public Map<Instrument, File> writeInstrumentFiles(File outputFile) throws Exception {
        File directory = outputFile.getAbsoluteFile().getParentFile();
        Map<Instrument, File> files = new EnumMap<>(Instrument.class);

        for (Map.Entry<Instrument, Sequence> entry : createInstrumentSequences().entrySet()) {
            File instrumentFile = new File(directory, entry.getKey().getFileName());
            MidiSystem.write(entry.getValue(), 1, instrumentFile);
            files.put(entry.getKey(), instrumentFile);
        }
        return files;
    }

    /**
     * Monta uma sequência independente por instrumento habilitado, contendo
     * apenas a track do próprio instrumento (mais os metadados de tempo/compasso,
     * sem os quais alguns players não tocam o arquivo corretamente).
     */
    public Map<Instrument, Sequence> createInstrumentSequences() throws InvalidMidiDataException {
        TimeSignatureInfo timeInfo = TimeSignatureInfo.parse(backingTrack.getMeasure());
        int bpm = parseBpm(backingTrack.getBpm());
        int totalSeconds = (parseDuration(backingTrack.getDurationMinutes()) * 60) + parseDuration(backingTrack.getDurationSeconds());
        long totalTicks = ticksForTotalDuration(bpm, totalSeconds);

        Map<Instrument, Sequence> sequences = new EnumMap<>(Instrument.class);
        for (Instrument instrument : Instrument.values()) {
            if (!isEnabled(instrument)) {
                continue;
            }
            Sequence sequence = new Sequence(Sequence.PPQ, PPQ);
            Track controlTrack = sequence.createTrack();
            setTempo(controlTrack, bpm);
            writeTimeSignatureEvent(controlTrack, timeInfo);
            appendInstrumentTrack(sequence, instrument, timeInfo, totalTicks);
            sequences.put(instrument, sequence);
        }
        return sequences;
    }

    private Set<Instrument> enabledInstruments() {
        Set<Instrument> enabled = EnumSet.noneOf(Instrument.class);
        if (enableGuitar) enabled.add(Instrument.GUITAR);
        if (enableBass) enabled.add(Instrument.BASS);
        if (enableKeyboard) enabled.add(Instrument.KEYBOARD);
        if (enableDrums) enabled.add(Instrument.DRUMS);
        return enabled;
    }

    private boolean isEnabled(Instrument instrument) {
        return enabledInstruments().contains(instrument);
    }

    public Sequence createSequence() throws InvalidMidiDataException {
        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);

        TimeSignatureInfo timeInfo = TimeSignatureInfo.parse(backingTrack.getMeasure());

        Track controlTrack = sequence.createTrack();

        int bpm = parseBpm(backingTrack.getBpm());
        int totalSeconds = (parseDuration(backingTrack.getDurationMinutes()) * 60) + parseDuration(backingTrack.getDurationSeconds());

        setTempo(controlTrack, bpm);
        writeTimeSignatureEvent(controlTrack, timeInfo);

        ShortMessage programChange = new ShortMessage();
        programChange.setMessage(ShortMessage.PROGRAM_CHANGE, CHANNEL_HARMONY, 0, 0);
        controlTrack.add(new MidiEvent(programChange, 0));

        long totalTicks = ticksForTotalDuration(bpm, totalSeconds);

        addHarmonyTrack(sequence, backingTrack, timeInfo, totalTicks);
        for (Instrument instrument : Instrument.values()) {
            if (isEnabled(instrument)) {
                appendInstrumentTrack(sequence, instrument, timeInfo, totalTicks);
            }
        }

        return sequence;
    }

    private long ticksForTotalDuration(int bpm, int totalSeconds) {
        double totalMinutes = totalSeconds / 60.0;
        return (long) (totalMinutes * bpm * PPQ);
    }

    /** Gera a track do instrumento como uma nova track da sequência informada. */
    private void appendInstrumentTrack(Sequence sequence, Instrument instrument,
                                       TimeSignatureInfo timeInfo,
                                       long totalTicks) throws InvalidMidiDataException {
        Track track = sequence.createTrack();
        switch (instrument) {
            case GUITAR -> {
                guitarConfig.syncPatternTo(timeInfo);
                new GuitarMidiService(guitarConfig, timeInfo)
                        .generateGuitarTrack(sequence, backingTrack, totalTicks, PPQ, track);
            }
            case BASS -> {
                BassConfig effectiveBassConfig = bassConfig != null ? bassConfig : new BassConfig(timeInfo);
                effectiveBassConfig.syncToTimeSignature(timeInfo);
                new BassMidiService(effectiveBassConfig, timeInfo)
                        .generateBassTrack(sequence, backingTrack, totalTicks, PPQ, track);
            }
            case KEYBOARD -> {
                KeyboardConfig effectiveKeyboardConfig =
                        keyboardConfig != null ? keyboardConfig : new KeyboardConfig(timeInfo);
                effectiveKeyboardConfig.syncToTimeSignature(timeInfo);
                new KeyboardMidiService(effectiveKeyboardConfig, timeInfo)
                        .generateKeyboardTrack(sequence, backingTrack, totalTicks, PPQ, track);
            }
            case DRUMS -> drumMidiService.generateDrumTrack(track, drumConfig, PPQ, totalTicks);
        }
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
