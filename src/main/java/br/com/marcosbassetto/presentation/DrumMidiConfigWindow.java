package br.com.marcosbassetto.presentation;

import br.com.marcosbassetto.model.drum.DrumConfig;
import br.com.marcosbassetto.model.drum.DrumInstrument;

import javax.swing.*;
import java.awt.*;
import java.util.EnumMap;
import java.util.Map;

public class DrumMidiConfigWindow extends JDialog {

    private final DrumConfig drumConfig;
    private final Map<DrumInstrument, JSpinner> spinnerMap = new EnumMap<>(DrumInstrument.class);
    private final Runnable onSaveCallback;

    public DrumMidiConfigWindow(Window owner, DrumConfig drumConfig, Runnable onSaveCallback) {
        super(owner, "Mapeamento MIDI da Bateria", ModalityType.APPLICATION_MODAL);
        this.drumConfig = drumConfig;
        this.onSaveCallback = onSaveCallback;

        setLayout(new BorderLayout(10, 10));
        setSize(420, 480);
        setLocationRelativeTo(owner);
        setResizable(false);

        JPanel panelFields = new JPanel(new GridLayout(DrumInstrument.values().length, 3, 10, 8));
        panelFields.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        for (DrumInstrument inst : DrumInstrument.values()) {
            JLabel lblName = new JLabel(inst.getName());
            JLabel lblCurrent = new JLabel("Atual: " + drumConfig.getMidiNote(inst));

            JSpinner spnMidi = new JSpinner(new SpinnerNumberModel(drumConfig.getMidiNote(inst), 0, 127, 1));
            spinnerMap.put(inst, spnMidi);

            panelFields.add(lblName);
            panelFields.add(lblCurrent);
            panelFields.add(spnMidi);
        }

        JButton btnSave = new JButton("Salvar");
        btnSave.addActionListener(_ -> saveAndClose());

        JButton btnCancel = new JButton("Cancelar");
        btnCancel.addActionListener(_ -> dispose());

        JPanel panelButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelButtons.add(btnCancel);
        panelButtons.add(btnSave);

        add(panelFields, BorderLayout.CENTER);
        add(panelButtons, BorderLayout.SOUTH);
    }

    private void saveAndClose() {
        for (Map.Entry<DrumInstrument, JSpinner> entry : spinnerMap.entrySet()) {
            int newNote = (int) entry.getValue().getValue();
            drumConfig.setMidiNote(entry.getKey(), newNote);
        }

        if (onSaveCallback != null) {
            onSaveCallback.run();
        }
        dispose();
    }
}
