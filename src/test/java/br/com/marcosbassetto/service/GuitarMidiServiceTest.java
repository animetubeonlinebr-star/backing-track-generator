package br.com.marcosbassetto.service;

import br.com.marcosbassetto.model.guitar.GuitarArticulation;
import br.com.marcosbassetto.model.guitar.GuitarConfig;
import br.com.marcosbassetto.model.guitar.GuitarRhythmPattern;
import br.com.marcosbassetto.model.music.BackingTrack;
import br.com.marcosbassetto.model.music.Intensity;
import br.com.marcosbassetto.model.music.TimeSignatureInfo;
import org.junit.jupiter.api.Test;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

    private static double meanVelocity(List<NoteOn> ons) {
        return ons.stream().mapToInt(NoteOn::velocity).average().orElse(0);
    }

    private static BackingTrack track(String progression, String measure) {
        return new BackingTrack(progression, "120", measure, "0", "8");
    }

    /**
     * Articulação de dedilhado: os ataques de batida viram notas sequenciais, mas
     * o groove (quando há ataque) continua sendo o do preset.
     */
    @Test
    void dedilhadoArticulationTurnsStrumAttacksIntoSequentialPicks() throws Exception {
        TimeSignatureInfo timeInfo = TimeSignatureInfo.parse("4/4");
        GuitarConfig config = new GuitarConfig();
        config.setArticulation(GuitarArticulation.DEDILHADO);
        config.getPattern().setAttack(0, GuitarRhythmPattern.AttackType.STRUM_DOWN);

        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);
        new GuitarMidiService(config, timeInfo)
                .generateGuitarTrack(sequence, track("C", "4/4"), 1920, PPQ);
        List<NoteOn> ons = noteOns(sequence.getTracks()[0]);

        assertEquals(3, ons.size(), "dedilhado toca nota a nota, nao o acorde");
        long stepTicks = timeInfo.getStepTicks(PPQ);
        assertEquals(stepTicks / 3, ons.get(1).tick(),
                "as notas saem em sequencia dentro do passo, nao espalhadas pelo offset");
    }

    /** Articulação de batida: ataques de dedilhado viram palhetada para baixo. */
    @Test
    void batidaArticulationTurnsPickAttacksIntoStrums() throws Exception {
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
        assertEquals(15, ons.get(1).tick(), "batida espalha as notas com o offset");
        assertEquals(30, ons.get(2).tick());
    }

    /** A articulação mista preserva o preset como está. */
    @Test
    void mistaArticulationKeepsThePresetTexture() throws Exception {
        TimeSignatureInfo timeInfo = TimeSignatureInfo.parse("4/4");
        GuitarConfig config = new GuitarConfig();
        config.setArticulation(GuitarArticulation.MISTA);
        config.getPattern().setAttack(0, GuitarRhythmPattern.AttackType.STRUM_DOWN);
        config.getPattern().setAttack(2, GuitarRhythmPattern.AttackType.PICK);

        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);
        new GuitarMidiService(config, timeInfo)
                .generateGuitarTrack(sequence, track("C", "4/4"), 1920, PPQ);
        List<NoteOn> ons = noteOns(sequence.getTracks()[0]);

        long stepTicks = timeInfo.getStepTicks(PPQ);
        assertTrue(ons.stream().anyMatch(n -> n.tick() == 15),
                "o ataque de batida continua espalhado");
        assertTrue(ons.stream().anyMatch(n -> n.tick() == (2 * stepTicks) + (stepTicks / 3)),
                "o ataque de dedilhado continua sequencial");
    }

    private static GuitarConfig configFor(BackingTrack backingTrack, String preset) {
        TimeSignatureInfo timeInfo = TimeSignatureInfo.parse(backingTrack.getMeasure());
        GuitarConfig config = new GuitarConfig();
        config.setPreset(preset);
        config.syncPatternTo(timeInfo);
        return config;
    }

    private static Track generate(BackingTrack backingTrack, String preset, long totalTicks) throws Exception {
        return generate(backingTrack, preset, null, totalTicks);
    }

    /**
     * Variante determinística: sem desvio aleatório, sobra apenas o acento de
     * fraseado — é o que permite afirmar qual nota do acorde é a mais forte.
     */
    private static Track generate(BackingTrack backingTrack, String preset,
                                  Intensity intensity, long totalTicks) throws Exception {
        TimeSignatureInfo timeInfo = TimeSignatureInfo.parse(backingTrack.getMeasure());
        GuitarConfig config = configFor(backingTrack, preset);
        if (intensity != null) {
            config.setIntensity(intensity);
        }
        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);
        new GuitarMidiService(config, timeInfo, new VelocityHumanizer(new Random(1), 0))
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
        config.setIntensity(Intensity.FORTE); // media 80
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
        // Varias execucoes: a humanizacao varia o valor, mas nao a media.
        long sum = 0;
        for (int run = 0; run < 40; run++) {
            Sequence repeated = new Sequence(Sequence.PPQ, PPQ);
            new GuitarMidiService(config, timeInfo)
                    .generateGuitarTrack(repeated, track("C", "4/4"), 1920, PPQ);
            sum += meanVelocity(noteOns(repeated.getTracks()[0]));
        }
        double mean = (double) sum / 40;
        assertTrue(Math.abs(mean - 80) <= 6,
                "media humanizada perto de 80, foi " + mean);
    }

    @Test
    void strumDownUsesHumanizedVelocitiesAroundTheIntensityMean() throws Exception {
        List<NoteOn> ons = noteOns(generate(track("C", "4/4"),
                GuitarRhythmPattern.PRESET_BATIDA_ROCK, Intensity.FORTE, 1920));

        assertTrue(ons.size() > 4);
        assertTrue(ons.stream().mapToInt(NoteOn::velocity).distinct().count() > 1,
                "velocity nao pode ser constante (deve ser humanizada)");
        assertTrue(Math.abs(meanVelocity(ons) - 80) <= 8,
                "media perto de 80 na intensidade forte, foi " + meanVelocity(ons));
    }

    @Test
    void strumUpKeepsChordToneAccentOnTheLowestNote() throws Exception {
        TimeSignatureInfo timeInfo = TimeSignatureInfo.parse("4/4");
        GuitarConfig config = new GuitarConfig();
        config.setIntensity(Intensity.FORTE);
        config.setStrumOffsetTicks(0);
        config.getPattern().setAttack(0, GuitarRhythmPattern.AttackType.STRUM_UP);

        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);
        new GuitarMidiService(config, timeInfo, new VelocityHumanizer(new Random(1), 0))
                .generateGuitarTrack(sequence, track("C", "4/4"), 1920, PPQ);
        List<NoteOn> ons = noteOns(sequence.getTracks()[0]);

        assertEquals(3, ons.size());
        assertTrue(ons.get(0).note() > ons.get(1).note(), "up comeca pelo agudo");
        // A nota mais grave (ultima no up) recebe o maior acento de fundamental.
        int lowestVelocity = ons.get(2).velocity();
        assertTrue(lowestVelocity > ons.get(0).velocity(),
                "nota grave do up deve ser a mais forte");
    }

    @Test
    void pickPlaysNotesSequentiallyWithinTheStep() throws Exception {
        TimeSignatureInfo timeInfo = TimeSignatureInfo.parse("4/4");
        GuitarConfig config = new GuitarConfig();
        config.setIntensity(Intensity.FORTE); // media 75
        config.getPattern().setAttack(0, GuitarRhythmPattern.AttackType.PICK);

        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);
        // Humanizador deterministico: com apenas 3 notas, um desvio aleatorio
        // torna a checagem de media instavel.
        new GuitarMidiService(config, timeInfo, new VelocityHumanizer(new Random(1), 0))
                .generateGuitarTrack(sequence, track("C", "4/4"), 1920, PPQ);
        List<NoteOn> ons = noteOns(sequence.getTracks()[0]);

        assertEquals(3, ons.size());
        long stepTicks = timeInfo.getStepTicks(PPQ);
        long subStep = stepTicks / 3;
        assertEquals(0, ons.get(0).tick());
        assertEquals(subStep, ons.get(1).tick());
        assertEquals(subStep * 2, ons.get(2).tick());
        assertTrue(Math.abs(meanVelocity(ons) - 75) <= 8,
                "media perto de 75 na intensidade forte, foi " + meanVelocity(ons));
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
