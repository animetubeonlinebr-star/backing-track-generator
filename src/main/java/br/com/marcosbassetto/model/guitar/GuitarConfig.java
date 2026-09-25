package br.com.marcosbassetto.model.guitar;

import br.com.marcosbassetto.model.music.TimeSignatureInfo;
import br.com.marcosbassetto.model.performance.VelocityLevel;

/**
 * Ajustes da guitarra rítmica: qual padrão de ataques usar, a articulação
 * (batida, dedilhado ou os dois) e os parâmetros de execução (offset da
 * palhetada, intensidades, timbre). É a fonte única dos valores editáveis pela
 * UI, para que o serviço de MIDI não os tenha fixos.
 *
 * <p>As intensidades são escolhidas por nível (fraco/médio/alto) e humanizadas
 * na geração. O serviço só consulta a intensidade correspondente à articulação
 * encontrada: uma batida ignora a intensidade do dedilhado e vice-versa.
 */
public class GuitarConfig {

    private GuitarRhythmPattern pattern = new GuitarRhythmPattern(16);
    private String preset = GuitarRhythmPattern.PRESET_BATIDA_BASICA;
    private String appliedPreset = null;
    private int appliedSteps = -1;
    private int strumOffsetTicks = 15;
    private GuitarArticulation articulation = GuitarArticulation.BATIDA;
    private VelocityLevel velocityDownLevel = VelocityLevel.MEDIO;
    private VelocityLevel velocityUpLevel = VelocityLevel.FRACO;
    private VelocityLevel velocityPickLevel = VelocityLevel.MEDIO;
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

    public GuitarArticulation getArticulation() {
        return articulation;
    }

    public void setArticulation(GuitarArticulation articulation) {
        if (articulation != null) {
            this.articulation = articulation;
        }
    }

    public VelocityLevel getVelocityDownLevel() {
        return velocityDownLevel;
    }

    public void setVelocityDownLevel(VelocityLevel level) {
        if (level != null) {
            this.velocityDownLevel = level;
        }
    }

    public VelocityLevel getVelocityUpLevel() {
        return velocityUpLevel;
    }

    public void setVelocityUpLevel(VelocityLevel level) {
        if (level != null) {
            this.velocityUpLevel = level;
        }
    }

    public VelocityLevel getVelocityPickLevel() {
        return velocityPickLevel;
    }

    public void setVelocityPickLevel(VelocityLevel level) {
        if (level != null) {
            this.velocityPickLevel = level;
        }
    }

    public int getProgramChange() {
        return programChange;
    }

    public void setProgramChange(int programChange) {
        this.programChange = Math.max(0, Math.min(127, programChange));
    }
}
