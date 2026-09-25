package br.com.marcosbassetto.service;

import br.com.marcosbassetto.model.guitar.GuitarArticulation;
import br.com.marcosbassetto.model.guitar.GuitarConfig;
import br.com.marcosbassetto.model.guitar.GuitarRhythmPattern;
import br.com.marcosbassetto.model.music.BackingTrack;
import br.com.marcosbassetto.model.music.TimeSignatureInfo;
import br.com.marcosbassetto.model.performance.VelocityLevel;
import org.junit.jupiter.api.Test;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuitarMidiServiceTest {

    private static final int PPQ = 480;
    private static final int CHANNEL_GUITAR = 2;

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

    private static long noteOffCount(Track track) {
        long count = 0;
        for (int i = 0; i < track.size(); i++) {
            if (track.get(i).getMessage() instanceof ShortMessage sm
                    && sm.getCommand() == ShortMessage.NOTE_OFF) {
                count++;
            }
        }
        return count;
    }

    private static BackingTrack track(String progression, String measure) {
        return new BackingTrack(progression, "120", measure, "0", "8");
    }

    private static GuitarConfig configFor(BackingTrack backingTrack, String preset) {
        TimeSignatureInfo timeInfo = TimeSignatureInfo.parse(backingTrack.getMeasure());
        GuitarConfig config = new GuitarConfig();
        config.setPreset(preset);
        config.syncPatternTo(timeInfo);
        return config;
    }

    private static Track generate(BackingTrack backingTrack, String preset, long totalTicks) throws Exception {
        TimeSignatureInfo timeInfo = TimeSignatureInfo.parse(backingTrack.getMeasure());
        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);
        new GuitarMidiService(configFor(backingTrack, preset), timeInfo)
                .generateGuitarTrack(sequence, backingTrack, totalTicks, PPQ);
        return sequence.getTracks()[0];
    }

    @Test
    void emitsNylonGuitarProgramChangeOnChannel2() throws Exception {
        Track track = generate(track("C", "4/4"), GuitarRhythmPattern.PRESET_BATIDA_BASICA, 1920);

        boolean found = false;
        for (int i = 0; i < track.size(); i++) {
            if (track.get(i).getMessage() instanceof ShortMessage sm
                    && sm.getCommand() == ShortMessage.PROGRAM_CHANGE) {
                assertEquals(CHANNEL_GUITAR, sm.getChannel());
                assertEquals(25, sm.getData1());
                found = true;
            }
        }
        assertTrue(found, "deve emitir program change de guitarra");
    }

    @Test
    void allEventsStayOnGuitarChannel() throws Exception {
        Track track = generate(track("C - Am", "4/4"), GuitarRhythmPattern.PRESET_BATIDA_ROCK, 3840);

        for (int i = 0; i < track.size(); i++) {
            if (track.get(i).getMessage() instanceof ShortMessage sm) {
                assertEquals(CHANNEL_GUITAR, sm.getChannel(), "evento fora do canal da guitarra");
            }
        }
    }

    @Test
    void strumDownAscendsInPitchWithSmallOffsets() throws Exception {
        // padrao minimo: apenas um STRUM_DOWN no passo 0
        TimeSignatureInfo timeInfo = TimeSignatureInfo.parse("4/4");
        GuitarConfig config = new GuitarConfig();
        config.getPattern().setAttack(0, GuitarRhythmPattern.AttackType.STRUM_DOWN);

        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);
        new GuitarMidiService(config, timeInfo)
                .generateGuitarTrack(sequence, track("C", "4/4"), 1920, PPQ);
        List<NoteOn> ons = noteOns(sequence.getTracks()[0]);

        assertEquals(3, ons.size());
        assertEquals(0, ons.get(0).tick());
        assertEquals(15, ons.get(1).tick());
        assertEquals(30, ons.get(2).tick());
        assertTrue(ons.get(0).note() < ons.get(1).note(), "down comeca pelo grave");
        assertTrue(ons.get(1).note() < ons.get(2).note());
        assertVelocityNear(ons, VelocityLevel.MEDIO, "down");
    }

    @Test
    void strumUpDescendsInPitchAndIsSofterThanDown() throws Exception {
        TimeSignatureInfo timeInfo = TimeSignatureInfo.parse("4/4");
        GuitarConfig config = new GuitarConfig();
        config.getPattern().setAttack(0, GuitarRhythmPattern.AttackType.STRUM_UP);

        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);
        new GuitarMidiService(config, timeInfo)
                .generateGuitarTrack(sequence, track("C", "4/4"), 1920, PPQ);
        List<NoteOn> ons = noteOns(sequence.getTracks()[0]);

        assertEquals(3, ons.size());
        assertTrue(ons.get(0).note() > ons.get(1).note(), "up comeca pelo agudo");
        assertVelocityNear(ons, VelocityLevel.FRACO, "up");
        assertTrue(VelocityLevel.FRACO.getAverage() < VelocityLevel.MEDIO.getAverage(),
                "up e mais suave que down");
    }

    @Test
    void pickPlaysNotesSequentiallyWithinTheStep() throws Exception {
        TimeSignatureInfo timeInfo = TimeSignatureInfo.parse("4/4");
        GuitarConfig config = new GuitarConfig();
        config.setArticulation(GuitarArticulation.DEDILHADO);
        config.getPattern().setAttack(0, GuitarRhythmPattern.AttackType.PICK);

        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);
        new GuitarMidiService(config, timeInfo)
                .generateGuitarTrack(sequence, track("C", "4/4"), 1920, PPQ);
        List<NoteOn> ons = noteOns(sequence.getTracks()[0]);

        assertEquals(3, ons.size());
        long stepTicks = timeInfo.getStepTicks(PPQ);
        long subStep = stepTicks / 3;
        assertEquals(0, ons.get(0).tick());
        assertEquals(subStep, ons.get(1).tick());
        assertEquals(subStep * 2, ons.get(2).tick());
        assertVelocityNear(ons, VelocityLevel.MEDIO, "dedilhado");
    }

    /** A humanizacao varia em torno da media do nivel, sem sair dele. */
    private static void assertVelocityNear(List<NoteOn> ons, VelocityLevel level, String label) {
        int average = level.getAverage();
        int range = average / 3;
        for (NoteOn note : ons) {
            assertTrue(note.velocity() >= 1 && note.velocity() <= 127,
                    label + ": velocity fora da faixa MIDI: " + note.velocity());
            assertTrue(Math.abs(note.velocity() - average) <= range,
                    label + ": velocity " + note.velocity() + " longe da media " + average);
        }
    }

    @Test
    void dedilhadoArticulationTurnsStrumAttacksIntoPicks() throws Exception {
        TimeSignatureInfo timeInfo = TimeSignatureInfo.parse("4/4");
        GuitarConfig config = new GuitarConfig();
        config.setArticulation(GuitarArticulation.DEDILHADO);
        config.getPattern().setAttack(0, GuitarRhythmPattern.AttackType.STRUM_DOWN);

        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);
        new GuitarMidiService(config, timeInfo)
                .generateGuitarTrack(sequence, track("C", "4/4"), 1920, PPQ);
        List<NoteOn> ons = noteOns(sequence.getTracks()[0]);

        assertEquals(3, ons.size());
        long stepTicks = timeInfo.getStepTicks(PPQ);
        assertEquals(stepTicks / 3, ons.get(1).tick(), "deve tocar como dedilhado, nao como acorde");
        assertVelocityNear(ons, VelocityLevel.MEDIO, "dedilhado");
    }

    @Test
    void batidaArticulationTurnsPickAttacksIntoDownStrums() throws Exception {
        TimeSignatureInfo timeInfo = TimeSignatureInfo.parse("4/4");
        GuitarConfig config = new GuitarConfig();
        config.setArticulation(GuitarArticulation.BATIDA);
        config.getPattern().setAttack(0, GuitarRhythmPattern.AttackType.PICK);

        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);
        new GuitarMidiService(config, timeInfo)
                .generateGuitarTrack(sequence, track("C", "4/4"), 1920, PPQ);
        List<NoteOn> ons = noteOns(sequence.getTracks()[0]);

        assertEquals(3, ons.size());
        assertEquals(0, ons.get(0).tick());
        assertEquals(15, ons.get(1).tick(), "deve tocar como batida para baixo");
        assertEquals(30, ons.get(2).tick());
        assertVelocityNear(ons, VelocityLevel.MEDIO, "batida");
    }

    @Test
    void batidaArticulationConvertsEveryAttackIntoAStrum() throws Exception {
        TimeSignatureInfo timeInfo = TimeSignatureInfo.parse("4/4");
        GuitarConfig config = new GuitarConfig();
        config.setArticulation(GuitarArticulation.BATIDA);
        config.getPattern().setAttack(0, GuitarRhythmPattern.AttackType.PICK);
        config.getPattern().setAttack(2, GuitarRhythmPattern.AttackType.PICK);

        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);
        new GuitarMidiService(config, timeInfo)
                .generateGuitarTrack(sequence, track("C", "4/4"), 1920, PPQ);
        List<NoteOn> ons = noteOns(sequence.getTracks()[0]);

        long stepTicks = timeInfo.getStepTicks(PPQ);
        long secondAttackTick = 2 * stepTicks;
        assertTrue(ons.stream().anyMatch(n -> n.tick() == 0), "primeiro ataque no passo 0");
        assertTrue(ons.stream().anyMatch(n -> n.tick() == 15),
                "batida espalha as notas com o offset da palhetada");
        assertTrue(ons.stream().anyMatch(n -> n.tick() == secondAttackTick),
                "segundo ataque no passo 2");
        assertVelocityNear(ons, VelocityLevel.MEDIO, "batida");
    }

    @Test
    void eachAttackStartsAtItsStepTick() throws Exception {
        TimeSignatureInfo timeInfo = TimeSignatureInfo.parse("4/4");
        GuitarConfig config = new GuitarConfig();
        config.getPattern().setAttack(0, GuitarRhythmPattern.AttackType.STRUM_DOWN);
        config.getPattern().setAttack(8, GuitarRhythmPattern.AttackType.STRUM_DOWN); // 3o tempo

        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);
        new GuitarMidiService(config, timeInfo)
                .generateGuitarTrack(sequence, track("C", "4/4"), 1920, PPQ);
        List<NoteOn> ons = noteOns(sequence.getTracks()[0]);

        long stepTicks = timeInfo.getStepTicks(PPQ);
        assertTrue(ons.stream().anyMatch(n -> n.tick() == 0));
        assertTrue(ons.stream().anyMatch(n -> n.tick() == 8 * stepTicks));
    }

    @Test
    void noEventsAreWrittenAtOrBeyondTotalTicks() throws Exception {
        Track track = generate(track("C", "4/4"), GuitarRhythmPattern.PRESET_BATIDA_ROCK, 1000);
        for (NoteOn note : noteOns(track)) {
            assertTrue(note.tick() < 1000, "evento alem do fim: " + note.tick());
        }
    }

    @Test
    void everyNoteOnHasMatchingNoteOff() throws Exception {
        Track track = generate(track("C - Am - Dm - G", "4/4"),
                GuitarRhythmPattern.PRESET_BATIDA_ROCK, 7680);
        assertEquals(noteOns(track).size(), noteOffCount(track));
    }

    @Test
    void progressionCyclesChordsAcrossMeasures() throws Exception {
        Track track = generate(track("C - Am", "4/4"), GuitarRhythmPattern.PRESET_BATIDA_ROCK, 3840);

        long measureTicks = 1920L;
        // compasso 0 toca C (60,64,67); compasso 1 toca Am (voicing com A)
        List<Integer> firstMeasure = new ArrayList<>();
        List<Integer> secondMeasure = new ArrayList<>();
        for (NoteOn note : noteOns(track)) {
            if (note.tick() < measureTicks) firstMeasure.add(note.note());
            else secondMeasure.add(note.note());
        }
        assertFalse(firstMeasure.isEmpty());
        assertFalse(secondMeasure.isEmpty());
        assertTrue(firstMeasure.stream().anyMatch(n -> Math.floorMod(n, 12) == 0),
                "primeiro compasso deve conter C");
        assertTrue(secondMeasure.stream().anyMatch(n -> Math.floorMod(n, 12) == 9),
                "segundo compasso (Am) deve conter A");
    }

    @Test
    void guitarVoicingRangeIsRespectedInGeneratedMidi() throws Exception {
        Track track = generate(track("C - Am - Dm - G", "4/4"),
                GuitarRhythmPattern.PRESET_DEDILHADO, 7680);
        for (NoteOn note : noteOns(track)) {
            assertTrue(note.note() >= VoicingService.GUITAR_LOW
                            && note.note() <= VoicingService.GUITAR_HIGH,
                    "guitarra fora do registro: " + note.note());
        }
    }

    @Test
    void oddMetersGenerateWithoutError() throws Exception {
        for (String sig : new String[]{"3/4", "5/4", "6/8", "7/8", "12/8"}) {
            Track track = generate(track("C - G", sig), GuitarRhythmPattern.PRESET_BATIDA_ROCK, 7680);
            assertFalse(noteOns(track).isEmpty(), sig + " deveria gerar notas");
        }
    }

    @Test
    void emptyPresetGeneratesOnlyProgramChange() throws Exception {
        TimeSignatureInfo timeInfo = TimeSignatureInfo.parse("4/4");
        GuitarConfig config = new GuitarConfig();
        config.getPattern().clear();

        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);
        new GuitarMidiService(config, timeInfo)
                .generateGuitarTrack(sequence, track("C", "4/4"), 1920, PPQ);

        assertTrue(noteOns(sequence.getTracks()[0]).isEmpty(), "sem ataques, sem notas");
    }
}
