package br.com.marcosbassetto.model.drum;

import br.com.marcosbassetto.model.music.StylePreset;
import br.com.marcosbassetto.model.music.TimeSignatureInfo;

import java.util.EnumMap;
import java.util.Map;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class DrumPattern {

    private int totalSteps;
    private Map<DrumInstrument, boolean[]> grid;

    public DrumPattern(int totalSteps) {
        this.totalSteps = totalSteps;
        this.grid = new EnumMap<>(DrumInstrument.class);
        initializeGrid(totalSteps);
    }

    public DrumPattern(TimeSignatureInfo timeInfo) {
        this(timeInfo.totalSteps());
        applyDefaultPattern(timeInfo);
    }

    private void initializeGrid(int steps) {
        for (DrumInstrument instrument : DrumInstrument.values()) {
            grid.put(instrument, new boolean[steps]);
        }
    }

    public int getTotalSteps() {
        return totalSteps;
    }

    public boolean[] getSteps(DrumInstrument instrument) {
        boolean[] stepsArray = grid.get(instrument);
        if (stepsArray == null) {
            return new boolean[totalSteps];
        }
        return stepsArray;
    }

    public boolean getStep(DrumInstrument instrument, int step) {
        if (step < 0 || step >= totalSteps) return false;
        boolean[] stepsArray = grid.get(instrument);
        return stepsArray != null && stepsArray[step];
    }

    public void setStep(DrumInstrument instrument, int step, boolean value) {
        if (step >= 0 && step < totalSteps) {
            boolean[] stepsArray = grid.get(instrument);
            if (stepsArray != null) {
                stepsArray[step] = value;
            }
        }
    }

    public void clear() {
        for (boolean[] stepsArray : grid.values()) {
            java.util.Arrays.fill(stepsArray, false);
        }
    }

    public void resizeSteps(int newTotalSteps, boolean resetToDefault, TimeSignatureInfo timeInfo) {
        if (newTotalSteps <= 0) return;

        if (resetToDefault && timeInfo != null) {
            this.totalSteps = timeInfo.totalSteps();
            this.grid = new EnumMap<>(DrumInstrument.class);
            initializeGrid(this.totalSteps);
            applyDefaultPattern(timeInfo);
            return;
        }

        Map<DrumInstrument, boolean[]> newGrid = new EnumMap<>(DrumInstrument.class);
        for (DrumInstrument instrument : DrumInstrument.values()) {
            boolean[] oldArray = grid.get(instrument);
            boolean[] newArray = new boolean[newTotalSteps];

            if (oldArray != null) {
                System.arraycopy(oldArray, 0, newArray, 0, Math.min(oldArray.length, newTotalSteps));
            }
            newGrid.put(instrument, newArray);
        }

        this.totalSteps = newTotalSteps;
        this.grid = newGrid;
    }

    public void applyDefaultPattern(TimeSignatureInfo timeInfo) {
        clear();
        if (timeInfo == null) return;

        int totalSteps = timeInfo.totalSteps();
        int stepsPerBeat = timeInfo.stepsPerBeat();
        int beats = timeInfo.beatsPerMeasure();

        for (int step = 0; step < totalSteps; step++) {
            setStep(DrumInstrument.HI_HAT_CLOSED, step, true);
        }

        for (int beat = 0; beat < beats; beat += 2) {
            setStep(DrumInstrument.KICK, beat * stepsPerBeat, true);
        }

        for (int beat = 1; beat < beats; beat += 2) {
            setStep(DrumInstrument.SNARE, beat * stepsPerBeat, true);
        }
    }

    /**
     * Aplica a levada do estilo. Cada estilo tem um desenho próprio, escrito
     * RELATIVO ao compasso (nunca com posições fixas — isso estouraria a grade
     * em 2/4, 3/4, 5/4 etc.).
     */
    public void applyStylePattern(String style, TimeSignatureInfo timeInfo) {
        if (timeInfo == null) {
            return;
        }
        StylePreset stylePreset = StylePreset.fromLabel(style);
        clear();
        switch (stylePreset != null ? stylePreset : StylePreset.ROCK) {
            case BLUES -> applyBlues(timeInfo);
            case BOSSA_NOVA -> applyBossaNova(timeInfo);
            case JAZZ -> applyJazz(timeInfo);
            case REGGAE -> applyReggae(timeInfo);
            case ROCK -> applyRock(timeInfo);
        }
    }

    private void applyStylePattern(StylePreset style, TimeSignatureInfo info) {
        applyStylePattern(style.getLabel(), info);
    }

    /** Rock: chimbal em colcheias, bumbo nos tempos 1 e 3, caixa nos 2 e 4. */
    private void applyRock(TimeSignatureInfo info) {
        int spb = info.stepsPerBeat();
        int beats = info.beatsPerMeasure();
        for (int beat = 0; beat < beats; beat++) {
            int beatStep = beat * spb;
            setStep(DrumInstrument.HI_HAT_CLOSED, beatStep, true);
            setStep(DrumInstrument.HI_HAT_CLOSED, beatStep + (spb / 2), true);
            setStep(DrumInstrument.KICK, beatStep, true);
            setStep(DrumInstrument.SNARE, beatStep + (spb / 2), true);
        }
    }

    /** Blues: shuffle (tercina), condução no ride. */
    private void applyBlues(TimeSignatureInfo info) {
        int spb = info.stepsPerBeat();
        int beats = info.beatsPerMeasure();
        for (int beat = 0; beat < beats; beat++) {
            int beatStep = beat * spb;
            setStep(DrumInstrument.RIDE, beatStep, true);
            setStep(DrumInstrument.RIDE, beatStep + (spb / 3), true);
            setStep(DrumInstrument.RIDE, beatStep + ((2 * spb) / 3), true);
            setStep(DrumInstrument.KICK, beatStep, true);
            if (beat % 2 == 1) {
                setStep(DrumInstrument.SNARE, beatStep, true);
            }
        }
    }

    /** Bossa Nova: surdo no 1 e no 3 (padrão 2–2 do partido alto). */
    private void applyBossaNova(TimeSignatureInfo info) {
        int spb = info.stepsPerBeat();
        int beats = info.beatsPerMeasure();
        for (int beat = 0; beat < beats; beat++) {
            setStep(DrumInstrument.HI_HAT_CLOSED, beat * spb, true);
        }
        setStep(DrumInstrument.KICK, 0, true);
        setStep(DrumInstrument.KICK, 2 * spb, true);
    }

    /** Jazz: ride com padrão de swing, bumbo e caixa apenas como acentos. */
    private void applyJazz(TimeSignatureInfo info) {
        int spb = info.stepsPerBeat();
        int beats = info.beatsPerMeasure();
        for (int beat = 0; beat < beats; beat++) {
            int beatStep = beat * spb;
            setStep(DrumInstrument.RIDE, beatStep, true);
            setStep(DrumInstrument.RIDE, beatStep + (spb / 2), true);
            setStep(DrumInstrument.HI_HAT_OPEN, beatStep + (spb / 4), true);
            if (beat % 2 == 0) {
                setStep(DrumInstrument.KICK, beatStep, true);
            }
        }
    }

    /** Reggae: one drop — bumbo e caixa juntos no tempo 3, chimbal no contratempo. */
    private void applyReggae(TimeSignatureInfo info) {
        int spb = info.stepsPerBeat();
        int beats = info.beatsPerMeasure();
        for (int beat = 0; beat < beats; beat++) {
            setStep(DrumInstrument.HI_HAT_CLOSED, (beat * spb) + (spb / 2), true);
        }
        int drop = beats >= 3 ? 2 * spb : 0;
        setStep(DrumInstrument.KICK, drop, true);
        setStep(DrumInstrument.SNARE, drop, true);
    }
}
