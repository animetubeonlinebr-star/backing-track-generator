package br.com.marcosbassetto.model.performance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VelocityLevelTest {

    @Test
    void levelsAreOrderedFromSoftestToStrongest() {
        assertTrue(VelocityLevel.FRACO.getAverage() < VelocityLevel.MEDIO.getAverage());
        assertTrue(VelocityLevel.MEDIO.getAverage() < VelocityLevel.ALTO.getAverage());
    }

    @Test
    void averagesStayInMidiRange() {
        for (VelocityLevel level : VelocityLevel.values()) {
            assertTrue(level.getAverage() >= 1 && level.getAverage() <= 127);
            assertTrue(level.getLabel() != null && !level.getLabel().isBlank());
        }
    }

    @Test
    void fromLabelAcceptsLabelAndEnumNameRegardlessOfCase() {
        assertEquals(VelocityLevel.FRACO, VelocityLevel.fromLabel("Fraco"));
        assertEquals(VelocityLevel.ALTO, VelocityLevel.fromLabel("alto"));
        assertEquals(VelocityLevel.MEDIO, VelocityLevel.fromLabel("MEDIO"));
    }

    @Test
    void fromLabelFallsBackToDefault() {
        assertEquals(VelocityLevel.DEFAULT, VelocityLevel.fromLabel("inexistente"));
        assertEquals(VelocityLevel.DEFAULT, VelocityLevel.fromLabel(null));
    }

    @Test
    void toStringIsTheLabelUsedByTheCombos() {
        assertEquals("Médio", VelocityLevel.MEDIO.toString());
    }
}
