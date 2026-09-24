package br.com.marcosbassetto.model.guitar;

import br.com.marcosbassetto.model.music.TimeSignatureInfo;

/**
 * Ajustes da guitarra rítmica: qual padrão de ataques usar e os parâmetros de
 * execução (offset da palhetada, velocities, timbre). É a fonte única dos
 * valores editáveis pela UI, para que o serviço de MIDI não os tenha fixos.
 */
public class GuitarConfig {

    private GuitarRhythmPattern pattern = new GuitarRhythmPattern(16);
    private String preset = GuitarRhythmPattern.PRESET_BATIDA_BASICA;
    private String appliedPreset = null;
    private int appliedSteps = -1;
    private int strumOffsetTicks = 15;
    private int velocityDown = 80;
    private int velocityUp = 65;
    private int velocityPick = 75;
    private int programChange = 25; // Acoustic Guitar (nylon)

    public GuitarRhythmPattern getPattern() {
        return pattern;
    }

    public void setPattern(GuitarRhythmPattern pattern) {
        if (pattern != null) {
            this.pattern = pattern;
        }
    }

    public String getPreset() {
        return preset;
    }

    public void setPreset(String preset) {
        if (preset != null) {
            this.preset = preset;
        }
    }

    /**
     * Garante que o padrão corresponde ao compasso e ao preset atuais,
     * reaplicando o preset apenas quando algo mudou. Rastrear o que foi
     * aplicado (e não só o tamanho) evita pular a aplicação quando o tamanho
     * coincide, ex.: padrão inicial de 16 passos e compasso 4/4.
     */
    public void syncPatternTo(TimeSignatureInfo timeInfo) {
        if (timeInfo == null) {
            return;
        }
        if (pattern.getTotalSteps() != timeInfo.totalSteps()) {
            pattern.resizeSteps(timeInfo.totalSteps());
        }
        if (appliedSteps != timeInfo.totalSteps() || !preset.equals(appliedPreset)) {
            pattern.applyPreset(preset, timeInfo);
            appliedPreset = preset;
            appliedSteps = timeInfo.totalSteps();
        }
    }

    /** Reaplica o preset ao grid atual, descartando edições manuais. */
    public void reapplyPreset(TimeSignatureInfo timeInfo) {
        appliedPreset = null;
        appliedSteps = -1;
        syncPatternTo(timeInfo);
    }

    public int getStrumOffsetTicks() {
        return strumOffsetTicks;
    }

    public void setStrumOffsetTicks(int strumOffsetTicks) {
        this.strumOffsetTicks = Math.max(0, strumOffsetTicks);
    }

    public int getVelocityDown() {
        return velocityDown;
    }

    public void setVelocityDown(int velocityDown) {
        this.velocityDown = clampVelocity(velocityDown);
    }

    public int getVelocityUp() {
        return velocityUp;
    }

    public void setVelocityUp(int velocityUp) {
        this.velocityUp = clampVelocity(velocityUp);
    }

    public int getVelocityPick() {
        return velocityPick;
    }

    public void setVelocityPick(int velocityPick) {
        this.velocityPick = clampVelocity(velocityPick);
    }

    public int getProgramChange() {
        return programChange;
    }

    public void setProgramChange(int programChange) {
        this.programChange = Math.max(0, Math.min(127, programChange));
    }

    private static int clampVelocity(int velocity) {
        return Math.max(0, Math.min(127, velocity));
    }
}
