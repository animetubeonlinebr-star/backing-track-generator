package br.com.marcosbassetto.service;

import br.com.marcosbassetto.model.guitar.GuitarConfig;
import br.com.marcosbassetto.model.guitar.GuitarRhythmPattern;
import br.com.marcosbassetto.model.guitar.GuitarRhythmPattern.AttackType;
import br.com.marcosbassetto.model.music.BackingTrack;
import br.com.marcosbassetto.model.music.TimeSignatureInfo;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import java.util.ArrayList;
import java.util.List;

/**
 * Gera a trilha de guitarra rítmica. A lógica é "para cada passo com ataque,
 * toque o acorde da forma certa" — o padrão rítmico define QUANDO, o
 * {@link VoicingService} define O QUÊ (registro e voice leading).
 */
public class GuitarMidiService {

    private static final int CHANNEL_GUITAR = 2;
    private static final int NOTE_OFF_GAP_TICKS = 5;

    private final ChordService chordService = new ChordService();
    private final GuitarConfig config;
    private final TimeSignatureInfo timeInfo;
    private final VelocityHumanizer humanizer;

    public GuitarMidiService(GuitarConfig config, TimeSignatureInfo timeInfo) {
        this(config, timeInfo, new VelocityHumanizer());
    }

    public GuitarMidiService(GuitarConfig config, TimeSignatureInfo timeInfo,
                             VelocityHumanizer humanizer) {
        this.config = config != null ? config : new GuitarConfig();
        this.timeInfo = timeInfo;
        this.humanizer = humanizer != null ? humanizer : new VelocityHumanizer();
    }

    public void generateGuitarTrack(Sequence sequence, BackingTrack backingTrack,
                                    long totalTicks, int ppq) throws InvalidMidiDataException {
        generateGuitarTrack(sequence, backingTrack, totalTicks, ppq, sequence.createTrack());
    }

    /**
     * Gera a track de guitarra dentro de uma track já existente. Usado tanto pelo
     * arquivo único (track própria) quanto pelo MIDI individual do instrumento.
     */
    public void generateGuitarTrack(Sequence sequence, BackingTrack backingTrack,
                                    long totalTicks, int ppq, Track track)
            throws InvalidMidiDataException {

        setProgramChange(track, CHANNEL_GUITAR, config.getProgramChange(), 0);

        String[] chords = backingTrack.getProgression().split("\\s*-\\s*");
        long measureTicks = timeInfo.getMeasureTicks(ppq);
        long stepTicks = timeInfo.getStepTicks(ppq);
        int stepsPerBeat = timeInfo.stepsPerBeat();
        GuitarRhythmPattern pattern = config.getPattern();

        List<Integer> previousVoicing = null;
        long currentMeasureTick = 0;
        int chordIndex = 0;

        while (currentMeasureTick < totalTicks) {
            List<Integer> rawNotes = chordService.getMidiNotes(chords[chordIndex % chords.length]);
            List<Integer> voicing = VoicingService.forGuitar(rawNotes, previousVoicing, config.getRegister());
            previousVoicing = voicing;

            for (int step = 0; step < pattern.getTotalSteps(); step++) {
                AttackType attack = pattern.getAttack(step);
                if (attack == AttackType.NONE) {
                    continue;
                }

                long attackTick = currentMeasureTick + ((long) step * stepTicks);
                if (attackTick >= totalTicks) {
                    break;
                }

                switch (attack) {
                    case STRUM_DOWN -> playStrum(track, voicing, attackTick, stepTicks, true,
                            step, stepsPerBeat);
                    case STRUM_UP -> playStrum(track, voicing, attackTick, stepTicks, false,
                            step, stepsPerBeat);
                    case PICK -> playPick(track, voicing, attackTick, stepTicks, step, stepsPerBeat);
                    case NONE -> { }
                }
            }

            currentMeasureTick += measureTicks;
            chordIndex++;
        }
    }

    /** Batida: notas em sequência, graves primeiro (down) ou agudos primeiro (up). */
    private void playStrum(Track track, List<Integer> notes, long startTick,
                           long stepTicks, boolean down, int step, int stepsPerBeat) {
        List<Integer> ordered = down ? notes : reversed(notes);
        int meanVelocity = down ? config.getVelocityDown() : config.getVelocityUp();
        int stepAccent = humanizer.accentForStep(step, stepsPerBeat);
        long noteOffTick = startTick + stepTicks - NOTE_OFF_GAP_TICKS;

        for (int i = 0; i < ordered.size(); i++) {
            int note = ordered.get(i);
            // O índice na ordem de execução não é a ordem de altura; inverte no up
            // para o acento de fundamental recair sempre sobre a nota grave.
            int pitchIndex = down ? i : ordered.size() - 1 - i;
            int velocity = humanizer.humanize(meanVelocity,
                    stepAccent + humanizer.accentForChordTone(pitchIndex, ordered.size()));
            long tick = startTick + ((long) i * config.getStrumOffsetTicks());
            addNoteEvent(track, ShortMessage.NOTE_ON, CHANNEL_GUITAR, note, velocity, tick);
            addNoteEvent(track, ShortMessage.NOTE_OFF, CHANNEL_GUITAR, note, 0,
                    Math.max(tick + 1, noteOffTick));
        }
    }

    /** Dedilhado: uma nota por vez, em sequência dentro do passo. */
    private void playPick(Track track, List<Integer> notes, long startTick, long stepTicks,
                          int step, int stepsPerBeat) {
        if (notes.isEmpty()) {
            return;
        }
        int meanVelocity = config.getVelocityPick();
        int stepAccent = humanizer.accentForStep(step, stepsPerBeat);
        long subStep = Math.max(1, stepTicks / notes.size());
        for (int i = 0; i < notes.size(); i++) {
            int note = notes.get(i);
            int velocity = humanizer.humanize(meanVelocity,
                    stepAccent + humanizer.accentForChordTone(i, notes.size()));
            long tick = startTick + ((long) i * subStep);
            addNoteEvent(track, ShortMessage.NOTE_ON, CHANNEL_GUITAR, note, velocity, tick);
            addNoteEvent(track, ShortMessage.NOTE_OFF, CHANNEL_GUITAR, note, 0,
                    Math.max(tick + 1, tick + subStep - NOTE_OFF_GAP_TICKS));
        }
    }

    private static List<Integer> reversed(List<Integer> notes) {
        List<Integer> result = new ArrayList<>(notes);
        java.util.Collections.reverse(result);
        return result;
    }

    private void addNoteEvent(Track track, int command, int channel, int note,
                              int velocity, long tick) {
        if (note < 0 || note > 127 || tick < 0) {
            return;
        }
        try {
            ShortMessage message = new ShortMessage(command, channel, note, velocity);
            track.add(new MidiEvent(message, tick));
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }
    }

    private void setProgramChange(Track track, int channel, int program, long tick) {
        try {
            ShortMessage programChange = new ShortMessage(
                    ShortMessage.PROGRAM_CHANGE, channel, program, 0);
            track.add(new MidiEvent(programChange, tick));
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }
    }
}
