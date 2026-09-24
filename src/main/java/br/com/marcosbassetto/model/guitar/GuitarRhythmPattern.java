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
    public static final String PRESET_REGGAE = "REGGAE";

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
            case PRESET_BATIDA_BASICA -> applyBatidaBasica(info);
            default -> applyBatidaBasica(info);
        }
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
}
