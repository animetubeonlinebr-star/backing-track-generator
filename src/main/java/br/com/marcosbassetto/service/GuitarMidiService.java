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

    public GuitarMidiService(GuitarConfig config, TimeSignatureInfo timeInfo) {
        this.config = config != null ? config : new GuitarConfig();
        this.timeInfo = timeInfo;
    }

    public void generateGuitarTrack(Sequence sequence, BackingTrack backingTrack,
                                    long totalTicks, int ppq) throws InvalidMidiDataException {

        Track track = sequence.createTrack();
        setProgramChange(track, CHANNEL_GUITAR, config.getProgramChange(), 0);

        String[] chords = backingTrack.getProgression().split("\\s*-\\s*");
        long measureTicks = timeInfo.getMeasureTicks(ppq);
        long stepTicks = timeInfo.getStepTicks(ppq);
        GuitarRhythmPattern pattern = config.getPattern();

        List<Integer> previousVoicing = null;
        long currentMeasureTick = 0;
        int chordIndex = 0;

        while (currentMeasureTick < totalTicks) {
            List<Integer> rawNotes = chordService.getMidiNotes(chords[chordIndex % chords.length]);
            List<Integer> voicing = VoicingService.forGuitar(rawNotes, previousVoicing);
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
                    case STRUM_DOWN -> playStrum(track, voicing, attackTick, stepTicks, true);
                    case STRUM_UP -> playStrum(track, voicing, attackTick, stepTicks, false);
                    case PICK -> playPick(track, voicing, attackTick, stepTicks);
                    case NONE -> { }
                }
            }

            currentMeasureTick += measureTicks;
            chordIndex++;
        }
    }

    /** Batida: notas em sequência, graves primeiro (down) ou agudos primeiro (up). */
    private void playStrum(Track track, List<Integer> notes, long startTick,
                           long stepTicks, boolean down) {
        List<Integer> ordered = down ? notes : reversed(notes);
        int velocity = down ? config.getVelocityDown() : config.getVelocityUp();
        long noteOffTick = startTick + stepTicks - NOTE_OFF_GAP_TICKS;

        for (int i = 0; i < ordered.size(); i++) {
            int note = ordered.get(i);
            long tick = startTick + ((long) i * config.getStrumOffsetTicks());
            addNoteEvent(track, ShortMessage.NOTE_ON, CHANNEL_GUITAR, note, velocity, tick);
            addNoteEvent(track, ShortMessage.NOTE_OFF, CHANNEL_GUITAR, note, 0,
                    Math.max(tick + 1, noteOffTick));
        }
    }

    /** Dedilhado: uma nota por vez, em sequência dentro do passo. */
    private void playPick(Track track, List<Integer> notes, long startTick, long stepTicks) {
        if (notes.isEmpty()) {
            return;
        }
        long subStep = Math.max(1, stepTicks / notes.size());
        for (int i = 0; i < notes.size(); i++) {
            int note = notes.get(i);
            long tick = startTick + ((long) i * subStep);
            addNoteEvent(track, ShortMessage.NOTE_ON, CHANNEL_GUITAR, note,
                    config.getVelocityPick(), tick);
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
