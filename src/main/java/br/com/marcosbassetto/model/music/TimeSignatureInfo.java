package br.com.marcosbassetto.model.music;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Track;

public record TimeSignatureInfo(
        int numerator,
        int denominator,
        int beatsPerMeasure,
        int stepsPerBeat,
        int totalSteps,
        boolean isCompound
) {

    public static TimeSignatureInfo parse(String measure) {
        if (measure == null || !measure.trim().matches("^[1-9][0-9]*/[1-9][0-9]*$")) {
            throw new IllegalArgumentException("Formato inválido. Use N/D.");
        }

        String[] parts = measure.trim().split("/");
        int num = Integer.parseInt(parts[0]);
        int den = Integer.parseInt(parts[1]);

        if (num <= 0 || num > 32) {
            throw new IllegalArgumentException("Numerador inválido: " + num + ". Deve estar entre 1 e 32.");
        }

        if ((den & (den - 1)) != 0 || den > 32) {
            throw new IllegalArgumentException("O denominador deve ser uma potência de 2 (2, 4, 8, 16, 32).");
        }

        boolean compound = (den == 8 && num % 3 == 0 && num >= 6);

        int beats;
        int stepsPerBeat;

        if (compound) {
            beats = num / 3;            // Pulsos de semínima pontuada
            stepsPerBeat = 6;           // 6 semicolcheias por pulso
        } else if (den == 8) {
            beats = num / 2;            // Pulsos equivalentes
            stepsPerBeat = 4;           // 4 semicolcheias por pulso
        } else if (den == 2) {
            beats = num;
            stepsPerBeat = 8;
        } else { // den == 4 ou outros
            beats = num;
            stepsPerBeat = 4;           // 4 semicolcheias por semínima
        }

        int totalSteps = compound ? (num * 2) : (den == 8 ? num * 2 : beats * stepsPerBeat);

        return new TimeSignatureInfo(num, den, beats, stepsPerBeat, totalSteps, compound);
    }

    public double getStepDurationInQuarterNotes() {
        double totalMeasureInQuarters = (double) numerator * (4.0 / denominator);
        return totalMeasureInQuarters / totalSteps;
    }

    public long getMeasureTicks(int ppq) {
        return (long) numerator * ppq * 4 / denominator;
    }

    public long getStepTicks(int ppq) {
        return getMeasureTicks(ppq) / totalSteps;
    }

    public String getSignature() {
        return numerator + "/" + denominator;
    }

    public void writeTimeSignatureEvent(Track track) {
        try {
            int denominatorPower = (int) (Math.log(denominator) / Math.log(2));

            byte[] data = new byte[] {
                    (byte) numerator,
                    (byte) denominatorPower,
                    24,
                    8
            };

            MetaMessage metaMessage = new MetaMessage(0x58, data, data.length);
            track.add(new MidiEvent(metaMessage, 0));
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }
    }

    public static void writeTimeSignatureEvent(Track track, TimeSignatureInfo timeInfo) {
        if (timeInfo != null) {
            timeInfo.writeTimeSignatureEvent(track);
        }
    }

    public int getNumerator() {
        return numerator;
    }

    public int getDenominator() {
        return denominator;
    }

    @Override
    public int numerator() {
        return numerator;
    }

    @Override
    public int denominator() {
        return denominator;
    }

    @Override
    public int beatsPerMeasure() {
        return beatsPerMeasure;
    }

    @Override
    public int stepsPerBeat() {
        return stepsPerBeat;
    }

    @Override
    public int totalSteps() {
        return totalSteps;
    }

    @Override
    public boolean isCompound() {
        return isCompound;
    }
}
