package br.com.marcosbassetto.service;

import br.com.marcosbassetto.model.bass.BassConfig;
import br.com.marcosbassetto.model.bass.BassRhythmPattern;
import br.com.marcosbassetto.model.music.BackingTrack;
import br.com.marcosbassetto.model.music.TimeSignatureInfo;
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
        TimeSignatureInfo timeInfo = TimeSignatureInfo.parse(backingTrack.getMeasure());
        BassConfig config = new BassConfig(timeInfo);
        config.applyPreset(preset, timeInfo);

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
    void velocityAndDurationComeFromConfig() throws Exception {
        TimeSignatureInfo timeInfo = TimeSignatureInfo.parse("4/4");
        BassConfig config = new BassConfig(timeInfo);
        config.setVelocity(100);
        config.setNoteDurationPercent(50);

        Sequence sequence = new Sequence(Sequence.PPQ, PPQ);
        new BassMidiService(config, timeInfo)
                .generateBassTrack(sequence, track("C", "4/4"), 1920, PPQ);
        Track t = sequence.getTracks()[0];

        long stepTicks = timeInfo.getStepTicks(PPQ);
        long expectedDuration = (stepTicks * 50) / 100;

        NoteOn first = noteOns(t).get(0);
        assertEquals(100, first.velocity());
        long noteOffTick = -1;
        for (int i = 0; i < t.size(); i++) {
            if (t.get(i).getMessage() instanceof ShortMessage sm
                    && sm.getCommand() == ShortMessage.NOTE_OFF) {
                noteOffTick = t.get(i).getTick();
                break;
            }
        }
        assertEquals(first.tick() + expectedDuration, noteOffTick);
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
