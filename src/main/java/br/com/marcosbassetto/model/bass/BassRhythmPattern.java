package br.com.marcosbassetto.model.bass;

import br.com.marcosbassetto.model.music.TimeSignatureInfo;

import java.util.Arrays;

/**
 * Define QUANDO o baixo toca e QUAL grau do acorde toca em cada passo.
 *
 * <p>Diferente da guitarra (que só decide "bate ou não"), o baixo decide o
 * grau: fundamental, quinta ou oitava. É como um baixista de backing track
 * pensa — em graus, não em voicings.
 */
public class BassRhythmPattern {

    public enum BassNoteType {
        NONE,   // silêncio
        ROOT,   // fundamental
        FIFTH,  // quinta (+7 semitons)
        OCTAVE  // oitava (+12 semitons)
    }

    public static final String PRESET_FUNDAMENTAL_SIMPLES = "FUNDAMENTAL_SIMPLES";
    public static final String PRESET_FUNDAMENTAL_E_QUINTA = "FUNDAMENTAL_E_QUINTA";
    public static final String PRESET_CAMINHANTE = "CAMINHANTE";
    public static final String PRESET_REGGAE = "REGGAE";

    public static final String[] ALL_PRESETS = {
            PRESET_FUNDAMENTAL_SIMPLES,
            PRESET_FUNDAMENTAL_E_QUINTA,
            PRESET_CAMINHANTE,
            PRESET_REGGAE
    };

    private BassNoteType[] steps;
    private String appliedPreset = PRESET_FUNDAMENTAL_SIMPLES;

    public BassRhythmPattern(int totalSteps) {
        this.steps = new BassNoteType[Math.max(1, totalSteps)];
        fillWithNone();
    }

    public BassRhythmPattern(int totalSteps, TimeSignatureInfo info, String preset) {
        this(totalSteps);
        applyPreset(preset, info);
    }

    public BassNoteType getNoteType(int step) {
        if (step < 0 || step >= steps.length) {
            return BassNoteType.NONE;
        }
        return steps[step];
    }

    public void setNoteType(int step, BassNoteType type) {
        if (step >= 0 && step < steps.length && type != null) {
            steps[step] = type;
        }
    }

    public int getTotalSteps() {
        return steps.length;
    }

    public String getAppliedPreset() {
        return appliedPreset;
    }

    public void clear() {
        fillWithNone();
    }

    private void fillWithNone() {
        Arrays.fill(steps, BassNoteType.NONE);
    }

    /**
     * Redimensiona a grade para um novo total de passos. Se {@code reapplyPreset}
     * for true, reaplica o preset atual (o tamanho 16 do padrão inicial coincide
     * com 4/4, então confiar só no tamanho pularia a aplicação). Se false,
     * preserva edições manuais copiando o que cabe.
     */
    public void resizeSteps(int newTotal, TimeSignatureInfo info, boolean reapplyPreset) {
        if (newTotal <= 0) {
            return;
        }

        if (reapplyPreset && info != null) {
            this.steps = new BassNoteType[newTotal];
            fillWithNone();
            applyPreset(appliedPreset, info);
            return;
        }

        BassNoteType[] resized = new BassNoteType[newTotal];
        Arrays.fill(resized, BassNoteType.NONE);
        System.arraycopy(steps, 0, resized, 0, Math.min(steps.length, newTotal));
        this.steps = resized;
    }

    /**
     * Aplica um preset definido RELATIVO ao compasso (nunca com posições
     * fixas — isso estouraria o array em 2/4, 3/4, 5/4 etc.).
     */
    public void applyPreset(String preset, TimeSignatureInfo info) {
        clear();

        String key = preset != null ? preset.trim().toUpperCase() : "";
        switch (key) {
            case PRESET_FUNDAMENTAL_E_QUINTA -> {
                appliedPreset = PRESET_FUNDAMENTAL_E_QUINTA;
                applyFundamentalEQuinta(info);
            }
            case PRESET_CAMINHANTE -> {
                appliedPreset = PRESET_CAMINHANTE;
                applyCaminhante(info);
            }
            case PRESET_REGGAE -> {
                appliedPreset = PRESET_REGGAE;
                applyReggae(info);
            }
            default -> {
                appliedPreset = PRESET_FUNDAMENTAL_SIMPLES;
                applyFundamentalSimples(info);
            }
        }
    }

    /** Fundamental em cada tempo. 4/4 → R . . . R . . . R . . . R . . . */
    private void applyFundamentalSimples(TimeSignatureInfo info) {
        if (info == null) {
            return;
        }
        int spb = info.stepsPerBeat();
        for (int beat = 0; beat < info.beatsPerMeasure(); beat++) {
            setNoteType(beat * spb, BassNoteType.ROOT);
        }
    }

    /** Fundamental nos tempos pares, quinta nos ímpares. 4/4 → R . . . F . . . R . . . F . . . */
    private void applyFundamentalEQuinta(TimeSignatureInfo info) {
        if (info == null) {
            return;
        }
        int spb = info.stepsPerBeat();
        for (int beat = 0; beat < info.beatsPerMeasure(); beat++) {
            setNoteType(beat * spb, beat % 2 == 0 ? BassNoteType.ROOT : BassNoteType.FIFTH);
        }
    }

    /** R → F → O → F, um por tempo, ciclando a cada 4. 4/4 → R . . . F . . . O . . . F . . . */
    private void applyCaminhante(TimeSignatureInfo info) {
        if (info == null) {
            return;
        }
        int spb = info.stepsPerBeat();
        for (int beat = 0; beat < info.beatsPerMeasure(); beat++) {
            BassNoteType type = switch (beat % 4) {
                case 1, 3 -> BassNoteType.FIFTH;
                case 2 -> BassNoteType.OCTAVE;
                default -> BassNoteType.ROOT;
            };
            setNoteType(beat * spb, type);
        }
    }

    /**
     * Fundamental no tempo 1 e nos contratempos dos tempos ímpares — deixa
     * espaço para a guitarra, marca registrada do reggae.
     * 4/4 → R . . . . . R . R . . . . . R .
     */
    private void applyReggae(TimeSignatureInfo info) {
        if (info == null) {
            return;
        }
        int spb = info.stepsPerBeat();
        int syncopation = spb / 2;

        for (int beat = 0; beat < info.beatsPerMeasure(); beat++) {
            int step = beat % 2 == 0 ? beat * spb : beat * spb + syncopation;
            setNoteType(step, BassNoteType.ROOT);
        }
    }
}
