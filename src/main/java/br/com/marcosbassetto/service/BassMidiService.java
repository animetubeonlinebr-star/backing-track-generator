package br.com.marcosbassetto.service;

import br.com.marcosbassetto.model.bass.BassConfig;
import br.com.marcosbassetto.model.bass.BassRhythmPattern;
import br.com.marcosbassetto.model.music.BackingTrack;
import br.com.marcosbassetto.model.music.TimeSignatureInfo;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import java.util.List;

/**
 * Geração da trilha de baixo MIDI.
 *
 * <p>O baixo é monofônico e não faz voice leading: em cada compasso ele extrai
 * a fundamental do acorde via {@link VoicingService#forBass} e dispara os graus
 * (fundamental, quinta, oitava) nos momentos definidos pelo
 * {@link BassRhythmPattern}.
 */
public class BassMidiService {

    private static final int CHANNEL_BASS = 1;
    private static final int MIN_NOTE_TICKS = 1;

    private final ChordService chordService = new ChordService();
    private final BassConfig config;
    private final TimeSignatureInfo timeInfo;
    private final VelocityHumanizer humanizer;

    public BassMidiService(BassConfig config, TimeSignatureInfo timeInfo) {
        this(config, timeInfo, new VelocityHumanizer());
    }

    public BassMidiService(BassConfig config, TimeSignatureInfo timeInfo,
                           VelocityHumanizer humanizer) {
        this.config = config != null ? config : new BassConfig(timeInfo);
        this.timeInfo = timeInfo;
        this.humanizer = humanizer != null ? humanizer : new VelocityHumanizer();
    }

    /**
     * Adiciona uma track de baixo à sequência.
     *
     * @param sequence     sequência destino
     * @param backingTrack progressão, BPM, compasso e duração
     * @param totalTicks   duração total em ticks
     * @param ppq          pulsos por semínima (480 no projeto)
     * @throws InvalidMidiDataException se a sequência não aceitar os eventos
     */
    public void generateBassTrack(Sequence sequence, BackingTrack backingTrack,
                                  long totalTicks, int ppq) throws InvalidMidiDataException {
        generateBassTrack(sequence, backingTrack, totalTicks, ppq, sequence.createTrack());
    }

    /**
     * Gera a track de baixo dentro de uma track já existente. Usado tanto pelo
     * arquivo único (track própria) quanto pelo MIDI individual do instrumento.
     */
    public void generateBassTrack(Sequence sequence, BackingTrack backingTrack,
                                  long totalTicks, int ppq, Track track)
            throws InvalidMidiDataException {

        setProgramChange(track, CHANNEL_BASS, config.getProgramChange(), 0);

        String[] chords = backingTrack.getProgression().split("\\s*-\\s*");
        long measureTicks = timeInfo.getMeasureTicks(ppq);
        long stepTicks = timeInfo.getStepTicks(ppq);
        long noteDuration = Math.max(MIN_NOTE_TICKS,
                (stepTicks * config.getNoteDurationPercent()) / 100);

        BassRhythmPattern pattern = config.getPattern();
        int stepsPerBeat = timeInfo.stepsPerBeat();

        long currentMeasureTick = 0;
        int chordIndex = 0;

        while (currentMeasureTick < totalTicks) {
            String chordSymbol = chords[chordIndex % chords.length];
            List<Integer> rawNotes = chordService.getMidiNotes(chordSymbol);

            // Baixo não guarda estado entre compassos — só a fundamental importa.
            int root = VoicingService.forBass(rawNotes, config.getRegister()).get(0);
            int third = VoicingService.bassThird(root, rawNotes, config.getRegister());
            int fifth = VoicingService.bassFifth(root, config.getRegister());
            int sixth = VoicingService.bassSixth(root, rawNotes, config.getRegister());
            int seventh = VoicingService.bassSeventh(root, rawNotes, config.getRegister());
            int octave = VoicingService.bassOctave(root, config.getRegister());

            for (int step = 0; step < pattern.getTotalSteps(); step++) {
                BassRhythmPattern.BassNoteType type = pattern.getNoteType(step);
                if (type == BassRhythmPattern.BassNoteType.NONE) {
                    continue;
                }

                long tick = currentMeasureTick + ((long) step * stepTicks);
                if (tick >= totalTicks) {
                    break;
                }

                int note = switch (type) {
                    case ROOT -> root;
                    case THIRD -> third;
                    case FIFTH -> fifth;
                    case SIXTH -> sixth;
                    case SEVENTH -> seventh;
                    case OCTAVE -> octave;
                    case NONE -> root;
                };

                int accent = humanizer.accentForStep(step, stepsPerBeat)
                        + humanizer.accentForChordTone(degreeIndex(type), 3);
                addNoteEvent(track, ShortMessage.NOTE_ON, CHANNEL_BASS,
                        note, humanizer.humanize(config.getVelocity(), accent), tick);
                addNoteEvent(track, ShortMessage.NOTE_OFF, CHANNEL_BASS,
                        note, 0, tick + noteDuration);
            }

            currentMeasureTick += measureTicks;
            chordIndex++;
        }
    }

    /** Posição do grau no acorde: fundamental mais grave, passagens mais agudas. */
    private static int degreeIndex(BassRhythmPattern.BassNoteType type) {
        return switch (type) {
            case ROOT -> 0;
            case THIRD -> 1;
            case FIFTH -> 2;
            case SIXTH, SEVENTH -> 3;
            case OCTAVE, NONE -> 4;
        };
    }

    private void addNoteEvent(Track track, int command, int channel, int note,
                              int velocity, long tick) {
        if (note < 0 || note > 127 || tick < 0) {
            return;
        }
        try {
            track.add(new MidiEvent(new ShortMessage(command, channel, note, velocity), tick));
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }
    }

    private void setProgramChange(Track track, int channel, int program, long tick) {
        try {
            track.add(new MidiEvent(
                    new ShortMessage(ShortMessage.PROGRAM_CHANGE, channel, program, 0), tick));
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }
    }
}
