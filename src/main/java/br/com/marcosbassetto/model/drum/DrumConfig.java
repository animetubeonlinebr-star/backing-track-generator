package br.com.marcosbassetto.model.drum;

import br.com.marcosbassetto.model.music.Intensity;
import br.com.marcosbassetto.model.music.StylePreset;
import br.com.marcosbassetto.model.music.TimeSignatureInfo;

import java.util.EnumMap;
import java.util.Map;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class DrumConfig {

    public static final int BASE_VELOCITY = 100;

    private TimeSignatureInfo timeSignatureInfo;
    private DrumPattern pattern;
    private Intensity intensity = Intensity.MEDIA;
    private final Map<DrumInstrument, Boolean> enabledInstruments = new EnumMap<>(DrumInstrument.class);
    private final Map<DrumInstrument, Integer> midiNotes = new EnumMap<>(DrumInstrument.class);

    public DrumConfig(String timeSignature) {
        this(TimeSignatureInfo.parse(timeSignature));
    }

    public DrumConfig(TimeSignatureInfo timeSignatureInfo) {
        this.timeSignatureInfo = timeSignatureInfo != null ? timeSignatureInfo : TimeSignatureInfo.parse("4/4");
        this.pattern = new DrumPattern(this.timeSignatureInfo.totalSteps());
        this.pattern.applyDefaultPattern(this.timeSignatureInfo);

        for (DrumInstrument instrument : DrumInstrument.values()) {
            enabledInstruments.put(instrument, true);
            midiNotes.put(instrument, instrument.getDefaultMidiNote());
        }
    }

    /**
     * Ajusta a levada ao estilo escolhido, de acordo com o compasso vigente.
     * O padrão é escrito relativo ao compasso, então qualquer métrica funciona.
     */
    public void applyStyle(StylePreset style, TimeSignatureInfo timeInfo) {
        if (style == null) {
            return;
        }
        this.intensity = style.getIntensity();
        TimeSignatureInfo info = timeInfo != null ? timeInfo : this.timeSignatureInfo;
        this.pattern.applyStylePattern(style.getLabel(), info);
    }


    public TimeSignatureInfo getTimeSignatureInfo() {
        return timeSignatureInfo;
    }

    public void setTimeSignatureInfo(TimeSignatureInfo timeSignatureInfo) {
        setTimeSignatureInfo(timeSignatureInfo, true);
    }

    public void setTimeSignatureInfo(TimeSignatureInfo timeSignatureInfo, boolean resetToDefault) {
        if (timeSignatureInfo == null) return;
        this.timeSignatureInfo = timeSignatureInfo;
        this.pattern.resizeSteps(timeSignatureInfo.totalSteps(), resetToDefault, timeSignatureInfo);
    }

    public void setTimeSignatureInfo(String timeSignature, boolean resetToDefault) {
        setTimeSignatureInfo(TimeSignatureInfo.parse(timeSignature), resetToDefault);
    }


    public int getTotalSteps() {
        return timeSignatureInfo.totalSteps();
    }

    public long getStepTicks(int ppq) {
        return timeSignatureInfo.getStepTicks(ppq);
    }

    public long getMeasureTicks(int ppq) {
        return timeSignatureInfo.getMeasureTicks(ppq);
    }


    public DrumPattern getPattern() {
        return pattern;
    }

    public boolean isHit(DrumInstrument instrument, int step) {
        if (!isInstrumentEnabled(instrument)) {
            return false;
        }
        return pattern.getStep(instrument, step);
    }

    public void setHit(DrumInstrument instrument, int step, boolean value) {
        pattern.setStep(instrument, step, value);
    }


    public boolean isInstrumentEnabled(DrumInstrument instrument) {
        return enabledInstruments.getOrDefault(instrument, true);
    }

    public void setInstrumentEnabled(DrumInstrument instrument, boolean enabled) {
        enabledInstruments.put(instrument, enabled);
    }

    public int getMidiNote(DrumInstrument instrument) {
        return midiNotes.getOrDefault(instrument, instrument.getDefaultMidiNote());
    }

    public void setMidiNote(DrumInstrument instrument, int note) {
        midiNotes.put(instrument, note);
    }


    public Intensity getIntensity() {
        return intensity;
    }

    public void setIntensity(Intensity intensity) {
        if (intensity != null) {
            this.intensity = intensity;
        }
    }

    /** Velocity média da peça de bateria, já ajustada pela intensidade. */
    public int getVelocity() {
        return intensity.scale(BASE_VELOCITY);
    }


    public void copyFrom(DrumConfig source) {
        if (source == null) return;

        this.timeSignatureInfo = source.timeSignatureInfo;
        this.intensity = source.intensity;

        this.enabledInstruments.clear();
        this.enabledInstruments.putAll(source.enabledInstruments);

        this.midiNotes.clear();
        this.midiNotes.putAll(source.midiNotes);

        if (source.pattern != null) {
            this.pattern = new DrumPattern(source.pattern.getTotalSteps());
            for (DrumInstrument inst : DrumInstrument.values()) {
                boolean[] sourceSteps = source.pattern.getSteps(inst);
                for (int i = 0; i < sourceSteps.length; i++) {
                    this.pattern.setStep(inst, i, sourceSteps[i]);
                }
            }
        }
    }
}
