package br.com.marcosbassetto.model.music;

public class BackingTrack {

    private String progression;
    private String bpm;
    private String measure;
    private String durationMinutes;
    private String durationSeconds;

    public BackingTrack(
            String progression,
            String bpm,
            String measure,
            String durationMinutes,
            String durationSeconds) {

        this.progression = progression;
        this.bpm = bpm;
        this.measure = measure;
        this.durationMinutes = durationMinutes;
        this.durationSeconds = durationSeconds;
    }

    public String getProgression() {
        return progression;
    }

    public String getBpm() {
        return bpm;
    }

    public String getMeasure() {
        return measure;
    }

    public String getDurationMinutes() {
        return durationMinutes;
    }

    public String getDurationSeconds() {
        return durationSeconds;
    }
}
