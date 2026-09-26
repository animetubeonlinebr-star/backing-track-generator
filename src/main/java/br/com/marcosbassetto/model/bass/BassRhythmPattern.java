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
        NONE,     // silêncio
        ROOT,     // fundamental
        THIRD,    // terça (+4 semitons; aceita a terça menor se o acorde pedir)
        FIFTH,    // quinta (+7 semitons)
        SIXTH,    // sexta (+9 semitons) — passagem
        SEVENTH,  // sétima (+10 semitons) — passagem
        OCTAVE,   // oitava (+12 semitons)
        // Aproximação cromática à fundamental do PRÓXIMO acorde. O padrão só
        // marca a posição; a nota é resolvida no serviço, que é quem conhece a
        // progressão. É o que dá o movimento do walking jazz.
        APPROACH
    }

    public static final String PRESET_FUNDAMENTAL_SIMPLES = "FUNDAMENTAL_SIMPLES";
    public static final String PRESET_FUNDAMENTAL_E_QUINTA = "FUNDAMENTAL_E_QUINTA";
    public static final String PRESET_CAMINHANTE = "CAMINHANTE";
    public static final String PRESET_REGGAE = "REGGAE";
    public static final String PRESET_BLUES_SHUFFLE = "BLUES_SHUFFLE";
    public static final String PRESET_WALKING_BLUES = "WALKING_BLUES";

    /** Presets por estilo: um para cada estilo do menu PRESETS. */
    public static final String PRESET_BLUES = "BLUES";
    public static final String PRESET_BOSSA_NOVA = "BOSSA_NOVA";
    public static final String PRESET_JAZZ = "JAZZ";
    public static final String PRESET_ROCK = "ROCK";

    /** Presets genéricos, sem vínculo com um estilo. */
    public static final String[] GENERIC_PRESETS = {
            PRESET_FUNDAMENTAL_SIMPLES,
            PRESET_FUNDAMENTAL_E_QUINTA,
            PRESET_CAMINHANTE,
            PRESET_BLUES_SHUFFLE,
            PRESET_WALKING_BLUES
    };

    public static final String[] ALL_PRESETS = {
            PRESET_FUNDAMENTAL_SIMPLES,
            PRESET_FUNDAMENTAL_E_QUINTA,
            PRESET_CAMINHANTE,
            PRESET_REGGAE,
            PRESET_BLUES_SHUFFLE,
            PRESET_WALKING_BLUES,
            PRESET_BLUES,
            PRESET_BOSSA_NOVA,
            PRESET_JAZZ,
            PRESET_ROCK
    };

    /** Estilos, na ordem em que aparecem no menu PRESETS. */
    public static final String[] STYLE_PRESETS = {
            PRESET_BLUES,
            PRESET_BOSSA_NOVA,
            PRESET_JAZZ,
            PRESET_REGGAE,
            PRESET_ROCK
    };

    /** Rótulos amigáveis dos estilos, na mesma ordem de {@link #STYLE_PRESETS}. */
    public static final String[] STYLE_LABELS = {
            "Blues", "Bossa Nova", "Jazz", "Reggae", "Rock"
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
            case PRESET_BLUES_SHUFFLE -> {
                appliedPreset = PRESET_BLUES_SHUFFLE;
                applyBluesShuffle(info);
            }
            case PRESET_WALKING_BLUES -> {
                appliedPreset = PRESET_WALKING_BLUES;
                applyWalkingBlues(info);
            }
            case PRESET_BLUES -> {
                appliedPreset = PRESET_BLUES;
                applyBluesShuffle(info);
            }
            case PRESET_BOSSA_NOVA -> {
                appliedPreset = PRESET_BOSSA_NOVA;
                applyBossaNova(info);
            }
            case PRESET_JAZZ -> {
                appliedPreset = PRESET_JAZZ;
                applyJazzWalking(info);
            }
            case PRESET_ROCK -> {
                appliedPreset = PRESET_ROCK;
                applyFundamentalSimples(info);
            }
            default -> {
                appliedPreset = PRESET_FUNDAMENTAL_SIMPLES;
                applyFundamentalSimples(info);
            }
        }
    }

    /**
     * Opções do menu "Padrão rítmico": os estilos primeiro, seguidos dos presets
     * genéricos. Um estilo nunca repete um nome genérico, então a escolha do
     * usuário é sempre resolvível de volta para um único preset.
     */
    public static String[] menuItems() {
        String[] items = new String[STYLE_LABELS.length + GENERIC_PRESETS.length];
        System.arraycopy(STYLE_LABELS, 0, items, 0, STYLE_LABELS.length);
        System.arraycopy(GENERIC_PRESETS, 0, items, STYLE_LABELS.length, GENERIC_PRESETS.length);
        return items;
    }

    /** Traduz o item exibido no menu para o nome do preset. */
    public static String presetForMenuItem(String item) {
        for (int i = 0; i < STYLE_LABELS.length; i++) {
            if (STYLE_LABELS[i].equals(item)) {
                return STYLE_PRESETS[i];
            }
        }
        return item;
    }

    /** Traduz o preset atual para o item exibido no menu (rótulo do estilo). */
    public static String menuItemForPreset(String preset) {
        for (int i = 0; i < STYLE_PRESETS.length; i++) {
            if (STYLE_PRESETS[i].equals(preset)) {
                return STYLE_LABELS[i];
            }
        }
        return preset;
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

    /**
     * Blues shuffle: fundamental na cabeça do tempo e o "repique" na tercina,
     * alternando com a quinta. 12/8 → R . . F . . R . . F . . (por tempo).
     */
    private void applyBluesShuffle(TimeSignatureInfo info) {
        if (info == null) {
            return;
        }
        int spb = info.stepsPerBeat();
        for (int beat = 0; beat < info.beatsPerMeasure(); beat++) {
            int beatStep = beat * spb;
            setNoteType(beatStep, BassNoteType.ROOT);
            setNoteType(beatStep + (spb / 3), BassNoteType.FIFTH);
        }
    }

    /**
     * Bossa Nova: fundamental na cabeça do tempo e a quinta no contratempo,
     * criando o balanço sincopado do estilo. 4/4 → R . . . . . F . R . . . . . F .
     */
    private void applyBossaNova(TimeSignatureInfo info) {
        if (info == null) {
            return;
        }
        int spb = info.stepsPerBeat();
        int offBeat = spb / 2;

        for (int beat = 0; beat < info.beatsPerMeasure(); beat++) {
            int step = beat * spb;
            setNoteType(step, BassNoteType.ROOT);
            setNoteType(step + offBeat, BassNoteType.FIFTH);
        }
    }

    /**
     * Walking jazz: uma nota por tempo, percorrendo o acorde (fundamental,
     * terça, quinta, sétima) e resolvendo o ÚLTIMO tempo numa aproximação
     * cromática à fundamental do próximo acorde — o cromatismo que define o
     * walking. A nota da aproximação é resolvida no serviço, que conhece a
     * progressão; aqui só marcamos a posição.
     *
     * <p>Com 4 tempos: R T F A (a aproximação ocupa o 4º). Em compassos de 2
     * tempos, R e aproximação. Em 3, R T e aproximação.
     */
    private void applyJazzWalking(TimeSignatureInfo info) {
        if (info == null) {
            return;
        }
        int spb = info.stepsPerBeat();
        int beats = info.beatsPerMeasure();

        if (beats <= 0) {
            return;
        }

        // Último tempo sempre aproxima o próximo acorde.
        setNoteType((beats - 1) * spb, BassNoteType.APPROACH);

        // Os tempos anteriores percorrem o acorde, sem repetir a fundamental.
        BassNoteType[] cycle = {BassNoteType.ROOT, BassNoteType.THIRD,
                BassNoteType.FIFTH, BassNoteType.SEVENTH};
        for (int beat = 0; beat < beats - 1; beat++) {
            setNoteType(beat * spb, cycle[beat % cycle.length]);
        }
    }

    /**
     * Walking blues: uma nota por tempo, andando pela escala do acorde —
     * fundamental, terça, quinta e sétima. Dá o movimento do walking bass.
     * 4/4 → R . . . T . . . F . . . S . . .
     */
    private void applyWalkingBlues(TimeSignatureInfo info) {
        if (info == null) {
            return;
        }
        int spb = info.stepsPerBeat();
        for (int beat = 0; beat < info.beatsPerMeasure(); beat++) {
            BassNoteType type = switch (beat % 4) {
                case 1 -> BassNoteType.THIRD;
                case 2 -> BassNoteType.FIFTH;
                case 3 -> BassNoteType.SEVENTH;
                default -> BassNoteType.ROOT;
            };
            setNoteType(beat * spb, type);
        }
    }
}
