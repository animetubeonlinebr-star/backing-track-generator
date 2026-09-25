package br.com.marcosbassetto.service;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VelocityHumanizerTest {

    @Test
    void staysWithinDeviationBounds() {
        VelocityHumanizer humanizer = new VelocityHumanizer(new Random(42), 8);

        for (int i = 0; i < 1000; i++) {
            int velocity = humanizer.humanize(90, 0);
            assertTrue(velocity >= 82 && velocity <= 98, "fora do desvio: " + velocity);
        }
    }

    @Test
    void keepsTheMeanCloseToTheRequestedLevel() {
        VelocityHumanizer humanizer = new VelocityHumanizer(new Random(7), 8);

        long sum = 0;
        int count = 10_000;
        for (int i = 0; i < count; i++) {
            sum += humanizer.humanize(80, 0);
        }
        double mean = (double) sum / count;

        assertTrue(Math.abs(mean - 80) <= 1, "media deveria ficar em ~80, foi " + mean);
    }

    @Test
    void accentShiftsTheResultUpOrDown() {
        VelocityHumanizer flat = new VelocityHumanizer(new Random(1), 0);
        assertEquals(95, flat.humanize(90, flat.downbeatAccent()));

        VelocityHumanizer negative = new VelocityHumanizer(new Random(1), 0);
        assertEquals(87, negative.humanize(90, negative.accentForChordTone(2, 3)));
    }

    @Test
    void neverLeavesTheMidiRange() {
        VelocityHumanizer humanizer = new VelocityHumanizer(new Random(3), 8);

        for (int i = 0; i < 1000; i++) {
            assertTrue(humanizer.humanize(1, 0) >= 1);
            assertTrue(humanizer.humanize(127, 0) <= 127);
        }
    }

    @Test
    void downbeatIsAccentedOnlyOnTheFirstBeat() {
        VelocityHumanizer humanizer = new VelocityHumanizer(new Random(5), 0);

        assertEquals(humanizer.downbeatAccent(), humanizer.accentForStep(0, 4));
        assertEquals(0, humanizer.accentForStep(4, 4), "segundo tempo sem acento");
        assertEquals(0, humanizer.accentForStep(5, 4));
    }

    @Test
    void invalidStepsPerBeatIsNeutral() {
        VelocityHumanizer humanizer = new VelocityHumanizer(new Random(5), 0);
        assertEquals(0, humanizer.accentForStep(0, 0));
    }

    @Test
    void chordToneAccentIsStrongestForTheLowestNote() {
        VelocityHumanizer humanizer = new VelocityHumanizer(new Random(5), 0);

        int low = humanizer.accentForChordTone(0, 3);
        int mid = humanizer.accentForChordTone(1, 3);
        int high = humanizer.accentForChordTone(2, 3);

        assertTrue(low > mid, "a fundamental deve ser mais forte que a terceira");
        assertTrue(mid > high, "a segunda nota deve ser mais forte que a aguda");
        assertEquals(0, humanizer.accentForChordTone(0, 1), "nota unica nao tem acento");
    }
}
