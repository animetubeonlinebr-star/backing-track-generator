package br.com.marcosbassetto.model.bass;

import br.com.marcosbassetto.model.music.TimeSignatureInfo;
import br.com.marcosbassetto.model.performance.VelocityLevel;

/**
 * Configurações editáveis do baixo. Reunidas em um POJO para que a
 * {@code BassConfigWindow} possa alterá-las sem tocar no serviço.
 *
 * <p>A duração das notas não é editável: o baixo deriva o tamanho da nota do
 * compasso informado no formulário principal. A intensidade é escolhida por
 * nível (fraco/médio/alto) e humanizada na geração.
 */
public class BassConfig {

    private BassRhythmPattern pattern;
    private String preset = BassRhythmPattern.PRESET_FUNDAMENTAL_SIMPLES;
    private VelocityLevel velocityLevel = VelocityLevel.DEFAULT;
    private int programChange = 33;       // Electric Bass (finger)

    public BassConfig(TimeSignatureInfo timeInfo) {
        TimeSignatureInfo info = timeInfo != null ? timeInfo : TimeSignatureInfo.parse("4/4");
        this.pattern = new BassRhythmPattern(info.totalSteps(), info, preset);
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

    public VelocityLevel getVelocityLevel() {
        return velocityLevel;
    }

    public void setVelocityLevel(VelocityLevel velocityLevel) {
        if (velocityLevel != null) {
            this.velocityLevel = velocityLevel;
        }
    }

    public int getProgramChange() {
        return programChange;
    }

    public void setProgramChange(int programChange) {
        if (programChange >= 0 && programChange <= 127) {
            this.programChange = programChange;
        }
    }
}
