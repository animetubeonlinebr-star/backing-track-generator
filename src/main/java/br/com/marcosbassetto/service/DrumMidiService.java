package br.com.marcosbassetto.service;

import br.com.marcosbassetto.model.drum.DrumConfig;
import br.com.marcosbassetto.model.drum.DrumInstrument;
import br.com.marcosbassetto.model.music.TimeSignatureInfo;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

public class DrumMidiService {

    private static final int CHANNEL_DRUMS = 9; // Canal 10 no MIDI (index 0-based)

    public void generateDrumTrack(Track drumTrack, DrumConfig drumConfig, int ppq, long totalTicks) throws InvalidMidiDataException {
        TimeSignatureInfo timeInfo = drumConfig.getTimeSignatureInfo();

        long stepTicks = timeInfo.getStepTicks(ppq);
        long ticksPerMeasure = timeInfo.getMeasureTicks(ppq);
        int totalStepsInMeasure = timeInfo.totalSteps();

        long currentMeasureTick = 0;
        while (currentMeasureTick < totalTicks) {
            for (int step = 0; step < totalStepsInMeasure; step++) {
                long currentStepTick = currentMeasureTick + (step * stepTicks);
                if (currentStepTick >= totalTicks) break;

                for (DrumInstrument inst : DrumInstrument.values()) {
                    if (drumConfig.isHit(inst, step)) {
                        int note = drumConfig.getMidiNote(inst);
                        int velocity = 100;

                        ShortMessage noteOn = new ShortMessage();
                        noteOn.setMessage(ShortMessage.NOTE_ON, CHANNEL_DRUMS, note, velocity);
                        drumTrack.add(new MidiEvent(noteOn, currentStepTick));

                        ShortMessage noteOff = new ShortMessage();
                        noteOff.setMessage(ShortMessage.NOTE_OFF, CHANNEL_DRUMS, note, 0);
                        drumTrack.add(new MidiEvent(noteOff, currentStepTick + (stepTicks / 2)));
                    }
                }
            }
            currentMeasureTick += ticksPerMeasure;
        }
    }
}
