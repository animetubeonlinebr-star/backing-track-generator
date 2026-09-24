package br.com.marcosbassetto.model.guitar;

import br.com.marcosbassetto.model.music.TimeSignatureInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuitarRhythmPatternTest {

    @Test
    void newPatternIsFilledWithNoneNotNulls() {
        GuitarRhythmPattern pattern = new GuitarRhythmPattern(16);
        for (int step = 0; step < pattern.getTotalSteps(); step++) {
            assertNotNull(pattern.getAttack(step), "nenhum passo pode ser null");
            assertEquals(GuitarRhythmPattern.AttackType.NONE, pattern.getAttack(step));
        }
    }

    @Test
    void getAttackOutsideRangeReturnsNone() {
        GuitarRhythmPattern pattern = new GuitarRhythmPattern(8);
        assertEquals(GuitarRhythmPattern.AttackType.NONE, pattern.getAttack(-1));
        assertEquals(GuitarRhythmPattern.AttackType.NONE, pattern.getAttack(999));
    }

    @Test
    void setAttackOutsideRangeIsIgnoredInsteadOfThrowing() {
        GuitarRhythmPattern pattern = new GuitarRhythmPattern(8);
        pattern.setAttack(50, GuitarRhythmPattern.AttackType.STRUM_DOWN);
        assertEquals(GuitarRhythmPattern.AttackType.NONE, pattern.getAttack(0));
    }

    @Test
    void nullAttackIsNormalizedToNone() {
        GuitarRhythmPattern pattern = new GuitarRhythmPattern(4);
        pattern.setAttack(0, null);
        assertEquals(GuitarRhythmPattern.AttackType.NONE, pattern.getAttack(0));
    }

    @Test
    void invalidStepCountRejected() {
        assertThrows(IllegalArgumentException.class, () -> new GuitarRhythmPattern(0));
    }

    @Test
    void basicPresetPlacesDownBeatsOnEveryBeat() {
        TimeSignatureInfo info = TimeSignatureInfo.parse("4/4");
        GuitarRhythmPattern pattern = new GuitarRhythmPattern(info.totalSteps());
        pattern.applyPreset(GuitarRhythmPattern.PRESET_BATIDA_BASICA, info);

        int spb = info.stepsPerBeat();
        for (int beat = 0; beat < info.beatsPerMeasure(); beat++) {
            assertEquals(GuitarRhythmPattern.AttackType.STRUM_DOWN, pattern.getAttack(beat * spb),
                    "tempo " + beat + " deve ser para baixo");
        }
    }

    @Test
    void basicPresetDoesNotThrowOnOddMeters() {
        for (String sig : new String[]{"2/4", "3/4", "5/4", "6/8", "7/8", "12/8", "3/8"}) {
            TimeSignatureInfo info = TimeSignatureInfo.parse(sig);
            GuitarRhythmPattern pattern = new GuitarRhythmPattern(info.totalSteps());
            pattern.applyPreset(GuitarRhythmPattern.PRESET_BATIDA_BASICA, info);
            assertTrue(pattern.getTotalSteps() > 0, sig + " deve gerar passos");
        }
    }

    @Test
    void rockPresetAlternatesDownAndUp() {
        TimeSignatureInfo info = TimeSignatureInfo.parse("4/4");
        GuitarRhythmPattern pattern = new GuitarRhythmPattern(info.totalSteps());
        pattern.applyPreset(GuitarRhythmPattern.PRESET_BATIDA_ROCK, info);

        int spb = info.stepsPerBeat();
        assertEquals(GuitarRhythmPattern.AttackType.STRUM_DOWN, pattern.getAttack(0));
        assertEquals(GuitarRhythmPattern.AttackType.STRUM_UP, pattern.getAttack(spb / 2));
    }

    @Test
    void reggaePresetOnlyAttacksOffBeat() {
        TimeSignatureInfo info = TimeSignatureInfo.parse("4/4");
        GuitarRhythmPattern pattern = new GuitarRhythmPattern(info.totalSteps());
        pattern.applyPreset(GuitarRhythmPattern.PRESET_REGGAE, info);

        int spb = info.stepsPerBeat();
        for (int beat = 0; beat < info.beatsPerMeasure(); beat++) {
            assertEquals(GuitarRhythmPattern.AttackType.NONE, pattern.getAttack(beat * spb),
                    "reggae nao ataca no tempo forte");
            assertEquals(GuitarRhythmPattern.AttackType.STRUM_UP,
                    pattern.getAttack(beat * spb + (spb / 2)));
        }
    }

    @Test
    void dedilhadoPresetUsesPickAttacks() {
        TimeSignatureInfo info = TimeSignatureInfo.parse("4/4");
        GuitarRhythmPattern pattern = new GuitarRhythmPattern(info.totalSteps());
        pattern.applyPreset(GuitarRhythmPattern.PRESET_DEDILHADO, info);

        assertEquals(GuitarRhythmPattern.AttackType.PICK, pattern.getAttack(0));
        boolean anyPick = false;
        for (int step = 0; step < pattern.getTotalSteps(); step++) {
            if (pattern.getAttack(step) == GuitarRhythmPattern.AttackType.PICK) {
                anyPick = true;
            }
        }
        assertTrue(anyPick);
    }

    @Test
    void unknownPresetFallsBackToBasic() {
        TimeSignatureInfo info = TimeSignatureInfo.parse("4/4");
        GuitarRhythmPattern pattern = new GuitarRhythmPattern(info.totalSteps());
        pattern.applyPreset("NAO_EXISTE", info);
        assertEquals(GuitarRhythmPattern.AttackType.STRUM_DOWN, pattern.getAttack(0));
    }

    @Test
    void applyPresetWithNullTimeSignatureJustClears() {
        GuitarRhythmPattern pattern = new GuitarRhythmPattern(4);
        pattern.setAttack(0, GuitarRhythmPattern.AttackType.PICK);
        pattern.applyPreset(GuitarRhythmPattern.PRESET_BATIDA_ROCK, null);
        assertEquals(GuitarRhythmPattern.AttackType.NONE, pattern.getAttack(0));
    }

    @Test
    void resizePreservesExistingStepsAndFillsNewWithNone() {
        GuitarRhythmPattern pattern = new GuitarRhythmPattern(4);
        pattern.setAttack(0, GuitarRhythmPattern.AttackType.STRUM_DOWN);

        pattern.resizeSteps(8);

        assertEquals(8, pattern.getTotalSteps());
        assertEquals(GuitarRhythmPattern.AttackType.STRUM_DOWN, pattern.getAttack(0));
        assertEquals(GuitarRhythmPattern.AttackType.NONE, pattern.getAttack(7));
    }
}
