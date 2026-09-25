package br.com.marcosbassetto.model.guitar;

import br.com.marcosbassetto.model.music.Intensity;
import br.com.marcosbassetto.model.music.TimeSignatureInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuitarConfigTest {

    @Test
    void syncAppliesPresetEvenWhenDefaultSizeMatchesFourFour() {
        TimeSignatureInfo info = TimeSignatureInfo.parse("4/4"); // totalSteps 16 == tamanho inicial
        GuitarConfig config = new GuitarConfig();
        config.setPreset(GuitarRhythmPattern.PRESET_BATIDA_ROCK);

        config.syncPatternTo(info);

        assertEquals(GuitarRhythmPattern.AttackType.STRUM_DOWN, config.getPattern().getAttack(0),
                "preset deve ser aplicado mesmo sem mudanca de tamanho");
        assertNotEquals(GuitarRhythmPattern.AttackType.NONE, config.getPattern().getAttack(2));
    }

    @Test
    void syncResizesPatternWhenMeasureChanges() {
        GuitarConfig config = new GuitarConfig();
        config.setPreset(GuitarRhythmPattern.PRESET_BATIDA_ROCK);

        config.syncPatternTo(TimeSignatureInfo.parse("4/4"));
        assertEquals(16, config.getPattern().getTotalSteps());

        config.syncPatternTo(TimeSignatureInfo.parse("3/4"));
        assertEquals(12, config.getPattern().getTotalSteps());

        config.syncPatternTo(TimeSignatureInfo.parse("6/8"));
        assertEquals(12, config.getPattern().getTotalSteps());
    }

    @Test
    void syncDoesNotClobberManualEditsOnUnchangedMeasure() {
        GuitarConfig config = new GuitarConfig();
        config.setPreset(GuitarRhythmPattern.PRESET_BATIDA_ROCK);
        config.syncPatternTo(TimeSignatureInfo.parse("4/4"));

        config.getPattern().setAttack(1, GuitarRhythmPattern.AttackType.PICK);
        config.syncPatternTo(TimeSignatureInfo.parse("4/4"));

        assertEquals(GuitarRhythmPattern.AttackType.PICK, config.getPattern().getAttack(1),
                "edicoes manuais devem sobreviver a um sync sem mudanca");
    }

    @Test
    void reapplyPresetDiscardsManualEdits() {
        GuitarConfig config = new GuitarConfig();
        config.setPreset(GuitarRhythmPattern.PRESET_BATIDA_ROCK);
        config.syncPatternTo(TimeSignatureInfo.parse("4/4"));
        config.getPattern().setAttack(1, GuitarRhythmPattern.AttackType.PICK);

        config.reapplyPreset(TimeSignatureInfo.parse("4/4"));

        assertEquals(GuitarRhythmPattern.AttackType.NONE, config.getPattern().getAttack(1));
        assertEquals(GuitarRhythmPattern.AttackType.STRUM_UP, config.getPattern().getAttack(2));
    }

    @Test
    void syncWithNullTimeSignatureIsNoOp() {
        GuitarConfig config = new GuitarConfig();
        config.syncPatternTo(null);
        assertEquals(16, config.getPattern().getTotalSteps());
    }

    @Test
    void intensityScalesVelocitiesWithoutClamping() {
        GuitarConfig config = new GuitarConfig();

        config.setIntensity(Intensity.FORTE);
        assertEquals(80, config.getVelocityDown());
        assertEquals(65, config.getVelocityUp());
        assertEquals(75, config.getVelocityPick());

        config.setIntensity(Intensity.MEDIA);
        assertEquals(68, config.getVelocityDown());
        assertEquals(55, config.getVelocityUp());
        assertEquals(64, config.getVelocityPick());

        config.setIntensity(Intensity.BAIXA);
        assertEquals(56, config.getVelocityDown());
        assertEquals(46, config.getVelocityUp());
        assertEquals(53, config.getVelocityPick());
    }

    @Test
    void programIsClampedAndStrumOffsetNeverNegative() {
        GuitarConfig config = new GuitarConfig();
        config.setProgramChange(200);
        config.setStrumOffsetTicks(-5);

        assertEquals(127, config.getProgramChange());
        assertEquals(0, config.getStrumOffsetTicks());
    }

    @Test
    void settersIgnoreNulls() {
        GuitarConfig config = new GuitarConfig();
        String preset = config.getPreset();
        config.setPreset(null);
        config.setPattern(null);
        assertEquals(preset, config.getPreset());
        assertTrue(config.getPattern().getTotalSteps() > 0);
    }
}