package br.com.marcosbassetto.presentation;

import br.com.marcosbassetto.model.guitar.GuitarArticulation;
import br.com.marcosbassetto.model.guitar.GuitarConfig;
import br.com.marcosbassetto.model.guitar.GuitarRhythmPattern;
import br.com.marcosbassetto.model.music.Intensity;
import br.com.marcosbassetto.model.music.TimeSignatureInfo;

import javax.swing.*;
import java.awt.*;

/**
 * Janela de configuração da guitarra rítmica: escolha do preset de ataques,
 * articulação (batida, dedilhado ou os dois) e ajuste fino dos parâmetros de
 * execução (offset da palhetada, intensidade e timbre). As alterações só são
 * aplicadas ao confirmar.
 */
public class GuitarConfigWindow extends JDialog {

    private static final String[] PRESETS = {
            GuitarRhythmPattern.PRESET_BATIDA_BASICA,
            GuitarRhythmPattern.PRESET_BATIDA_BALADA,
            GuitarRhythmPattern.PRESET_BATIDA_ROCK,
            GuitarRhythmPattern.PRESET_DEDILHADO,
            GuitarRhythmPattern.PRESET_REGGAE
    };

    private final GuitarConfig config;
    private final TimeSignatureInfo timeInfo;
    private final Runnable onSaveCallback;

    private final JComboBox<String> cmbPreset = new JComboBox<>(PRESETS);
    private final JComboBox<GuitarArticulation> cmbArticulation =
            new JComboBox<>(GuitarArticulation.values());
    private final JSpinner spnStrumOffset = new JSpinner(new SpinnerNumberModel(15, 0, 60, 1));
    private final JComboBox<Intensity> cmbIntensity = new JComboBox<>(Intensity.values());
    private final JSpinner spnProgram = new JSpinner(new SpinnerNumberModel(25, 0, 127, 1));

    public GuitarConfigWindow(Window owner, GuitarConfig config, TimeSignatureInfo timeInfo,
                              Runnable onSaveCallback) {
        super(owner, "Configuração da Guitarra Rítmica", ModalityType.APPLICATION_MODAL);
        this.config = config;
        this.timeInfo = timeInfo;
        this.onSaveCallback = onSaveCallback;

        setLayout(new BorderLayout(10, 10));
        setSize(420, 320);
        setResizable(false);
        setLocationRelativeTo(owner);

        JPanel fields = new JPanel(new GridLayout(0, 2, 10, 8));
        fields.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        fields.add(new JLabel("Padrão rítmico"));
        fields.add(cmbPreset);
        fields.add(new JLabel("Articulação"));
        fields.add(cmbArticulation);
        fields.add(new JLabel("Offset da palhetada (ticks)"));
        fields.add(spnStrumOffset);
        fields.add(new JLabel("Intensidade"));
        fields.add(cmbIntensity);
        fields.add(new JLabel("Program Change (timbre)"));
        fields.add(spnProgram);

        JButton btnSave = new JButton("Salvar");
        btnSave.addActionListener(_ -> saveAndClose());

        JButton btnCancel = new JButton("Cancelar");
        btnCancel.addActionListener(_ -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(btnCancel);
        buttons.add(btnSave);

        add(fields, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        loadFromConfig();
    }

    private void loadFromConfig() {
        cmbPreset.setSelectedItem(config.getPreset());
        cmbArticulation.setSelectedItem(config.getArticulation());
        spnStrumOffset.setValue(config.getStrumOffsetTicks());
        cmbIntensity.setSelectedItem(config.getIntensity());
        spnProgram.setValue(config.getProgramChange());
    }

    private void saveAndClose() {
        String preset = (String) cmbPreset.getSelectedItem();
        config.setPreset(preset);
        config.setArticulation((GuitarArticulation) cmbArticulation.getSelectedItem());
        config.setStrumOffsetTicks((int) spnStrumOffset.getValue());
        config.setIntensity((Intensity) cmbIntensity.getSelectedItem());
        config.setProgramChange((int) spnProgram.getValue());

        if (timeInfo != null) {
            config.reapplyPreset(timeInfo);
        }

        if (onSaveCallback != null) {
            onSaveCallback.run();
        }
        dispose();
    }
}