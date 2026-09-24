package br.com.marcosbassetto.model.drum;

@SuppressWarnings("SpellCheckingInspection")
public enum DrumInstrument {
    KICK("Bumbo", 36),
    SNARE("Caixa", 38),
    HI_HAT_CLOSED("Chimbal Fechado", 42),
    HI_HAT_OPEN("Chimbal Aberto", 46),
    TOM_HIGH("Tom 1", 50),
    TOM_MID("Tom 2", 47),
    TOM_LOW("Surdo", 43),
    CRASH("Prato de Ataque", 49),
    RIDE("Prato de Condução", 51);

    private final String name;
    private final int defaultMidiNote;

    DrumInstrument(String name, int defaultMidiNote) {
        this.name = name;
        this.defaultMidiNote = defaultMidiNote;
    }

    public String getName() {
        return name;
    }

    public int getDefaultMidiNote() {
        return defaultMidiNote;
    }

    public String getLabelWithMidi() {
        return String.format("%s (%d)", name, defaultMidiNote);
    }
}
