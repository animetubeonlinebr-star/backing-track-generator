package br.com.marcosbassetto.model.guitar;

/**
 * Articulação da guitarra rítmica. Decide quais controles de velocity ficam
 * ativos e como os ataques do preset são executados: a batida usa as palhetadas
 * para baixo/cima, o dedilhado usa a velocity do dedilhado e a articulação
 * mista mantém o preset como está (batida e dedilhado convivendo).
 */
public enum GuitarArticulation {

    BATIDA("Batida"),
    DEDILHADO("Dedilhado"),
    MISTA("Mista");

    private final String label;

    GuitarArticulation(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** A batida (ou a mista) executa palhetadas para baixo e para cima. */
    public boolean usesStrum() {
        return this != DEDILHADO;
    }

    /** O dedilhado (ou a mista) usa a velocity do dedilhado. */
    public boolean usesPick() {
        return this != BATIDA;
    }

    /** Aceita o rótulo ("Batida") ou o nome do enum ("BATIDA"), sem caixa exata. */
    public static GuitarArticulation fromLabel(String value) {
        if (value != null) {
            String key = value.trim();
            for (GuitarArticulation articulation : values()) {
                if (articulation.label.equalsIgnoreCase(key)
                        || articulation.name().equalsIgnoreCase(key)) {
                    return articulation;
                }
            }
        }
        return MISTA;
    }

    @Override
    public String toString() {
        return label;
    }
}
