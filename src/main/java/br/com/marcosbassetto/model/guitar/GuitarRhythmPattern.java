package br.com.marcosbassetto.model.guitar;

import br.com.marcosbassetto.model.music.TimeSignatureInfo;

import java.util.Arrays;

/**
 * Padrão rítmico da guitarra: para cada passo do compasso, define SE há ataque
 * e QUAL o tipo. É independente do acorde — o mesmo padrão serve para qualquer
 * progressão, exatamente como um guitarrista rítmico pensa.
 */
public class GuitarRhythmPattern {

    public enum AttackType { STRUM_DOWN, STRUM_UP, PICK, NONE }

    public static final String PRESET_BATIDA_BASICA = "BATIDA_BASICA";
    public static final String PRESET_BATIDA_ROCK = "BATIDA_ROCK";
    public static final String PRESET_DEDILHADO = "DEDILHADO";
    public static final String PRESET_BATIDA_BALADA = "BATIDA_BALADA";

    /** Presets por estilo: um para cada estilo do menu PRESETS. */
    public static final String PRESET_BLUES = "BLUES";
    public static final String PRESET_BOSSA_NOVA = "BOSSA_NOVA";
    public static final String PRESET_JAZZ = "JAZZ";
    public static final String PRESET_REGGAE = "REGGAE";
    public static final String PRESET_ROCK = "ROCK";

