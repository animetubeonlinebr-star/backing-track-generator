package br.com.marcosbassetto.model.performance;

import java.util.Random;

/**
 * Sorteia velocities em torno de uma média, em passos de 5 e dentro da faixa
 * MIDI válida (1–127). Velocity fixa em toda a trilha soa mecânica; a variação
 * preserva a média escolhida e deixa a execução mais humana.
 *
 * <p>A amplitude é proporcional à média (±1/3), portanto um nível {@code FRACO}
 * varia pouco e um {@code ALTO} varia mais, como um músico faria.
 */
public final class VelocityHumanizer {

    private static final int STEP = 5;
    private static final int RANGE_DIVISOR = 3;
    private static final int MIN_VELOCITY = 1;
    private static final int MAX_VELOCITY = 127;

    private final Random random;

    public VelocityHumanizer() {
        this(new Random());
    }

    /** Injeta o {@link Random} para permitir testes determinísticos. */
    public VelocityHumanizer(Random random) {
        this.random = random != null ? random : new Random();
    }

    /** Velocity humanizada em torno de {@code average}. */
    public int around(int average) {
        int range = Math.max(STEP, average / RANGE_DIVISOR);
        int steps = Math.max(1, range / STEP);
        int offset = (random.nextInt(2 * steps + 1) - steps) * STEP;
        return clamp(average + offset);
    }

    /** Conveniência: humaniza a média de um nível. */
    public int around(VelocityLevel level) {
        return around(level != null ? level.getAverage() : VelocityLevel.DEFAULT.getAverage());
    }

    private static int clamp(int velocity) {
        return Math.max(MIN_VELOCITY, Math.min(MAX_VELOCITY, velocity));
    }
}
