package br.com.marcosbassetto.service;

import br.com.marcosbassetto.model.bass.BassConfig;
import br.com.marcosbassetto.model.bass.BassRhythmPattern;
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

class BassMidiServiceTest {

    private static final int PPQ = 480;
    private static final int CHANNEL_BASS = 1;

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

    private static BackingTrack track(String progression, String measure) {
        return new BackingTrack(progression, "120", measure, "0", "8");
    }

    private static Track generate(BackingTrack backingTrack, String preset, long totalTicks)
            throws Exception {
        return generate(backingTrack, preset, Intensity.MEDIA, totalTicks);
    }

    private static Track generate(BackingTrack backingTrack, String preset,
                                  Intensity intensity, long totalTicks) throws Exception {
        TimeSignatureInfo timeInfo = TimeSignatureInfo.parse(backingTrack.getMeasure());
        BassConfig config = new BassConfig(timeInfo);
        config.applyPreset(preset, timeInfo);
        config.setIntensity(intensity);

        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);
        new BassMidiService(config, timeInfo)
                .generateBassTrack(sequence, backingTrack, totalTicks, PPQ);
        return sequence.getTracks()[0];
    }

    @Test
    void createsExactlyOneTrack() throws Exception {
        TimeSignatureInfo timeInfo = TimeSignatureInfo.parse("4/4");
        BassConfig config = new BassConfig(timeInfo);

        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);
        new BassMidiService(config, timeInfo)
                .generateBassTrack(sequence, track("C", "4/4"), 1920, PPQ);

        assertEquals(1, sequence.getTracks().length);
    }

    @Test
    void emitsFingerBassProgramChangeOnChannel1() throws Exception {
        Track t = generate(track("C", "4/4"), BassRhythmPattern.PRESET_FUNDAMENTAL_SIMPLES, 1920);

        boolean found = false;
        for (int i = 0; i < t.size(); i++) {
            if (t.get(i).getMessage() instanceof ShortMessage sm
                    && sm.getCommand() == ShortMessage.PROGRAM_CHANGE) {
                assertEquals(CHANNEL_BASS, sm.getChannel());
                assertEquals(33, sm.getData1());
                found = true;
            }
        }
        assertTrue(found);
    }

    @Test
    void allEventsStayOnBassChannel() throws Exception {
        Track t = generate(track("C - Am - Dm - G7", "4/4"),
                BassRhythmPattern.PRESET_CAMINHANTE, 7680);

        for (int i = 0; i < t.size(); i++) {
            if (t.get(i).getMessage() instanceof ShortMessage sm) {
                assertEquals(CHANNEL_BASS, sm.getChannel());
            }
        }
    }

    @Test
    void everyNoteOnHasMatchingNoteOff() throws Exception {
        Track t = generate(track("C - Am - Dm - G7", "4/4"),
                BassRhythmPattern.PRESET_CAMINHANTE, 7680);
        assertEquals(noteOns(t).size(), countCommand(t, ShortMessage.NOTE_OFF));
    }

    @Test
    void rootNotesStayInBassRegion() throws Exception {
        Track t = generate(track("C - Am - Dm - G", "4/4"),
                BassRhythmPattern.PRESET_FUNDAMENTAL_SIMPLES, 7680);

        for (NoteOn note : noteOns(t)) {
            assertTrue(note.note() >= VoicingService.BASS_LOW
                            && note.note() <= VoicingService.BASS_HIGH,
                    "fundamental fora da regiao do baixo: " + note.note());
        }
    }

    /**
     * Regressão: a quinta e a oitava derivadas não podem subir até a região da
     * guitarra (43–64). A fundamental em G2 = 43 era o pior caso.
     */
    @Test
    void fifthAndOctaveNeverInvadeGuitarRegion() throws Exception {
        for (String chord : new String[]{"C", "G", "Am", "Dm", "G7", "F", "Bb", "Em"}) {
            Track t = generate(track(chord, "4/4"), BassRhythmPattern.PRESET_CAMINHANTE, 1920);
            for (NoteOn note : noteOns(t)) {
                assertTrue(note.note() <= VoicingService.BASS_DERIVED_HIGH,
                        chord + ": baixo em " + note.note() + " invade a guitarra");
            }
        }
    }

    @Test
    void caminhanteUsesRootFifthAndOctave() throws Exception {
        Track t = generate(track("C", "4/4"), BassRhythmPattern.PRESET_CAMINHANTE, 1920);
        List<NoteOn> ons = noteOns(t);

        assertEquals(4, ons.size());
        int root = ons.get(0).note();
        assertEquals(0, root % 12, "fundamental de C");
        assertEquals(Math.floorMod(root + 7, 12), Math.floorMod(ons.get(1).note(), 12), "quinta");
        assertEquals(0, ons.get(2).note() % 12, "oitava");
    }

    @Test
    void velocityIsHumanizedAroundTheIntensityMean() throws Exception {
        TimeSignatureInfo timeInfo = TimeSignatureInfo.parse("4/4");
        BassConfig config = new BassConfig(timeInfo);
        config.applyPreset(BassRhythmPattern.PRESET_CAMINHANTE, timeInfo);
        config.setIntensity(Intensity.FORTE); // media 90
        config.setNoteDurationPercent(50);

        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);
        new BassMidiService(config, timeInfo)
                .generateBassTrack(sequence, track("C - Am - Dm - G", "4/4"), 7680, PPQ);
        Track t = sequence.getTracks()[0];

        List<NoteOn> ons = noteOns(t);
        assertTrue(ons.size() > 4, "precisa de varias notas para avaliar humanizacao");
        assertTrue(ons.stream().mapToInt(NoteOn::velocity).distinct().count() > 1,
                "velocity nao pode ser constante (deve ser humanizada)");
        assertTrue(ons.stream().allMatch(n -> n.velocity() >= 1 && n.velocity() <= 127));

        double mean = ons.stream().mapToInt(NoteOn::velocity).average().orElseThrow();
        assertTrue(Math.abs(mean - 90) <= 8, "media humanizada perto de 90, foi " + mean);

        long stepTicks = timeInfo.getStepTicks(PPQ);
        long expectedDuration = (stepTicks * 50) / 100;
        long noteOffTick = -1;
        for (int i = 0; i < t.size(); i++) {
            if (t.get(i).getMessage() instanceof ShortMessage sm
                    && sm.getCommand() == ShortMessage.NOTE_OFF) {
                noteOffTick = t.get(i).getTick();
                break;
            }
        }
        assertEquals(ons.get(0).tick() + expectedDuration, noteOffTick);
    }

    @Test
    void noEventsAtOrBeyondTotalTicks() throws Exception {
        Track t = generate(track("C - Am", "4/4"), BassRhythmPattern.PRESET_CAMINHANTE, 1000);
        for (NoteOn note : noteOns(t)) {
            assertTrue(note.tick() < 1000, "evento alem do fim: " + note.tick());
        }
    }

    @Test
    void progressionCyclesChordsAcrossMeasures() throws Exception {
        Track t = generate(track("C - G", "4/4"),
                BassRhythmPattern.PRESET_FUNDAMENTAL_SIMPLES, 3840);

        List<Integer> firstMeasure = new ArrayList<>();
        List<Integer> secondMeasure = new ArrayList<>();
        for (NoteOn note : noteOns(t)) {
            if (note.tick() < 1920) firstMeasure.add(note.note());
            else secondMeasure.add(note.note());
        }
        assertFalse(firstMeasure.isEmpty());
        assertFalse(secondMeasure.isEmpty());
        assertTrue(firstMeasure.stream().allMatch(n -> n % 12 == 0), "compasso 1 = C");
        assertTrue(secondMeasure.stream().allMatch(n -> n % 12 == 7), "compasso 2 = G");
    }

    /**
     * O walking jazz precisa realmente resolver a aproximação cromática: no
     * último tempo do compasso a nota tem de ser um semitom da fundamental do
     * PRÓXIMO acorde. Sem isso o preset seria só um walking blues com outro nome.
     */
    @Test
    void jazzApproachLandsOneSemitoneBelowTheNextRoot() throws Exception {
        Track t = generate(track("C - Am - Dm - G7", "4/4"),
                BassRhythmPattern.PRESET_JAZZ, 7680);

        List<NoteOn> ons = noteOns(t);
        assertEquals(16, ons.size(), "4 compassos x 4 tempos");

        // Fundamental de cada compasso e a aproximação gravada no compasso anterior.
        List<NoteOn> downbeats = new ArrayList<>();
        for (NoteOn note : ons) {
            if (note.tick() % 1920 == 0) {
                downbeats.add(note);
            }
        }
        assertEquals(4, downbeats.size());

        for (int measure = 0; measure < 4; measure++) {
            NoteOn approach = ons.get((measure * 4) + 3);
            int nextRoot = downbeats.get((measure + 1) % downbeats.size()).note();
            assertEquals(1, Math.floorMod(nextRoot - approach.note(), 12),
                    "aproximacao no fim do compasso " + (measure + 1)
                            + " deve estar um semitom abaixo da fundamental seguinte");
        }
    }

    /** A aproximação nunca sai da região do baixo, nem na fronteira do registro. */
    @Test
    void jazzApproachStaysInBassRegion() throws Exception {
        for (String chord : new String[]{"E", "F", "G", "A", "Bb", "B", "C", "D"}) {
            Track t = generate(track(chord + " - C", "4/4"),
                    BassRhythmPattern.PRESET_JAZZ, 3840);
            for (NoteOn note : noteOns(t)) {
                assertTrue(note.note() >= VoicingService.BASS_LOW
                                && note.note() <= VoicingService.BASS_DERIVED_HIGH,
                        chord + ": aproximacao " + note.note() + " sai da regiao do baixo");
            }
        }
    }

    /** A progressão circular: o último compasso aproxima o primeiro acorde. */
    @Test
    void jazzApproachWrapsAroundTheProgression() throws Exception {
        Track t = generate(track("C - F", "4/4"), BassRhythmPattern.PRESET_JAZZ, 3840);
        List<NoteOn> ons = noteOns(t);

        assertEquals(8, ons.size(), "2 compassos x 4 tempos");
        NoteOn lastOfFirst = ons.get(3);
        NoteOn secondRoot = ons.get(4);
        assertEquals(1, Math.floorMod(secondRoot.note() - lastOfFirst.note(), 12),
                "compasso 1 aproxima o C do compasso 2 (F)");

        NoteOn lastOfSecond = ons.get(7);
        NoteOn firstRoot = ons.get(0);
        assertEquals(1, Math.floorMod(firstRoot.note() - lastOfSecond.note(), 12),
                "compasso 2 aproxima o C de volta, fechando o ciclo");
    }

    @Test
    void oddMetersGenerateWithoutError() throws Exception {
        for (String sig : new String[]{"3/4", "5/4", "6/8", "7/8", "12/8"}) {
            for (String preset : BassRhythmPattern.ALL_PRESETS) {
                Track t = generate(track("C - G", sig), preset, 7680);
                assertFalse(noteOns(t).isEmpty(), sig + "/" + preset + " deveria gerar notas");
            }
        }
    }

    @Test
    void emptyPatternGeneratesOnlyProgramChange() throws Exception {
        TimeSignatureInfo timeInfo = TimeSignatureInfo.parse("4/4");
        BassConfig config = new BassConfig(timeInfo);
        config.getPattern().clear();

        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);
        new BassMidiService(config, timeInfo)
                .generateBassTrack(sequence, track("C", "4/4"), 1920, PPQ);

        assertTrue(noteOns(sequence.getTracks()[0]).isEmpty());
    }

    @Test
    void nullConfigDoesNotCrash() throws Exception {
        TimeSignatureInfo timeInfo = TimeSignatureInfo.parse("4/4");
        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);

        new BassMidiService(null, timeInfo)
                .generateBassTrack(sequence, track("C", "4/4"), 1920, PPQ);

        assertFalse(noteOns(sequence.getTracks()[0]).isEmpty());
    }
}
