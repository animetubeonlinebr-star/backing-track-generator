package br.com.marcosbassetto.model.music;

/**
 * Faixa de alturas (notas MIDI) preferida por um instrumento na mixagem.
 *
 * <p>Existe para que a separação de registro entre baixo, guitarra e teclado
 * seja um dado configurável — e não constantes fixas no serviço de voicing. É o
 * que permite a cada preset de estilo colocar a guitarra fora do baixo e o
 * teclado fora de ambos.
 */
public record Register(int low, int high) {

    /** Baixo: E1–G2. Região de referência da fundamental. */
    public static final Register BASS = new Register(28, 43);

    /** Guitarra: G2–E4 (voicing fechado). */
    public static final Register GUITAR = new Register(43, 64);

    /** Teclado: C4–C6 (voicing espalhado). */
    public static final Register KEYBOARD = new Register(60, 84);

    public Register {
        if (low < 0 || high > 127) {
            throw new IllegalArgumentException("Registro fora do intervalo MIDI: " + low + "-" + high);
        }
        if (low > high) {
            throw new IllegalArgumentException("Registro invertido: " + low + "-" + high);
        }
    }

    public int span() {
        return high - low;
    }

    public boolean contains(int note) {
        return note >= low && note <= high;
    }
}
