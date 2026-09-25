package br.com.marcosbassetto.service;

import br.com.marcosbassetto.model.keyboard.KeyboardConfig;
import br.com.marcosbassetto.model.keyboard.KeyboardRhythmPattern;
import br.com.marcosbassetto.model.music.BackingTrack;
import br.com.marcosbassetto.model.music.TimeSignatureInfo;
import br.com.marcosbassetto.model.performance.VelocityLevel;
import org.junit.jupiter.api.Test;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyboardMidiServiceTest {

    private static final int PPQ = 480;
    private static final int CHANNEL_KEYBOARD = 3;
    private static final int CC_SUSTAIN = 64;

    private record NoteOn(long tick, int note, int velocity) {}

    private static List<NoteOn> noteOns(Track track) {
        List<NoteOn> result = new ArrayList<>();
        for (int i = 0; i < track.size(); i++) {
            MidiEvent event = track.get(i);
            if (event.getMessage() instanceof ShortMessage sm
                    && sm.getCommand() == ShortMessage.NOTE_ON
                    && sm.getData2() > 0) {
                result.add(new NoteOn(event.getTick(), sm.getData1(), sm.getData2()));
            }
        }
        return result;
    }

    private static List<Integer> sustainValues(Track track) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < track.size(); i++) {
            MidiEvent event = track.get(i);
            if (event.getMessage() instanceof ShortMessage sm
                    && sm.getCommand() == ShortMessage.CONTROL_CHANGE
                    && sm.getData1() == CC_SUSTAIN) {
                result.add(sm.getData2());
            }
        }
        return result;
    }

    private static long countCommand(Track track, int command) {
        long count = 0;
        for (int i = 0; i < track.size(); i++) {
            if (track.get(i).getMessage() instanceof ShortMessage sm
                    && sm.getCommand() == command) {
                count++;
            }
        }
        return count;
    }

    private static BackingTrack chord(String symbol, String measure) {
        return new BackingTrack(symbol, "120", measure, "0", "8");
    }

    private static Track generate(BackingTrack backingTrack, String preset,
                                  boolean sustain, long totalTicks) throws Exception {
        TimeSignatureInfo timeInfo = TimeSignatureInfo.parse(backingTrack.getMeasure());
        KeyboardConfig config = new KeyboardConfig(timeInfo);
        config.applyPreset(preset, timeInfo);
        config.setSustainEnabled(sustain);

        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);
        new KeyboardMidiService(config, timeInfo)
                .generateKeyboardTrack(sequence, backingTrack, totalTicks, PPQ);
        return sequence.getTracks()[0];
    }

    private static Track generate(BackingTrack backingTrack, String preset, long totalTicks)
            throws Exception {
        return generate(backingTrack, preset, true, totalTicks);
    }

    @Test
    void createsExactlyOneTrack() throws Exception {
        TimeSignatureInfo timeInfo = TimeSignatureInfo.parse("4/4");
        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);
        new KeyboardMidiService(new KeyboardConfig(timeInfo), timeInfo)
                .generateKeyboardTrack(sequence, chord("C", "4/4"), 1920, PPQ);

        assertEquals(1, sequence.getTracks().length);
    }

    @Test
    void emitsGrandPianoProgramChangeOnChannel3() throws Exception {
        Track t = generate(chord("C", "4/4"),
                KeyboardRhythmPattern.PRESET_PAD_SUSTENTADO, 1920);

        boolean found = false;
        for (int i = 0; i < t.size(); i++) {
            if (t.get(i).getMessage() instanceof ShortMessage sm
                    && sm.getCommand() == ShortMessage.PROGRAM_CHANGE) {
                assertEquals(CHANNEL_KEYBOARD, sm.getChannel());
                assertEquals(0, sm.getData1());
                found = true;
            }
        }
        assertTrue(found);
    }

    @Test
    void allEventsStayOnKeyboardChannel() throws Exception {
        Track t = generate(chord("C - Am - Dm - G7", "4/4"),
                KeyboardRhythmPattern.PRESET_ARPEJO_UP, 7680);

        for (int i = 0; i < t.size(); i++) {
            if (t.get(i).getMessage() instanceof ShortMessage sm) {
                assertEquals(CHANNEL_KEYBOARD, sm.getChannel());
            }
        }
    }

    @Test
    void blockPlaysAllNotesAtTheSameTick() throws Exception {
        Track t = generate(chord("C", "4/4"),
                KeyboardRhythmPattern.PRESET_PAD_SUSTENTADO, 1920);
        List<NoteOn> ons = noteOns(t);

        assertEquals(3, ons.size(), "C maior = 3 notas");
        assertEquals(1, ons.stream().map(NoteOn::tick).distinct().count(),
                "BLOCK deve tocar todas as notas no mesmo tick");
        assertTrue(ons.stream().allMatch(n -> n.tick() == 0));
    }

    /**
     * Regressão: o pad tem um único ataque no tempo 1. A nota precisa durar
     * até o fim do compasso, não o intervalo de um único passo (semicolcheia),
     * senão o pad vira um pipoco — e pior, o problema fica escondido quando o
     * pedal de sustain está ligado.
     */
    @Test
    void padNoteIsHeldForTheMeasureEvenWithoutSustain() throws Exception {
        long measureTicks = 1920;
        Track t = generate(chord("C", "4/4"),
                KeyboardRhythmPattern.PRESET_PAD_SUSTENTADO, false, measureTicks * 2);

        long firstOn = noteOns(t).get(0).tick();
        long firstOff = -1;
        for (int i = 0; i < t.size(); i++) {
            if (t.get(i).getMessage() instanceof ShortMessage sm
                    && sm.getCommand() == ShortMessage.NOTE_OFF) {
                firstOff = t.get(i).getTick();
                break;
            }
        }

        assertEquals(0, firstOn);
        assertTrue(firstOff > measureTicks / 2,
                "pad deveria sustentar pelo compasso inteiro, mas durou " + firstOff + " ticks");
        assertTrue(firstOff <= measureTicks, "nao deve passar do compasso: " + firstOff);
    }

    @Test
    void everyNoteOnHasMatchingNoteOff() throws Exception {
        for (String preset : KeyboardRhythmPattern.ALL_PRESETS) {
            for (boolean sustain : new boolean[]{true, false}) {
                Track t = generate(chord("C - Am - Dm - G", "4/4"), preset, sustain, 7680);
                assertEquals(noteOns(t).size(), countCommand(t, ShortMessage.NOTE_OFF),
                        preset + " sustain=" + sustain);
            }
        }
    }

    @Test
    void notesNeverHaveZeroOrNegativeDuration() throws Exception {
        for (String preset : KeyboardRhythmPattern.ALL_PRESETS) {
            Track t = generate(chord("C", "3/4"), preset, 3840);
            List<NoteOn> ons = noteOns(t);
            List<Long> offs = new ArrayList<>();
            for (int i = 0; i < t.size(); i++) {
                if (t.get(i).getMessage() instanceof ShortMessage sm
                        && sm.getCommand() == ShortMessage.NOTE_OFF) {
                    offs.add(t.get(i).getTick());
                }
            }
            for (int i = 0; i < ons.size(); i++) {
                assertTrue(offs.get(i) > ons.get(i).tick(),
                        preset + ": duracao invalida em " + ons.get(i));
            }
        }
    }

    @Test
    void sustainEmitsOnAndOffPerMeasure() throws Exception {
        Track t = generate(chord("C - Am", "4/4"),
                KeyboardRhythmPattern.PRESET_PAD_SUSTENTADO, 3840);

        List<Integer> values = sustainValues(t);
        assertEquals(4, values.size(), "2 compassos x (liga + desliga)");
        assertEquals(List.of(127, 0, 127, 0), values);
    }

    /** Regressão: num último compasso parcial o release precisa sair mesmo assim. */
    @Test
    void sustainIsReleasedEvenInAPartialLastMeasure() throws Exception {
        // 1000 ticks cortam o compasso no meio
        Track t = generate(chord("C", "4/4"),
                KeyboardRhythmPattern.PRESET_PAD_SUSTENTADO, 1000);

        List<Integer> values = sustainValues(t);
        assertFalse(values.isEmpty(), "deve haver eventos de sustain");
        assertEquals(0, values.get(values.size() - 1),
                "o pedal nao pode ficar preso ligado (ultimo valor deve ser 0)");
    }

    @Test
    void noSustainEventsWhenDisabled() throws Exception {
        Track t = generate(chord("C", "4/4"),
                KeyboardRhythmPattern.PRESET_PAD_SUSTENTADO, false, 3840);
        assertTrue(sustainValues(t).isEmpty());
    }

    @Test
    void arpejoUpPlaysNotesFromLowToHigh() throws Exception {
        Track t = generate(chord("C", "4/4"),
                KeyboardRhythmPattern.PRESET_ARPEJO_UP, 1920);
        List<NoteOn> firstBeat = noteOns(t).stream()
                .filter(n -> n.tick() < 480)
                .toList();

        assertEquals(3, firstBeat.size());
        for (int i = 1; i < firstBeat.size(); i++) {
            assertTrue(firstBeat.get(i).note() > firstBeat.get(i - 1).note(),
                    "arpejo ascendente fora de ordem");
        }
    }

    @Test
    void arpejoDownPlaysNotesFromHighToLow() throws Exception {
        Track t = generate(chord("C", "4/4"),
                KeyboardRhythmPattern.PRESET_ARPEJO_DOWN, 1920);
        List<NoteOn> firstBeat = noteOns(t).stream()
                .filter(n -> n.tick() < 480)
                .toList();

        assertEquals(3, firstBeat.size());
        for (int i = 1; i < firstBeat.size(); i++) {
            assertTrue(firstBeat.get(i).note() < firstBeat.get(i - 1).note(),
                    "arpejo descendente fora de ordem");
        }
    }

    @Test
    void arpeggioNotesAreSequentialNotSimultaneous() throws Exception {
        Track t = generate(chord("C", "4/4"),
                KeyboardRhythmPattern.PRESET_ARPEJO_UP, 1920);
        List<NoteOn> firstBeat = noteOns(t).stream()
                .filter(n -> n.tick() < 480)
                .toList();

        assertTrue(firstBeat.stream().map(NoteOn::tick).distinct().count() > 1,
                "arpejo deve espalhar as notas no tempo, nao toca-las juntas");
    }

    @Test
    void allNotesStayInKeyboardRegion() throws Exception {
        for (String symbol : new String[]{"C", "G", "Am", "Dm", "G7", "F", "Bb", "Em", "C#m"}) {
            Track t = generate(chord(symbol, "4/4"),
                    KeyboardRhythmPattern.PRESET_PAD_SUSTENTADO, 1920);
            for (NoteOn note : noteOns(t)) {
                assertTrue(note.note() >= VoicingService.KEYBOARD_LOW
                                && note.note() <= VoicingService.KEYBOARD_HIGH,
                        symbol + ": nota " + note.note() + " fora de C4-C6");
            }
        }
    }

    @Test
    void velocitiesAreHumanizedAroundTheChosenLevel() throws Exception {
        TimeSignatureInfo timeInfo = TimeSignatureInfo.parse("4/4");
        KeyboardConfig config = new KeyboardConfig(timeInfo);
        config.setVelocityLevel(VelocityLevel.ALTO);

        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);
        new KeyboardMidiService(config, timeInfo)
                .generateKeyboardTrack(sequence, chord("C - Am - Dm - G", "4/4"), 7680, PPQ);
        List<NoteOn> ons = noteOns(sequence.getTracks()[0]);

        assertFalse(ons.isEmpty());
        int average = VelocityLevel.ALTO.getAverage();
        int range = average / 3;
        for (NoteOn note : ons) {
            assertTrue(note.velocity() >= 1 && note.velocity() <= 127,
                    "velocity fora da faixa MIDI: " + note.velocity());
            assertTrue(Math.abs(note.velocity() - average) <= range,
                    "velocity " + note.velocity() + " longe da media " + average);
        }
        double mean = ons.stream().mapToInt(NoteOn::velocity).average().orElse(0);
        assertTrue(Math.abs(mean - average) <= range,
                "media das velocities (" + mean + ") deve ficar proxima de " + average);
    }

    @Test
    void noEventsAtOrBeyondTotalTicks() throws Exception {
        Track t = generate(chord("C - Am", "4/4"),
                KeyboardRhythmPattern.PRESET_ARPEJO_UP, 1000);
        for (NoteOn note : noteOns(t)) {
            assertTrue(note.tick() < 1000, "evento alem do fim: " + note.tick());
        }
    }

    @Test
    void progressionCyclesChordsAcrossMeasures() throws Exception {
        Track t = generate(chord("C - G", "4/4"),
                KeyboardRhythmPattern.PRESET_PAD_SUSTENTADO, 3840);

        Set<Integer> firstMeasure = new HashSet<>();
        Set<Integer> secondMeasure = new HashSet<>();
        for (NoteOn note : noteOns(t)) {
            // O teclado toca o acorde inteiro: normaliza para classe de altura.
            if (note.tick() < 1920) firstMeasure.add(note.note() % 12);
            else secondMeasure.add(note.note() % 12);
        }

        assertFalse(firstMeasure.isEmpty());
        assertFalse(secondMeasure.isEmpty());
        assertEquals(Set.of(0, 4, 7), firstMeasure, "compasso 1 = C maior (C-E-G)");
        assertEquals(Set.of(7, 11, 2), secondMeasure, "compasso 2 = G maior (G-B-D)");
    }

    @Test
    void oddMetersGenerateWithoutError() throws Exception {
        for (String sig : new String[]{"3/4", "5/4", "6/8", "7/8", "12/8"}) {
            for (String preset : KeyboardRhythmPattern.ALL_PRESETS) {
                Track t = generate(chord("C - G", sig), preset, 7680);
                assertFalse(noteOns(t).isEmpty(), sig + "/" + preset + " deveria gerar notas");
            }
        }
    }

    @Test
    void emptyPatternGeneratesOnlyProgramChange() throws Exception {
        TimeSignatureInfo timeInfo = TimeSignatureInfo.parse("4/4");
        KeyboardConfig config = new KeyboardConfig(timeInfo);
        config.getPattern().clear();

        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);
        new KeyboardMidiService(config, timeInfo)
                .generateKeyboardTrack(sequence, chord("C", "4/4"), 1920, PPQ);

        assertTrue(noteOns(sequence.getTracks()[0]).isEmpty());
    }

    @Test
    void nullConfigDoesNotCrash() throws Exception {
        TimeSignatureInfo timeInfo = TimeSignatureInfo.parse("4/4");
        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);

        new KeyboardMidiService(null, timeInfo)
                .generateKeyboardTrack(sequence, chord("C", "4/4"), 1920, PPQ);

        assertFalse(noteOns(sequence.getTracks()[0]).isEmpty());
    }
}
