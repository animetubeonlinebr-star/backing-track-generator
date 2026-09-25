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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Gera os arquivos MIDI do backing track.
 *
 * <p>A exportação é individual: um arquivo por instrumento habilitado, com nome
 * fixo ({@code bass.midi}, {@code guitar.midi}, {@code keyboard.midi},
 * {@code drums.midi}). Como os arquivos são complementares e devem formar o
 * mesmo backing track quando abertos juntos, cada um carrega o mesmo cabeçalho
 * (BPM e fórmula de compasso) e a mesma quantidade de ticks, derivados de um
 * único {@link BackingTrack}. Essa consistência é o que faz os arquivos
 * começarem e terminarem alinhados.
 *
 * <p>O mix completo continua disponível em {@link #createSequence()}; os
 * arquivos individuais saem de {@link #createInstrumentSequence(Instrument)}.
 */
public class MidiGenerationService {

    private static final int CHANNEL_HARMONY = 0;
    private static final int PPQ = 480;

    /** Instrumentos exportáveis, cada um com o nome de arquivo padrão. */
    public enum Instrument {
        BASS("bass.midi"),
        GUITAR("guitar.midi"),
        KEYBOARD("keyboard.midi"),
        DRUMS("drums.midi");

        private final String fileName;

        Instrument(String fileName) {
            this.fileName = fileName;
        }

        public String getFileName() {
            return fileName;
        }
    }

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

    /**
     * Pede a pasta de destino e grava um arquivo por instrumento habilitado.
     * A escolha é de diretório, não de arquivo: os nomes são fixos.
     */
    public void generate() {
        JFileChooser directoryChooser = new JFileChooser();
        directoryChooser.setDialogTitle("Escolha a pasta para salvar os arquivos MIDI");
        directoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        directoryChooser.setAcceptAllFileFilterUsed(false);

        if (directoryChooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File directory = directoryChooser.getSelectedFile();

        try {
            List<File> generated = generateTo(directory);
            if (generated.isEmpty()) {
                JOptionPane.showMessageDialog(null,
                        "Nenhum instrumento selecionado. Marque ao menos um para gerar.");
                return;
            }
            JOptionPane.showMessageDialog(null, buildSuccessMessage(directory, generated));
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Erro ao gerar os arquivos MIDI: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Grava um arquivo por instrumento habilitado, com o nome padrão de cada um,
     * reutilizando o mesmo cabeçalho de tempo/compasso. Devolve os arquivos
     * escritos, na ordem em que foram gerados.
     */
    public List<File> generateTo(File directory) throws IOException, InvalidMidiDataException {
        ensureDirectory(directory);

        List<File> written = new ArrayList<>();
        for (Instrument instrument : Instrument.values()) {
            if (!isEnabled(instrument)) {
                continue;
            }
            File outputFile = new File(directory, instrument.getFileName());
            MidiSystem.write(createInstrumentSequence(instrument), 1, outputFile);
            written.add(outputFile);
        }
        return written;
    }

    private static void ensureDirectory(File directory) throws IOException {
        if (directory == null) {
            throw new IOException("Pasta de destino não informada.");
        }
        if (directory.exists() && !directory.isDirectory()) {
            throw new IOException("O caminho informado não é uma pasta: " + directory);
        }
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Não foi possível criar a pasta: " + directory);
        }
    }

    private boolean isEnabled(Instrument instrument) {
        return switch (instrument) {
            case BASS -> enableBass;
            case GUITAR -> enableGuitar;
            case KEYBOARD -> enableKeyboard;
            case DRUMS -> enableDrums;
        };
    }

    private static String buildSuccessMessage(File directory, List<File> generated) {
        StringBuilder message = new StringBuilder("MIDI gerado com sucesso!\n\nPasta: ")
                .append(directory.getAbsolutePath())
                .append("\n\nArquivos:\n");
        for (File file : generated) {
            message.append("• ").append(file.getName()).append('\n');
        }
        return message.toString();
    }

    /**
     * Mix completo: harmonia (sempre presente) mais todos os instrumentos
     * habilitados, tudo num único {@link Sequence}. Também é a base do cabeçalho
     * compartilhado pelos arquivos individuais.
     */
    public Sequence createSequence() throws InvalidMidiDataException {
        Sequence sequence = createBaseSequence();

        TimeSignatureInfo timeInfo = TimeSignatureInfo.parse(backingTrack.getMeasure());
        long totalTicks = totalTicks(timeInfo);

        addHarmonyTrack(sequence, backingTrack, timeInfo, totalTicks);
        if (enableDrums) {
            drumMidiService.generateDrumTrack(sequence.createTrack(), drumConfig, PPQ, totalTicks);
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

    /**
     * Sequence de um único instrumento, com o cabeçalho de tempo/compasso e a
     * duração iguais aos do mix. Graças a esse cabeçalho comum, os arquivos
     * individuais podem ser abertos juntos como um mesmo backing track.
     */
    public Sequence createInstrumentSequence(Instrument instrument) throws InvalidMidiDataException {
        Sequence sequence = createBaseSequence();

        TimeSignatureInfo timeInfo = TimeSignatureInfo.parse(backingTrack.getMeasure());
        long totalTicks = totalTicks(timeInfo);

        switch (instrument) {
            case BASS -> {
                BassConfig effectiveBassConfig = bassConfig != null ? bassConfig : new BassConfig(timeInfo);
                effectiveBassConfig.syncToTimeSignature(timeInfo);
                new BassMidiService(effectiveBassConfig, timeInfo)
                        .generateBassTrack(sequence, backingTrack, totalTicks, PPQ);
            }
            case GUITAR -> {
                guitarConfig.syncPatternTo(timeInfo);
                new GuitarMidiService(guitarConfig, timeInfo)
                        .generateGuitarTrack(sequence, backingTrack, totalTicks, PPQ);
            }
            case KEYBOARD -> {
                KeyboardConfig effectiveKeyboardConfig =
                        keyboardConfig != null ? keyboardConfig : new KeyboardConfig(timeInfo);
                effectiveKeyboardConfig.syncToTimeSignature(timeInfo);
                new KeyboardMidiService(effectiveKeyboardConfig, timeInfo)
                        .generateKeyboardTrack(sequence, backingTrack, totalTicks, PPQ);
            }
            case DRUMS -> drumMidiService.generateDrumTrack(
                    sequence.createTrack(), drumConfig, PPQ, totalTicks);
        }

        return sequence;
    }

    /**
     * Cria a sequence com o cabeçalho comum: faixa de controle com tempo e
     * fórmula de compasso. Todos os arquivos partem daqui, o que garante BPM,
     * compasso e quantidade de ticks idênticos, indispensáveis para que os
     * arquivos individuais soem alinhados quando abertos juntos.
     */
    private Sequence createBaseSequence() throws InvalidMidiDataException {
        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);

        TimeSignatureInfo timeInfo = TimeSignatureInfo.parse(backingTrack.getMeasure());
        Track controlTrack = sequence.createTrack();

        setTempo(controlTrack, parseBpm(backingTrack.getBpm()));
        writeTimeSignatureEvent(controlTrack, timeInfo);

        return sequence;
    }

    /** Total de ticks derivado de BPM e duração do formulário principal. */
    private long totalTicks(TimeSignatureInfo timeInfo) {
        int bpm = parseBpm(backingTrack.getBpm());
        int totalSeconds = (parseDuration(backingTrack.getDurationMinutes()) * 60)
                + parseDuration(backingTrack.getDurationSeconds());
        double totalMinutes = totalSeconds / 60.0;
        return (long) (totalMinutes * bpm * PPQ);
    }

    private void addHarmonyTrack(Sequence sequence, BackingTrack backingTrack, TimeSignatureInfo timeInfo, long totalTicks) {
        Track track = sequence.createTrack();
        String[] chords = backingTrack.getProgression().split("\\s*-\\s*");
        long measureTicks = timeInfo.getMeasureTicks(PPQ);

        ShortMessage programChange = new ShortMessage();
        try {
            programChange.setMessage(ShortMessage.PROGRAM_CHANGE, CHANNEL_HARMONY, 0, 0);
            track.add(new MidiEvent(programChange, 0));
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }
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
