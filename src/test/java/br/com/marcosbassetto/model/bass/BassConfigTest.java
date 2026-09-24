package br.com.marcosbassetto.model.bass;

import br.com.marcosbassetto.model.music.TimeSignatureInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class BassConfigTest {

    private static final TimeSignatureInfo FOUR_FOUR = TimeSignatureInfo.parse("4/4");

    @Test
    void defaultsAreSensible() {
        BassConfig config = new BassConfig(FOUR_FOUR);
        assertEquals(90, config.getVelocity());
        assertEquals(80, config.getNoteDurationPercent());
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
    void velocityIsClampedToRange() {
        BassConfig config = new BassConfig(FOUR_FOUR);
        config.setVelocity(200);
        assertEquals(BassConfig.MAX_VELOCITY, config.getVelocity());
        config.setVelocity(10);
        assertEquals(BassConfig.MIN_VELOCITY, config.getVelocity());
    }

    @Test
    void durationIsClampedToRange() {
        BassConfig config = new BassConfig(FOUR_FOUR);
        config.setNoteDurationPercent(200);
        assertEquals(BassConfig.MAX_DURATION_PERCENT, config.getNoteDurationPercent());
        config.setNoteDurationPercent(5);
        assertEquals(BassConfig.MIN_DURATION_PERCENT, config.getNoteDurationPercent());
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
