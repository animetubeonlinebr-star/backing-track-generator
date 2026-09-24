package br.com.marcosbassetto.model.keyboard;

import br.com.marcosbassetto.model.music.TimeSignatureInfo;

/**
 * Configurações editáveis do teclado: padrão rítmico, velocity, timbre,
 * duração das notas e pedal de sustain.
 */
public class KeyboardConfig {

    public static final int MIN_VELOCITY = 40;
    public static final int MAX_VELOCITY = 110;
    public static final int MIN_DURATION_PERCENT = 50;
    public static final int MAX_DURATION_PERCENT = 100;

    private KeyboardRhythmPattern pattern;
    private String preset = KeyboardRhythmPattern.PRESET_PAD_SUSTENTADO;
    private int velocity = 70;
    private int programChange = 0;        // Acoustic Grand Piano
    private int noteDurationPercent = 95; // piano sustenta naturalmente
    private boolean sustainEnabled = true;

    public KeyboardConfig(TimeSignatureInfo timeInfo) {
        TimeSignatureInfo info = timeInfo != null ? timeInfo : TimeSignatureInfo.parse("4/4");
        this.pattern = new KeyboardRhythmPattern(info.totalSteps(), info, preset);
    }

    public KeyboardRhythmPattern getPattern() {
        return pattern;
    }

    public String getPreset() {
        return preset;
    }

    /**
     * Seleciona um novo preset rítmico. O preset é registrado antes da
     * sincronização para que uma mudança simultânea de compasso não reaplique
     * o padrão antigo.
     */
    public void applyPreset(String preset, TimeSignatureInfo timeInfo) {
        if (preset != null) {
            this.preset = preset;
        }
        syncToTimeSignature(timeInfo);
        if (timeInfo != null) {
            pattern.applyPreset(this.preset, timeInfo);
        }
    }

    /** Garante que o grid corresponde ao compasso atual, reaplicando o preset. */
    public void syncToTimeSignature(TimeSignatureInfo timeInfo) {
        if (timeInfo == null) {
            return;
        }
        int newTotal = timeInfo.totalSteps();
        if (pattern.getTotalSteps() != newTotal) {
            pattern.resizeSteps(newTotal, timeInfo, true);
        }
    }

    public int getVelocity() {
        return velocity;
    }

    public void setVelocity(int velocity) {
        this.velocity = clamp(velocity, MIN_VELOCITY, MAX_VELOCITY);
    }

    public int getProgramChange() {
        return programChange;
    }

    public void setProgramChange(int programChange) {
        if (programChange >= 0 && programChange <= 127) {
            this.programChange = programChange;
        }
    }

    public int getNoteDurationPercent() {
        return noteDurationPercent;
    }

    public void setNoteDurationPercent(int percent) {
        this.noteDurationPercent = clamp(percent, MIN_DURATION_PERCENT, MAX_DURATION_PERCENT);
    }

    public boolean isSustainEnabled() {
        return sustainEnabled;
    }

    public void setSustainEnabled(boolean sustainEnabled) {
        this.sustainEnabled = sustainEnabled;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
