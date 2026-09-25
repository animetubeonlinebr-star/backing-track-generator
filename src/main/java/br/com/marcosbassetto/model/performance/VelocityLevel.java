package br.com.marcosbassetto.model.performance;

/**
 * Intensidade escolhida na UI. Cada nível define uma velocity média; o
 * {@link VelocityHumanizer} varia em torno dela para a execução não soar
 * mecânica. A média é o contrato: a variação é simétrica o suficiente para
 * mantê-la como valor central.
 */
public enum VelocityLevel {

    FRACO("Fraco", 60),
    MEDIO("Médio", 85),
    ALTO("Alto", 110);

    public static final VelocityLevel DEFAULT = MEDIO;

    private final String label;
    private final int average;

    VelocityLevel(String label, int average) {
        this.label = label;
        this.average = average;
    }

    public String getLabel() {
        return label;
    }

    /** Velocity central do nível, antes da humanização. */
    public int getAverage() {
        return average;
    }

    /** Aceita o rótulo ("Fraco") ou o nome do enum ("FRACO"), sem caixa exata. */
    public static VelocityLevel fromLabel(String value) {
        if (value != null) {
            String key = value.trim();
            for (VelocityLevel level : values()) {
                if (level.label.equalsIgnoreCase(key) || level.name().equalsIgnoreCase(key)) {
                    return level;
                }
            }
        }
        return DEFAULT;
    }

    @Override
    public String toString() {
        return label;
    }
}
