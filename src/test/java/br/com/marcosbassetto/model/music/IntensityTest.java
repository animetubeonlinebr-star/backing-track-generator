package br.com.marcosbassetto.model.music;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntensityTest {

    @Test
    void scalesBaseVelocityByLevel() {
        assertEquals(56, Intensity.BAIXA.scale(80));
        assertEquals(68, Intensity.MEDIA.scale(80));
        assertEquals(80, Intensity.FORTE.scale(80));
    }

    @Test
    void louderIntensityAlwaysMeansStrongerVelocity() {
        for (int base : new int[]{40, 70, 90, 100}) {
            assertTrue(Intensity.BAIXA.scale(base) < Intensity.MEDIA.scale(base));
            assertTrue(Intensity.MEDIA.scale(base) < Intensity.FORTE.scale(base));
        }
    }

    @Test
    void neverLeavesTheMidiRange() {
        for (Intensity intensity : Intensity.values()) {
            assertTrue(intensity.scale(127) <= 127);
            assertTrue(intensity.scale(0) >= 1);
        }
    }

    @Test
    void resolvesByDisplayedLabel() {
        assertEquals(Intensity.BAIXA, Intensity.fromLabel("Baixa"));
        assertEquals(Intensity.FORTE, Intensity.fromLabel("Forte"));
        assertEquals(Intensity.MEDIA, Intensity.fromLabel("desconhecido"));
        assertEquals(Intensity.MEDIA, Intensity.fromLabel(null));
    }
}
