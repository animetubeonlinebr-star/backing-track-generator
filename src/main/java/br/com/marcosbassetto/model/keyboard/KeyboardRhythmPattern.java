package br.com.marcosbassetto.model.keyboard;

import br.com.marcosbassetto.model.music.TimeSignatureInfo;

import java.util.Arrays;

/**
 * Define QUANDO o teclado toca e COMO o acorde é atacado em cada passo.
 *
 * <p>O teclado é o irmão harmônico da guitarra: ambos tocam acordes, mas o
 * teclado escolhe entre bloco e arpejo, enquanto a guitarra escolhe entre
 * palhetada para baixo e para cima.
 */
public class KeyboardRhythmPattern {

    public enum AttackType {
        NONE,     // silêncio
        BLOCK,    // acorde em bloco (todas as notas no mesmo tick)
        ARP_UP,   // arpejo ascendente (grave → agudo)
        ARP_DOWN  // arpejo descendente (agudo → grave)
    }

    public static final String PRESET_PAD_SUSTENTADO = "PAD_SUSTENTADO";
    public static final String PRESET_BLOCO_RITMICO = "BLOCO_RITMICO";
    public static final String PRESET_ARPEJO_UP = "ARPEJO_UP";
    public static final String PRESET_ARPEJO_DOWN = "ARPEJO_DOWN";

    public static final String[] ALL_PRESETS = {
            PRESET_PAD_SUSTENTADO,
            PRESET_BLOCO_RITMICO,
            PRESET_ARPEJO_UP,
            PRESET_ARPEJO_DOWN
    };

    private AttackType[] steps;
    private String appliedPreset = PRESET_PAD_SUSTENTADO;

    public KeyboardRhythmPattern(int totalSteps) {
        this.steps = new AttackType[Math.max(1, totalSteps)];
        fillWithNone();
    }

    public KeyboardRhythmPattern(int totalSteps, TimeSignatureInfo info, String preset) {
        this(totalSteps);
        applyPreset(preset, info);
    }

    public AttackType getAttack(int step) {
        if (step < 0 || step >= steps.length) {
            return AttackType.NONE;
        }
        return steps[step];
    }

    public void setAttack(int step, AttackType type) {
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
        Arrays.fill(steps, AttackType.NONE);
    }

    /**
     * Redimensiona a grade. Se {@code reapplyPreset} for true, reaplica o preset
     * atual (o tamanho inicial de 16 coincide com 4/4, então confiar só no
     * tamanho pularia a aplicação). Se false, preserva edições manuais.
     */
    public void resizeSteps(int newTotal, TimeSignatureInfo info, boolean reapplyPreset) {
        if (newTotal <= 0) {
            return;
        }

        if (reapplyPreset && info != null) {
            this.steps = new AttackType[newTotal];
            fillWithNone();
            applyPreset(appliedPreset, info);
            return;
        }

        AttackType[] resized = new AttackType[newTotal];
        Arrays.fill(resized, AttackType.NONE);
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
            case PRESET_BLOCO_RITMICO -> {
                appliedPreset = PRESET_BLOCO_RITMICO;
                applyBlocoRitmico(info);
            }
            case PRESET_ARPEJO_UP -> {
                appliedPreset = PRESET_ARPEJO_UP;
                applyArpejoUp(info);
            }
            case PRESET_ARPEJO_DOWN -> {
                appliedPreset = PRESET_ARPEJO_DOWN;
                applyArpejoDown(info);
            }
            default -> {
                appliedPreset = PRESET_PAD_SUSTENTADO;
                applyPadSustentado(info);
            }
        }
    }

    /** BLOCK no tempo 1. O serviço segura o acorde (com sustain) até o fim do compasso. */
    private void applyPadSustentado(TimeSignatureInfo info) {
        if (info == null) {
            return;
        }
        setAttack(0, AttackType.BLOCK);
    }

    /** BLOCK nos tempos 1 e 3, mais o contratempo do 3. */
    private void applyBlocoRitmico(TimeSignatureInfo info) {
        if (info == null) {
            return;
        }
        int beats = info.beatsPerMeasure();
        int spb = info.stepsPerBeat();

        setAttack(0, AttackType.BLOCK);
        if (beats >= 3) {
            setAttack(2 * spb, AttackType.BLOCK);
            setAttack(2 * spb + (spb / 2), AttackType.BLOCK);
        } else if (beats == 2) {
            setAttack(spb, AttackType.BLOCK);
        }
    }

    /** ARP_UP em cada tempo (não em cada passo, para soar espaçado). */
    private void applyArpejoUp(TimeSignatureInfo info) {
        if (info == null) {
            return;
        }
        int spb = info.stepsPerBeat();
        for (int beat = 0; beat < info.beatsPerMeasure(); beat++) {
            setAttack(beat * spb, AttackType.ARP_UP);
        }
    }

    /** ARP_DOWN em cada tempo. */
    private void applyArpejoDown(TimeSignatureInfo info) {
        if (info == null) {
            return;
        }
        int spb = info.stepsPerBeat();
        for (int beat = 0; beat < info.beatsPerMeasure(); beat++) {
            setAttack(beat * spb, AttackType.ARP_DOWN);
        }
    }
}
