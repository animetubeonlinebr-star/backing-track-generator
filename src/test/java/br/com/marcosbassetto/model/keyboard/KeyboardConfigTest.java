package br.com.marcosbassetto.model.keyboard;

import br.com.marcosbassetto.model.music.Intensity;
import br.com.marcosbassetto.model.music.TimeSignatureInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyboardConfigTest {

    private static final TimeSignatureInfo FOUR_FOUR = TimeSignatureInfo.parse("4/4");

    @Test
    void defaultsAreSensible() {
        KeyboardConfig config = new KeyboardConfig(FOUR_FOUR);
        assertEquals(Intensity.MEDIA, config.getIntensity());
        assertEquals(60, config.getVelocity(), "70 * 85%: teclado mais suave que guitarra/baixo");
        assertEquals(95, config.getNoteDurationPercent());
        assertEquals(0, config.getProgramChange(), "Acoustic Grand Piano");
        assertTrue(config.isSustainEnabled());
        assertEquals(16, config.getPattern().getTotalSteps());
        assertEquals(KeyboardRhythmPattern.PRESET_PAD_SUSTENTADO, config.getPreset());
    }

    @Test
    void nullTimeSignatureFallsBackToFourFour() {
        KeyboardConfig config = new KeyboardConfig(null);
        assertEquals(16, config.getPattern().getTotalSteps());
    }

    @Test
    void intensityDrivesMeanVelocity() {
        KeyboardConfig config = new KeyboardConfig(FOUR_FOUR);

        config.setIntensity(Intensity.BAIXA);
        assertEquals(49, config.getVelocity(), "70 * 70%");

        config.setIntensity(Intensity.FORTE);
        assertEquals(70, config.getVelocity(), "70 * 100%");
    }

    @Test
    void durationIsClampedToRange() {
        KeyboardConfig config = new KeyboardConfig(FOUR_FOUR);
        config.setNoteDurationPercent(200);
        assertEquals(KeyboardConfig.MAX_DURATION_PERCENT, config.getNoteDurationPercent());
        config.setNoteDurationPercent(10);
        assertEquals(KeyboardConfig.MIN_DURATION_PERCENT, config.getNoteDurationPercent());
    }

    @Test
    void invalidProgramChangeIsIgnored() {
        KeyboardConfig config = new KeyboardConfig(FOUR_FOUR);
        config.setProgramChange(200);
        assertEquals(0, config.getProgramChange());
        config.setProgramChange(-1);
        assertEquals(0, config.getProgramChange());
        config.setProgramChange(48);
        assertEquals(48, config.getProgramChange());
    }

    @Test
    void syncToTimeSignatureResizesPattern() {
        KeyboardConfig config = new KeyboardConfig(FOUR_FOUR);
        config.syncToTimeSignature(TimeSignatureInfo.parse("3/4"));
        assertEquals(12, config.getPattern().getTotalSteps());

        config.syncToTimeSignature(TimeSignatureInfo.parse("6/8"));
        assertEquals(12, config.getPattern().getTotalSteps());
    }

    @Test
    void applyPresetChangesPatternOnSameMeter() {
        KeyboardConfig config = new KeyboardConfig(FOUR_FOUR);
        config.applyPreset(KeyboardRhythmPattern.PRESET_ARPEJO_UP, FOUR_FOUR);

        assertEquals(KeyboardRhythmPattern.PRESET_ARPEJO_UP, config.getPreset());
        assertEquals(KeyboardRhythmPattern.AttackType.ARP_UP,
                config.getPattern().getAttack(0));
    }

    /**
     * Regressão: aplicar um preset novo junto com uma mudança de compasso não
     * pode reaplicar o preset antigo (o bug de resizeSteps com reapplyPreset).
     */
    @Test
    void applyPresetOnMeterChangeUsesTheNewPreset() {
        KeyboardConfig config = new KeyboardConfig(FOUR_FOUR);
        config.applyPreset(KeyboardRhythmPattern.PRESET_ARPEJO_DOWN,
                TimeSignatureInfo.parse("3/4"));

        assertEquals(12, config.getPattern().getTotalSteps());
        assertEquals(KeyboardRhythmPattern.PRESET_ARPEJO_DOWN, config.getPreset());
        assertEquals(KeyboardRhythmPattern.PRESET_ARPEJO_DOWN,
                config.getPattern().getAppliedPreset());
        assertEquals(KeyboardRhythmPattern.AttackType.ARP_DOWN,
                config.getPattern().getAttack(0));
    }

    @Test
    void syncWithNullTimeSignatureIsNoOp() {
        KeyboardConfig config = new KeyboardConfig(FOUR_FOUR);
        config.syncToTimeSignature(null);
        assertEquals(16, config.getPattern().getTotalSteps());
    }

    @Test
    void sustainToggles() {
        KeyboardConfig config = new KeyboardConfig(FOUR_FOUR);
        config.setSustainEnabled(false);
        assertFalse(config.isSustainEnabled());
    }
}
