package br.com.marcosbassetto.model.music;

import br.com.marcosbassetto.model.bass.BassRhythmPattern;
import br.com.marcosbassetto.model.guitar.GuitarRhythmPattern;
import br.com.marcosbassetto.model.keyboard.KeyboardRhythmPattern;

/**
 * Preset de estilo musical. Cada constante reúne a configuração padrão aplicada
 * quando o usuário escolhe o estilo no menu PRESETS:
 *
 * <ul>
 *   <li>o compasso gerado no formulário principal;</li>
 *   <li>a intensidade e o timbre de cada instrumento;</li>
 *   <li>a região de alturas de cada instrumento na mixagem;</li>
 *   <li>o padrão rítmico de bateria, baixo, guitarra e teclado.</li>
 * </ul>
 *
 * <p>As três regiões são deliberadamente disjuntas: baixo ≤ 40, guitarra 43–64 e
 * teclado ≥ 65. Assim a guitarra nunca ocupa o espaço do baixo, e o teclado
 * nunca ocupa o espaço de nenhum dos dois — em todos os estilos.
 */
public enum StylePreset {

    BLUES("Blues", "12/8", Intensity.MEDIA,
            new Register(28, 40),   // baixo: E1–E2
            new Register(43, 64),   // guitarra: G2–E4
            new Register(67, 86),   // teclado: G4–D6 (acima da guitarra)
            GuitarRhythmPattern.PRESET_BLUES,
            BassRhythmPattern.PRESET_BLUES_SHUFFLE,
            KeyboardRhythmPattern.PRESET_BLOCO_RITMICO,
            27, 33, 4),

    BOSSA_NOVA("Bossa Nova", "4/4", Intensity.BAIXA,
            new Register(28, 40),
            new Register(45, 64),   // guitarra começa acima do teto do baixo
            new Register(65, 84),
            GuitarRhythmPattern.PRESET_BOSSA_NOVA,
            BassRhythmPattern.PRESET_FUNDAMENTAL_E_QUINTA,
            KeyboardRhythmPattern.PRESET_BOSSA_NOVA,
            24, 32, 4),

    JAZZ("Jazz", "4/4", Intensity.MEDIA,
            new Register(28, 40),
            new Register(43, 64),
            new Register(65, 86),
            GuitarRhythmPattern.PRESET_DEDILHADO,
            BassRhythmPattern.PRESET_CAMINHANTE,
            KeyboardRhythmPattern.PRESET_ARPEJO_UP,
            26, 32, 0),

    REGGAE("Reggae", "4/4", Intensity.MEDIA,
            new Register(28, 38),   // baixo mais grave: abre espaço para o skank
            new Register(43, 64),
            new Register(65, 86),
            GuitarRhythmPattern.PRESET_REGGAE,
            BassRhythmPattern.PRESET_REGGAE,
            KeyboardRhythmPattern.PRESET_REGGAE,
            28, 33, 16),

    ROCK("Rock", "4/4", Intensity.FORTE,
            new Register(28, 40),
            new Register(43, 64),
            new Register(65, 86),
            GuitarRhythmPattern.PRESET_BATIDA_ROCK,
            BassRhythmPattern.PRESET_FUNDAMENTAL_SIMPLES,
            KeyboardRhythmPattern.PRESET_PAD_SUSTENTADO,
            30, 34, 17);

    private final String label;
    private final String measure;
    private final Intensity intensity;
    private final Register bassRegister;
    private final Register guitarRegister;
    private final Register keyboardRegister;
    private final String guitarPattern;
    private final String bassPattern;
    private final String keyboardPattern;
    private final int guitarProgram;
    private final int bassProgram;
    private final int keyboardProgram;

    StylePreset(String label, String measure, Intensity intensity,
                Register bassRegister, Register guitarRegister, Register keyboardRegister,
                String guitarPattern, String bassPattern, String keyboardPattern,
                int guitarProgram, int bassProgram, int keyboardProgram) {
        this.label = label;
        this.measure = measure;
        this.intensity = intensity;
        this.bassRegister = bassRegister;
        this.guitarRegister = guitarRegister;
        this.keyboardRegister = keyboardRegister;
        this.guitarPattern = guitarPattern;
        this.bassPattern = bassPattern;
        this.keyboardPattern = keyboardPattern;
        this.guitarProgram = guitarProgram;
        this.bassProgram = bassProgram;
        this.keyboardProgram = keyboardProgram;
    }

    /** Nome exibido no menu. */
    public String getLabel() {
        return label;
    }

    /** Compasso padrão do estilo, no formato {@code N/D}. */
    public String getMeasure() {
        return measure;
    }

    public Intensity getIntensity() {
        return intensity;
    }

    public Register getBassRegister() {
        return bassRegister;
    }

    public Register getGuitarRegister() {
        return guitarRegister;
    }

    public Register getKeyboardRegister() {
        return keyboardRegister;
    }

    public String getGuitarPattern() {
        return guitarPattern;
    }

    public String getBassPattern() {
        return bassPattern;
    }

    public String getKeyboardPattern() {
        return keyboardPattern;
    }

    public int getGuitarProgram() {
        return guitarProgram;
    }

    public int getBassProgram() {
        return bassProgram;
    }

    public int getKeyboardProgram() {
        return keyboardProgram;
    }

    /** Resolve pelo rótulo exibido no menu, ou {@code null} se não existir. */
    public static StylePreset fromLabel(String label) {
        if (label != null) {
            for (StylePreset style : values()) {
                if (style.label.equals(label)) {
                    return style;
                }
            }
        }
        return null;
    }

    /** Nomes exibidos no menu, na ordem de declaração. */
    public static String[] allLabels() {
        StylePreset[] styles = values();
        String[] labels = new String[styles.length];
        for (int i = 0; i < styles.length; i++) {
            labels[i] = styles[i].label;
        }
        return labels;
    }

    @Override
    public String toString() {
        return label;
    }
}
