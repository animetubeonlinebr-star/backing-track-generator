package br.com.marcosbassetto.model.performance;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VelocityHumanizerTest {

    @Test
    void valuesStayWithinTheLevelBand() {
        VelocityHumanizer humanizer = new VelocityHumanizer(new Random(7));
        for (VelocityLevel level : VelocityLevel.values()) {
            int average = level.getAverage();
            int range = average / 3;
            for (int i = 0; i < 200; i++) {
                int velocity = humanizer.around(level);
                assertTrue(velocity >= average - range && velocity <= average + range,
                        level + ": " + velocity + " fora da banda " + average);
            }
        }
    }

    @Test
    void valuesNeverLeaveMidiRange() {
        VelocityHumanizer humanizer = new VelocityHumanizer(new Random(11));
        for (int i = 0; i < 500; i++) {
            int velocity = humanizer.around(1000);
            assertTrue(velocity >= 1 && velocity <= 127, "velocity invalida: " + velocity);
        }
    }

    /** A media da amostra precisa refletir o nivel escolhido, nao outro. */
    @Test
    void sampleMeanStaysNearTheChosenAverage() {
        VelocityHumanizer humanizer = new VelocityHumanizer(new Random(42));
        for (VelocityLevel level : VelocityLevel.values()) {
            int average = level.getAverage();
            long sum = 0;
            int samples = 5000;
            for (int i = 0; i < samples; i++) {
                sum += humanizer.around(level);
            }
            double mean = (double) sum / samples;
            assertTrue(Math.abs(mean - average) <= average / 3.0,
                    level + ": media " + mean + " distante de " + average);
        }
    }

    @Test
    void variationActuallyHappens() {
        VelocityHumanizer humanizer = new VelocityHumanizer(new Random(3));
        boolean varied = false;
        int first = humanizer.around(VelocityLevel.ALTO);
        for (int i = 0; i < 50 && !varied; i++) {
            varied = humanizer.around(VelocityLevel.ALTO) != first;
        }
        assertTrue(varied, "a humanizacao deve produzir valores diferentes");
    }

    @Test
    void nullRandomFallsBackAndNullLevelUsesDefault() {
        VelocityHumanizer humanizer = new VelocityHumanizer(null);
        int velocity = humanizer.around((VelocityLevel) null);
        int average = VelocityLevel.DEFAULT.getAverage();
        assertTrue(velocity >= average - average / 3 && velocity <= average + average / 3);
    }

    @Test
    void smallAverageStillProducesAtLeastOneStep() {
        VelocityHumanizer humanizer = new VelocityHumanizer(new Random(5));
        for (int i = 0; i < 100; i++) {
            int velocity = humanizer.around(6);
            assertTrue(velocity >= 1 && velocity <= 11, "valor inesperado: " + velocity);
        }
    }

    @Test
    void defaultConstructorIsUsable() {
        assertEquals(true, new VelocityHumanizer().around(VelocityLevel.MEDIO) > 0);
    }
}
