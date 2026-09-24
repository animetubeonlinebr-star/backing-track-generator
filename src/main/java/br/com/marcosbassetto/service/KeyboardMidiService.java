package br.com.marcosbassetto.service;

import br.com.marcosbassetto.model.keyboard.KeyboardConfig;
import br.com.marcosbassetto.model.keyboard.KeyboardRhythmPattern;
import br.com.marcosbassetto.model.keyboard.KeyboardRhythmPattern.AttackType;
import br.com.marcosbassetto.model.music.BackingTrack;
import br.com.marcosbassetto.model.music.TimeSignatureInfo;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Geração da trilha de teclado MIDI.
 *
 * <p>O teclado é o irmão harmônico da guitarra: usa o mesmo voice leading do
 * {@link VoicingService}, mas toca acordes em bloco ou arpejos e pode sustentar
 * com o pedal (CC 64).
 */
public class KeyboardMidiService {

    private static final int CHANNEL_KEYBOARD = 3;
    private static final int CC_SUSTAIN = 64;
    private static final int SUSTAIN_ON = 127;
    private static final int SUSTAIN_OFF = 0;
    private static final int SUSTAIN_GAP_TICKS = 5;
    private static final int MIN_NOTE_TICKS = 1;

    private final ChordService chordService = new ChordService();
    private final KeyboardConfig config;
    private final TimeSignatureInfo timeInfo;

    public KeyboardMidiService(KeyboardConfig config, TimeSignatureInfo timeInfo) {
        this.config = config != null ? config : new KeyboardConfig(timeInfo);
        this.timeInfo = timeInfo;
    }

    /**
     * Adiciona uma track de teclado à sequência.
     *
     * @param sequence     sequência destino
     * @param backingTrack progressão, BPM, compasso e duração
     * @param totalTicks   duração total em ticks
     * @param ppq          pulsos por semínima (480 no projeto)
     * @throws InvalidMidiDataException se a sequência não aceitar os eventos
     */
    public void generateKeyboardTrack(Sequence sequence, BackingTrack backingTrack,
                                      long totalTicks, int ppq) throws InvalidMidiDataException {

        Track track = sequence.createTrack();
        setProgramChange(track, CHANNEL_KEYBOARD, config.getProgramChange(), 0);

        String[] chords = backingTrack.getProgression().split("\\s*-\\s*");
        long measureTicks = timeInfo.getMeasureTicks(ppq);
        long stepTicks = timeInfo.getStepTicks(ppq);

        KeyboardRhythmPattern pattern = config.getPattern();

        List<Integer> previousVoicing = null;
        long currentMeasureTick = 0;
        int chordIndex = 0;

        while (currentMeasureTick < totalTicks) {
            String chordSymbol = chords[chordIndex % chords.length];
            List<Integer> rawNotes = chordService.getMidiNotes(chordSymbol);
            List<Integer> voicing = VoicingService.forKeyboard(rawNotes, previousVoicing);
            previousVoicing = voicing;

            addSustainEvents(track, currentMeasureTick, measureTicks, totalTicks);

            List<Integer> attackSteps = attackSteps(pattern);
            for (int i = 0; i < attackSteps.size(); i++) {
                int step = attackSteps.get(i);
                long attackTick = currentMeasureTick + ((long) step * stepTicks);
                if (attackTick >= totalTicks) {
                    break;
                }

                // Segura até o próximo ataque (ou o fim do compasso), para o pad
                // soar sustentado e não como uma semicolcheia picada.
                long nextStep = (i + 1 < attackSteps.size()) ? attackSteps.get(i + 1) : pattern.getTotalSteps();
                long spanTicks = (nextStep - step) * stepTicks;
                long holdTicks = scaleDuration(spanTicks);
                long holdUntil = Math.min(attackTick + holdTicks, totalTicks - 1);

                AttackType attack = pattern.getAttack(step);
                switch (attack) {
                    case BLOCK -> playBlock(track, voicing, attackTick, holdUntil);
                    case ARP_UP -> playArpeggio(track, voicing, attackTick, stepTicks, true, totalTicks);
                    case ARP_DOWN -> playArpeggio(track, voicing, attackTick, stepTicks, false, totalTicks);
                    case NONE -> { }
                }
            }

            currentMeasureTick += measureTicks;
            chordIndex++;
        }
    }