    /** Presets genéricos, sem vínculo com um estilo. */
    public static final String[] GENERIC_PRESETS = {
            PRESET_BATIDA_BASICA,
            PRESET_BATIDA_ROCK,
            PRESET_DEDILHADO,
            PRESET_BATIDA_BALADA
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

    /** Todos os presets (estilos + genéricos), para varreduras e testes. */
    public static final String[] ALL_PRESETS = {
            PRESET_BLUES,
            PRESET_BOSSA_NOVA,
            PRESET_JAZZ,
            PRESET_REGGAE,
            PRESET_ROCK,
            PRESET_BATIDA_BASICA,
            PRESET_BATIDA_ROCK,
            PRESET_DEDILHADO,
            PRESET_BATIDA_BALADA
    };

    private AttackType[] steps;

    public GuitarRhythmPattern(int totalSteps) {
        if (totalSteps <= 0) {
            throw new IllegalArgumentException("totalSteps deve ser positivo");
        }
        this.steps = new AttackType[totalSteps];
        clear();
    }

    public AttackType getAttack(int step) {
        if (step < 0 || step >= steps.length) {
            return AttackType.NONE;
        }
        return steps[step];
    }

    public void setAttack(int step, AttackType type) {
        if (step < 0 || step >= steps.length) {
            return;
        }
        steps[step] = type != null ? type : AttackType.NONE;
    }

    public int getTotalSteps() {
        return steps.length;
    }

    public void resizeSteps(int newTotal) {
        if (newTotal <= 0) {
            return;
        }
        AttackType[] resize = new AttackType[newTotal];
        Arrays.fill(resize, AttackType.NONE);
        System.arraycopy(steps, 0, resize, 0, Math.min(steps.length, newTotal));
        this.steps = resize;
    }

    public void clear() {
        Arrays.fill(steps, AttackType.NONE);
    }

    public void applyPreset(String preset, TimeSignatureInfo info) {
        clear();
        if (info == null) {
            return;
        }
        String key = preset != null ? preset.trim().toUpperCase() : "";
        switch (key) {
            case PRESET_BATIDA_ROCK -> applyBatidaRock(info);
            case PRESET_DEDILHADO -> applyDedilhado(info);
            case PRESET_BATIDA_BALADA -> applyBatidaBalada(info);
            case PRESET_REGGAE -> applyReggae(info);
            case PRESET_BLUES -> applyBlues(info);
            case PRESET_BOSSA_NOVA -> applyBossaNova(info);
            case PRESET_JAZZ -> applyJazz(info);
            case PRESET_ROCK -> applyBatidaRock(info);
            case PRESET_BATIDA_BASICA -> applyBatidaBasica(info);
            default -> applyBatidaBasica(info);
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

    /** Batida simples: para baixo em cada tempo, para cima no contratempo. */
    private void applyBatidaBasica(TimeSignatureInfo info) {
        int spb = info.stepsPerBeat();
        for (int beat = 0; beat < info.beatsPerMeasure(); beat++) {
            setAttack(beat * spb, AttackType.STRUM_DOWN);
            if (beat > 0) {
                setAttack(beat * spb + (spb / 2), AttackType.STRUM_UP);
            }
        }
    }

    /** Rock: colcheias alternando para baixo/para cima em todos os tempos. */
    private void applyBatidaRock(TimeSignatureInfo info) {
        int spb = info.stepsPerBeat();
        for (int beat = 0; beat < info.beatsPerMeasure(); beat++) {
            setAttack(beat * spb, AttackType.STRUM_DOWN);
            setAttack(beat * spb + (spb / 2), AttackType.STRUM_UP);
        }
    }

    /** Balada: mais espaçada, ataques nos tempos fortes. */
    private void applyBatidaBalada(TimeSignatureInfo info) {
        int spb = info.stepsPerBeat();
        for (int beat = 0; beat < info.beatsPerMeasure(); beat++) {
            setAttack(beat * spb, AttackType.STRUM_DOWN);
            if (beat % 2 == 1) {
                setAttack(beat * spb + (spb / 2), AttackType.STRUM_UP);
            }
        }
    }

    /** Reggae: skank — contratempos para cima. */
    private void applyReggae(TimeSignatureInfo info) {
        int spb = info.stepsPerBeat();
        for (int beat = 0; beat < info.beatsPerMeasure(); beat++) {
            setAttack(beat * spb + (spb / 2), AttackType.STRUM_UP);
        }
    }

    /** Dedilhado: uma nota por ataque, em sequência. */
    private void applyDedilhado(TimeSignatureInfo info) {
        int spb = info.stepsPerBeat();
        int stride = Math.max(1, spb / 2);
        for (int step = 0; step < steps.length; step += stride) {
            setAttack(step, AttackType.PICK);
        }
    }

    /**
     * Blues: shuffle. Cada tempo bate para baixo e repica na subdivisão da
     * tercina — a "galope" que define o estilo.
     */
    private void applyBlues(TimeSignatureInfo info) {
        int spb = info.stepsPerBeat();
        for (int beat = 0; beat < info.beatsPerMeasure(); beat++) {
            int beatStep = beat * spb;
            setAttack(beatStep, AttackType.STRUM_DOWN);
            setAttack(beatStep + (spb / 3), AttackType.PICK);
            setAttack(beatStep + ((2 * spb) / 3), AttackType.STRUM_UP);
        }
    }

    /**
     * Jazz: comping de dedilhado. Notas soltas na cabeça do tempo e no
     * contratempo do tempo 2, com a síncope que caracteriza o comping — mais
     * espaçado que o dedilhado corrido.
     */
    private void applyJazz(TimeSignatureInfo info) {
        int spb = info.stepsPerBeat();
        int beats = info.beatsPerMeasure();
        int offBeat = spb / 2;

        for (int beat = 0; beat < beats; beat++) {
            setAttack(beat * spb, AttackType.PICK);
        }
        if (beats >= 2) {
            setAttack(offBeat, AttackType.PICK);
        }
    }

    /**
     * Bossa Nova: violão sincopado. Polegar nos tempos, dedos nos contratempos —
     * por isso PICK na cabeça de cada tempo e STRUM_UP nas síncopes.
     */
    private void applyBossaNova(TimeSignatureInfo info) {
        int spb = info.stepsPerBeat();
        int beats = info.beatsPerMeasure();
        int offBeat = spb / 2;

        for (int beat = 0; beat < beats; beat++) {
            setAttack(beat * spb, AttackType.PICK);
        }
        if (beats >= 2) {
            setAttack(offBeat, AttackType.STRUM_UP);
        }
        if (beats >= 3) {
            setAttack((2 * spb) + offBeat, AttackType.STRUM_UP);
        }
    }
}
