package br.com.marcosbassetto.model.guitar;

import br.com.marcosbassetto.model.music.TimeSignatureInfo;
import br.com.marcosbassetto.model.performance.VelocityLevel;
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
    void velocitiesAndProgramAreClamped() {
        GuitarConfig config = new GuitarConfig();
        config.setVelocityDownLevel(VelocityLevel.ALTO);
        config.setVelocityUpLevel(null);
        config.setVelocityPickLevel(VelocityLevel.FRACO);
        config.setProgramChange(200);
        config.setStrumOffsetTicks(-5);

        assertEquals(VelocityLevel.ALTO, config.getVelocityDownLevel());
        assertEquals(VelocityLevel.FRACO, config.getVelocityUpLevel(), "nulo e ignorado");
        assertEquals(VelocityLevel.FRACO, config.getVelocityPickLevel());
        assertEquals(127, config.getProgramChange());
        assertEquals(0, config.getStrumOffsetTicks());
    }

    @Test
    void articulationDefaultsToMistaAndIgnoresNulls() {
        GuitarConfig config = new GuitarConfig();
        assertEquals(GuitarArticulation.MISTA, config.getArticulation());
        config.setArticulation(GuitarArticulation.DEDILHADO);
        assertEquals(GuitarArticulation.DEDILHADO, config.getArticulation());
        config.setArticulation(null);
        assertEquals(GuitarArticulation.DEDILHADO, config.getArticulation());
    }

    @Test
    void articulationIsParsedFromItsLabel() {
        assertEquals(GuitarArticulation.DEDILHADO, GuitarArticulation.fromLabel("dedilhado"));
        assertEquals(GuitarArticulation.BATIDA, GuitarArticulation.fromLabel("Batida"));
        assertEquals(GuitarArticulation.MISTA, GuitarArticulation.fromLabel("mista"));
        assertEquals(GuitarArticulation.MISTA, GuitarArticulation.fromLabel("inexistente"));
        assertEquals(GuitarArticulation.MISTA, GuitarArticulation.fromLabel(null));
    }

    @Test
    void articulationControlsWhichVelocityApplies() {
        assertTrue(GuitarArticulation.BATIDA.usesStrum());
        assertNotEquals(true, GuitarArticulation.BATIDA.usesPick());
        assertTrue(GuitarArticulation.DEDILHADO.usesPick());
        assertNotEquals(true, GuitarArticulation.DEDILHADO.usesStrum());
        assertTrue(GuitarArticulation.MISTA.usesStrum());
        assertTrue(GuitarArticulation.MISTA.usesPick());
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