    /** Steps com ataque, em ordem crescente. */
    private List<Integer> attackSteps(KeyboardRhythmPattern pattern) {
        List<Integer> steps = new ArrayList<>();
        for (int step = 0; step < pattern.getTotalSteps(); step++) {
            if (pattern.getAttack(step) != AttackType.NONE) {
                steps.add(step);
            }
        }
        return steps;
    }

    private long scaleDuration(long spanTicks) {
        return Math.max(MIN_NOTE_TICKS, (spanTicks * config.getNoteDurationPercent()) / 100);
    }

    /**
     * Liga o pedal no início do compasso e desliga no fim. O release é sempre
     * emitido (mesmo no último compasso parcial), senão o pedal fica preso.
     */
    private void addSustainEvents(Track track, long measureStart, long measureTicks, long totalTicks) {
        if (!config.isSustainEnabled()) {
            return;
        }
        long release = Math.min(measureStart + measureTicks - SUSTAIN_GAP_TICKS, totalTicks - 1);
        release = Math.max(release, measureStart);

        addControlChange(track, CHANNEL_KEYBOARD, CC_SUSTAIN, SUSTAIN_ON, measureStart);
        addControlChange(track, CHANNEL_KEYBOARD, CC_SUSTAIN, SUSTAIN_OFF, release);
    }

    /** Todas as notas no mesmo tick. */
    private void playBlock(Track track, List<Integer> notes, long tick, long holdUntil) {
        for (int note : notes) {
            addNoteEvent(track, ShortMessage.NOTE_ON, CHANNEL_KEYBOARD, note, config.getVelocity(), tick);
            addNoteEvent(track, ShortMessage.NOTE_OFF, CHANNEL_KEYBOARD, note, 0,
                    Math.max(tick + MIN_NOTE_TICKS, holdUntil));
        }
    }

    /**
     * Notas em sequência dentro do passo, uma após a outra. Sub-notas que
     * começariam depois do fim da música são descartadas — o gate do loop só
     * valida a primeira nota do arpejo.
     */
    private void playArpeggio(Track track, List<Integer> notes, long startTick,
                              long stepTicks, boolean up, long totalTicks) {
        if (notes.isEmpty()) {
            return;
        }
        List<Integer> ordered = new ArrayList<>(notes);
        if (!up) {
            Collections.reverse(ordered);
        }

        long subStep = Math.max(MIN_NOTE_TICKS, stepTicks / ordered.size());
        for (int i = 0; i < ordered.size(); i++) {
            long tick = startTick + ((long) i * subStep);
            if (tick >= totalTicks) {
                break;
            }
            long noteOff = Math.min(
                    Math.max(tick + MIN_NOTE_TICKS, tick + subStep - SUSTAIN_GAP_TICKS),
                    totalTicks - 1);
            addNoteEvent(track, ShortMessage.NOTE_ON, CHANNEL_KEYBOARD,
                    ordered.get(i), config.getVelocity(), tick);
            addNoteEvent(track, ShortMessage.NOTE_OFF, CHANNEL_KEYBOARD,
                    ordered.get(i), 0, noteOff);
        }
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

    private void addControlChange(Track track, int channel, int controller,
                                  int value, long tick) {
        try {
            track.add(new MidiEvent(new ShortMessage(
                    ShortMessage.CONTROL_CHANGE, channel, controller, value), tick));
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }
    }

    private void setProgramChange(Track track, int channel, int program, long tick) {
        try {
            track.add(new MidiEvent(new ShortMessage(
                    ShortMessage.PROGRAM_CHANGE, channel, program, 0), tick));
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }
    }
}
