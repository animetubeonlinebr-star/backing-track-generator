package br.com.marcosbassetto.model.guitar;

/**
 * Articulação da guitarra rítmica. Decide quais controles de velocity ficam
 * ativos e como os ataques do preset são executados: a batida usa as palhetadas
 * para baixo/cima, o dedilhado usa a velocity do dedilhado.
 */
public enum GuitarArticulation {

    BATIDA("Batida"),
    DEDILHADO("Dedilhado");

    private final String label;

    GuitarArticulation(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
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
        return BATIDA;
    }

    @Override
    public String toString() {
        return label;
    }
}
