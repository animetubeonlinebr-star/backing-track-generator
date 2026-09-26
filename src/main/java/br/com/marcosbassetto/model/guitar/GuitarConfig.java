package br.com.marcosbassetto.model.guitar;

import br.com.marcosbassetto.model.music.Intensity;
import br.com.marcosbassetto.model.music.Register;
import br.com.marcosbassetto.model.music.StylePreset;
import br.com.marcosbassetto.model.music.TimeSignatureInfo;

/**
 * Ajustes da guitarra rítmica: qual padrão de ataques usar e os parâmetros de
 * execução (offset da palhetada, intensidade, timbre, registro). É a fonte única
 * dos valores editáveis pela UI, para que o serviço de MIDI não os tenha fixos.
 *
 * <p>A intensidade (baixa/média/forte) define a velocity média; o valor final
 * de cada nota recebe humanização no {@code GuitarMidiService}. A articulação
 * decide como os ataques do preset são executados, sem alterar o groove: a
 * batida converte tudo em palhetadas, o dedilhado converte tudo em notas
 * sequenciais e a mista preserva o preset.
 */
public class GuitarConfig {

    private static final int BASE_VELOCITY_DOWN = 80;
    private static final int BASE_VELOCITY_UP = 65;
    private static final int BASE_VELOCITY_PICK = 75;

    private GuitarRhythmPattern pattern = new GuitarRhythmPattern(16);
    private String preset = GuitarRhythmPattern.PRESET_BATIDA_BASICA;
    private String appliedPreset = null;
    private int appliedSteps = -1;
    private int strumOffsetTicks = 15;
    private Intensity intensity = Intensity.MEDIA;
    private Register register = Register.GUITAR;
    private GuitarArticulation articulation = GuitarArticulation.MISTA;
    private int programChange = 25; // Acoustic Guitar (nylon)

    /**
     * Aplica a configuração padrão de um estilo: padrão rítmico, intensidade,
     * timbre e região de alturas — esta última escolhida para não disputar
     * espaço com o baixo.
     */
    public void applyStyle(StylePreset style, TimeSignatureInfo timeInfo) {
        if (style == null) {
            return;
        }
        this.preset = style.getGuitarPattern();
        this.intensity = style.getIntensity();
        this.register = style.getGuitarRegister();
        this.programChange = style.getGuitarProgram();
        reapplyPreset(timeInfo);
    }

    public Register getRegister() {
        return register;
    }

    public void setRegister(Register register) {
        if (register != null) {
            this.register = register;
        }
    }

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

    public Intensity getIntensity() {
        return intensity;
    }

    public void setIntensity(Intensity intensity) {
        if (intensity != null) {
            this.intensity = intensity;
        }
    }

    public GuitarArticulation getArticulation() {
        return articulation;
    }

    public void setArticulation(GuitarArticulation articulation) {
        if (articulation != null) {
            this.articulation = articulation;
        }
    }

    /** Velocity média da batida para baixo, já ajustada pela intensidade. */
    public int getVelocityDown() {
        return intensity.scale(BASE_VELOCITY_DOWN);
    }

    /** Velocity média da batida para cima, já ajustada pela intensidade. */
    public int getVelocityUp() {
        return intensity.scale(BASE_VELOCITY_UP);
    }

    /** Velocity média do dedilhado, já ajustada pela intensidade. */
    public int getVelocityPick() {
        return intensity.scale(BASE_VELOCITY_PICK);
    }

    public int getProgramChange() {
        return programChange;
    }

    public void setProgramChange(int programChange) {
        this.programChange = Math.max(0, Math.min(127, programChange));
    }
}
