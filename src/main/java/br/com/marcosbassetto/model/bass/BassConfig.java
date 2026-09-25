package br.com.marcosbassetto.model.bass;

import br.com.marcosbassetto.model.music.Intensity;
import br.com.marcosbassetto.model.music.Register;
import br.com.marcosbassetto.model.music.StylePreset;
import br.com.marcosbassetto.model.music.TimeSignatureInfo;

/**
 * Configurações editáveis do baixo. Reunidas em um POJO para que a
 * {@code BassConfigWindow} possa alterá-las sem tocar no serviço.
 *
 * <p>A intensidade define a velocity média; a humanização por nota é aplicada
 * no {@code BassMidiService}.
 */
public class BassConfig {

    public static final int BASE_VELOCITY = 90;
    public static final int MIN_DURATION_PERCENT = 40;
    public static final int MAX_DURATION_PERCENT = 100;

    private BassRhythmPattern pattern;
    private String preset = BassRhythmPattern.PRESET_FUNDAMENTAL_SIMPLES;
    private Intensity intensity = Intensity.MEDIA;
    private Register register = Register.BASS;
    private int programChange = 33;       // Electric Bass (finger)
    private int noteDurationPercent = 80; // 80% do passo = leve staccato

    public BassConfig(TimeSignatureInfo timeInfo) {
        TimeSignatureInfo info = timeInfo != null ? timeInfo : TimeSignatureInfo.parse("4/4");
        this.pattern = new BassRhythmPattern(info.totalSteps(), info, preset);
    }

    /**
     * Aplica a configuração padrão de um estilo: padrão rítmico, intensidade,
     * timbre e região de alturas.
     */
    public void applyStyle(StylePreset style, TimeSignatureInfo timeInfo) {
        if (style == null) {
            return;
        }
        this.intensity = style.getIntensity();
        this.register = style.getBassRegister();
        this.programChange = style.getBassProgram();
        applyPreset(style.getBassPattern(), timeInfo);
    }

    public Register getRegister() {
        return register;
    }

    public void setRegister(Register register) {
        if (register != null) {
            this.register = register;
        }
    }

    public BassRhythmPattern getPattern() {
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

    /**
     * Garante que o grid corresponde ao compasso atual, reaplicando o preset
     * quando o total de passos muda.
     */
    public void syncToTimeSignature(TimeSignatureInfo timeInfo) {
        if (timeInfo == null) {
            return;
        }
        int newTotal = timeInfo.totalSteps();
        if (pattern.getTotalSteps() != newTotal) {
            pattern.resizeSteps(newTotal, timeInfo, true);
        }
    }

    public Intensity getIntensity() {
        return intensity;
    }

    public void setIntensity(Intensity intensity) {
        if (intensity != null) {
            this.intensity = intensity;
        }
    }

    /** Velocity média da fundamental, já ajustada pela intensidade. */
    public int getVelocity() {
        return intensity.scale(BASE_VELOCITY);
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

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
