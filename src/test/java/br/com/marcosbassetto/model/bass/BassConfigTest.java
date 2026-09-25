package br.com.marcosbassetto.model.bass;

import br.com.marcosbassetto.model.music.TimeSignatureInfo;
import br.com.marcosbassetto.model.performance.VelocityLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class BassConfigTest {

    private static final TimeSignatureInfo FOUR_FOUR = TimeSignatureInfo.parse("4/4");

    @Test
    void defaultsAreSensible() {
        BassConfig config = new BassConfig(FOUR_FOUR);
        assertEquals(VelocityLevel.MEDIO, config.getVelocityLevel());
        assertEquals(33, config.getProgramChange());
        assertEquals(16, config.getPattern().getTotalSteps());
        assertEquals(BassRhythmPattern.PRESET_FUNDAMENTAL_SIMPLES, config.getPreset());
    }

    @Test
    void nullTimeSignatureFallsBackToFourFour() {
        BassConfig config = new BassConfig(null);
        assertEquals(16, config.getPattern().getTotalSteps());
    }

    @Test
    void velocityLevelChanges() {
        BassConfig config = new BassConfig(FOUR_FOUR);
        config.setVelocityLevel(VelocityLevel.ALTO);
        assertEquals(VelocityLevel.ALTO, config.getVelocityLevel());
    }

    @Test
    void nullVelocityLevelIsIgnored() {
        BassConfig config = new BassConfig(FOUR_FOUR);
        config.setVelocityLevel(null);
        assertEquals(VelocityLevel.MEDIO, config.getVelocityLevel());
    }

    @Test
    void invalidProgramChangeIsIgnored() {
        BassConfig config = new BassConfig(FOUR_FOUR);
        config.setProgramChange(200);
        assertEquals(33, config.getProgramChange());
        config.setProgramChange(-1);
        assertEquals(33, config.getProgramChange());
        config.setProgramChange(35);
        assertEquals(35, config.getProgramChange());
    }

    @Test
    void syncToTimeSignatureResizesPattern() {
        BassConfig config = new BassConfig(FOUR_FOUR);
        assertEquals(16, config.getPattern().getTotalSteps());

        config.syncToTimeSignature(TimeSignatureInfo.parse("3/4"));
        assertEquals(12, config.getPattern().getTotalSteps());

        config.syncToTimeSignature(TimeSignatureInfo.parse("6/8"));
        assertEquals(12, config.getPattern().getTotalSteps());
    }

    @Test
    void applyPresetChangesPatternOnSameMeter() {
        BassConfig config = new BassConfig(FOUR_FOUR);
        config.applyPreset(BassRhythmPattern.PRESET_CAMINHANTE, FOUR_FOUR);

        assertEquals(BassRhythmPattern.PRESET_CAMINHANTE, config.getPreset());
        assertNotEquals(BassRhythmPattern.BassNoteType.NONE,
                config.getPattern().getNoteType(8));
    }

    /**
     * Regressão: aplicar um preset novo junto com uma mudança de compasso não
     * pode reaplicar o preset antigo (o bug de resizeSteps com reapplyPreset).
     */
    @Test
    void applyPresetOnMeterChangeUsesTheNewPreset() {
        BassConfig config = new BassConfig(FOUR_FOUR);
        config.applyPreset(BassRhythmPattern.PRESET_REGGAE,
                TimeSignatureInfo.parse("3/4"));

        assertEquals(12, config.getPattern().getTotalSteps());
        assertEquals(BassRhythmPattern.PRESET_REGGAE, config.getPreset());
        assertEquals(BassRhythmPattern.PRESET_REGGAE, config.getPattern().getAppliedPreset());
        assertEquals(BassRhythmPattern.BassNoteType.ROOT, config.getPattern().getNoteType(0));
        assertEquals(BassRhythmPattern.BassNoteType.ROOT, config.getPattern().getNoteType(6));
    }

    @Test
    void syncWithNullTimeSignatureIsNoOp() {
        BassConfig config = new BassConfig(FOUR_FOUR);
        config.syncToTimeSignature(null);
        assertEquals(16, config.getPattern().getTotalSteps());
    }
}
