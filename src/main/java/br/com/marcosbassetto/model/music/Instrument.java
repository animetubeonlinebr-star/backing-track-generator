package br.com.marcosbassetto.model.music;

/**
 * Instrumentos que podem ser exportados individualmente. Cada um carrega o nome
 * do arquivo MIDI gerado, conforme solicitado pela aplicação.
 */
public enum Instrument {

    GUITAR("Guitar.midi", "Guitarra"),
    BASS("Bass.midi", "Baixo"),
    KEYBOARD("keyboard.midi", "Teclado"),
    DRUMS("drums.midi", "Bateria");

    private final String fileName;
    private final String label;

    Instrument(String fileName, String label) {
        this.fileName = fileName;
        this.label = label;
    }

    public String getFileName() {
        return fileName;
    }

    public String getLabel() {
        return label;
    }
}
