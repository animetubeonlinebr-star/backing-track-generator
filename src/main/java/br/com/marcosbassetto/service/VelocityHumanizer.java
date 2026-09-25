package br.com.marcosbassetto.service;

import java.util.Random;

/**
 * Aplica variação natural (humanize) à velocity das notas.
 *
 * <p>Uma execução MIDI com velocity fixa soa mecânica. Aqui cada nota recebe um
 * pequeno desvio aleatório em torno da velocity média do instrumento, mantendo a
 * média geral no nível escolhido (intensidade baixa/média/forte) mas evitando
 * que todas as notas tenham exatamente o mesmo valor.
 *
 * <p>Além do desvio aleatório há uma leve tendência de fraseado: o primeiro
 * tempo do compasso e as notas mais graves do acorde soam um pouco mais fortes,
 * como acontece numa execução real.
 */
public class VelocityHumanizer {

    private static final int MIN_VELOCITY = 1;
    private static final int MAX_VELOCITY = 127;

    private final Random random;
    private final int maximumDeviation;

    public VelocityHumanizer() {
        this(new Random(), 8);
    }

    public VelocityHumanizer(Random random) {
        this(random, 8);
    }

    /**
     * @param random            fonte de aleatoriedade (permite testes determinísticos)
     * @param maximumDeviation  desvio máximo, em unidades de velocity, aplicado a cada nota
     */
    public VelocityHumanizer(Random random, int maximumDeviation) {
        this.random = random != null ? random : new Random();
        this.maximumDeviation = Math.max(0, maximumDeviation);
    }

    /**
     * Humaniza a velocity de uma nota.
     *
     * @param meanVelocity velocity média (resultado da intensidade escolhida)
     * @param accent       tendência de fraseado; positivo acentua, negativo suaviza
     */
    public int humanize(int meanVelocity, int accent) {
        int deviation = maximumDeviation == 0
                ? 0
                : random.nextInt((maximumDeviation * 2) + 1) - maximumDeviation;
        return clamp(meanVelocity + deviation + accent);
    }

    /** Humaniza sem acento de fraseado. */
    public int humanize(int meanVelocity) {
        return humanize(meanVelocity, 0);
    }

    /** Acento do downbeat: o tempo 1 de cada compasso toca um pouco mais forte. */
    public int downbeatAccent() {
        return 5;
    }

    /**
     * Acento por posição do compasso. O primeiro tempo é acentuado; os tempos
     * seguintes ficam neutros ou levemente suavizados para dar respiração.
     */
    public int accentForStep(int step, int stepsPerBeat) {
        if (stepsPerBeat <= 0) {
            return 0;
        }
        int beat = step / stepsPerBeat;
        return beat == 0 ? downbeatAccent() : 0;
    }

    /**
     * Acento por posição no acorde: notas mais graves sustentam a harmonia e
     * soam ligeiramente mais fortes que as agudas.
     */
    public int accentForChordTone(int index, int total) {
        if (total <= 1) {
            return 0;
        }
        double normalized = (double) index / (total - 1); // 0 = grave, 1 = agudo
        return (int) Math.round(3 - (normalized * 6));
    }

    private static int clamp(int velocity) {
        return Math.max(MIN_VELOCITY, Math.min(MAX_VELOCITY, velocity));
    }
}
