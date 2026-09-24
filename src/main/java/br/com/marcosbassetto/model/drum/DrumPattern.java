package br.com.marcosbassetto.model.drum;

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
}
