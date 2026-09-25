package br.com.marcosbassetto.model.keyboard;

import br.com.marcosbassetto.model.music.TimeSignatureInfo;
import br.com.marcosbassetto.model.performance.VelocityLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyboardConfigTest {

    private static final TimeSignatureInfo FOUR_FOUR = TimeSignatureInfo.parse("4/4");

    @Test
    void defaultsAreSensible() {
        KeyboardConfig config = new KeyboardConfig(FOUR_FOUR);
        assertEquals(VelocityLevel.MEDIO, config.getVelocityLevel());
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
    void velocityLevelChanges() {
        KeyboardConfig config = new KeyboardConfig(FOUR_FOUR);
        config.setVelocityLevel(VelocityLevel.FRACO);
        assertEquals(VelocityLevel.FRACO, config.getVelocityLevel());
    }

    @Test
    void nullVelocityLevelIsIgnored() {
        KeyboardConfig config = new KeyboardConfig(FOUR_FOUR);
        config.setVelocityLevel(null);
        assertEquals(VelocityLevel.MEDIO, config.getVelocityLevel());
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
