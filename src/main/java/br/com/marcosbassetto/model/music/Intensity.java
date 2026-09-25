package br.com.marcosbassetto.model.music;

/**
 * Nível de intensidade executado pelo instrumento. Substitui o antigo controle
 * numérico de velocity: em vez de um valor cru, o usuário escolhe baixa, média
 * ou forte, e cada instrumento deriva sua velocity média a partir disso.
 *
 * <p>O percentual é aplicado sobre a velocity-base de cada instrumento, de modo
 * que a relação de força entre os instrumentos (ex.: bateria mais forte que o
 * teclado) se mantenha em todos os níveis.
 */
public enum Intensity {

    BAIXA("Baixa", 70),
    MEDIA("Média", 85),
    FORTE("Forte", 100);

    private final String label;
    private final int percent;

    Intensity(String label, int percent) {
        this.label = label;
        this.percent = percent;
    }

    public String getLabel() {
        return label;
    }

    /** Escala uma velocity-base para o nível atual, sempre dentro de 1–127. */
    public int scale(int baseVelocity) {
        int scaled = Math.round(baseVelocity * percent / 100f);
        return Math.max(1, Math.min(127, scaled));
    }

    /** Resolve pelo rótulo exibido na UI, caindo em {@link #MEDIA} se desconhecido. */
    public static Intensity fromLabel(String label) {
        if (label != null) {
            for (Intensity intensity : values()) {
                if (intensity.label.equals(label)) {
                    return intensity;
                }
            }
        }
        return MEDIA;
    }

    @Override
    public String toString() {
        return label;
    }
}
