package br.com.marcosbassetto.presentation;

import br.com.marcosbassetto.model.keyboard.KeyboardConfig;
import br.com.marcosbassetto.model.keyboard.KeyboardRhythmPattern;
import br.com.marcosbassetto.model.music.Intensity;
import br.com.marcosbassetto.model.music.TimeSignatureInfo;

import javax.swing.*;
import java.awt.*;

/**
 * Janela de configuração do teclado: preset rítmico, timbre, intensidade,
 * duração das notas e pedal de sustain.
 */
public class KeyboardConfigWindow extends JDialog {

    private static final String[] PROGRAM_NAMES = {
            "0 - Acoustic Grand Piano",
            "1 - Bright Acoustic Piano",
            "4 - Electric Piano 1",
            "5 - Electric Piano 2",
            "6 - Harpsichord",
            "7 - Clavinet",
            "11 - Vibraphone",
            "12 - Marimba",
            "16 - Drawbar Organ",
            "17 - Percussive Organ",
            "19 - Church Organ",
            "21 - Accordion",
            "46 - Harp",
            "48 - String Ensemble 1",
            "49 - String Ensemble 2",
            "52 - Choir Aahs",
            "53 - Voice Oohs"
    };

    private final KeyboardConfig config;
    private final TimeSignatureInfo timeInfo;

    private final JComboBox<String> comboPreset;
    private final JComboBox<String> comboProgram;
    private final JComboBox<Intensity> comboIntensity;
    private final JSlider sliderDuration;
    private final JCheckBox chkSustain;

    private boolean saved = false;

    public KeyboardConfigWindow(Window owner, KeyboardConfig config, TimeSignatureInfo timeInfo) {
        super(owner, "Configuração de Teclado", ModalityType.APPLICATION_MODAL);
        this.config = config;
        this.timeInfo = timeInfo;

        comboPreset = new JComboBox<>(KeyboardRhythmPattern.ALL_PRESETS);
        comboPreset.setSelectedItem(config.getPattern().getAppliedPreset());

        comboProgram = new JComboBox<>(PROGRAM_NAMES);
        comboProgram.setSelectedIndex(findProgramIndex(config.getProgramChange()));

        comboIntensity = new JComboBox<>(Intensity.values());
        comboIntensity.setSelectedItem(config.getIntensity());

        sliderDuration = new JSlider(KeyboardConfig.MIN_DURATION_PERCENT,
                KeyboardConfig.MAX_DURATION_PERCENT, config.getNoteDurationPercent());
        sliderDuration.setMajorTickSpacing(10);
        sliderDuration.setMinorTickSpacing(5);
        sliderDuration.setPaintTicks(true);
        sliderDuration.setPaintLabels(true);

        chkSustain = new JCheckBox("Usar pedal de sustain (CC 64)", config.isSustainEnabled());

        setLayout(new BorderLayout(10, 10));
        add(buildFieldsPanel(), BorderLayout.CENTER);
        add(buildButtonsPanel(), BorderLayout.SOUTH);

        pack();
        setMinimumSize(new Dimension(480, 360));
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    /** Indica se o usuário confirmou as alterações. */
    public boolean isSaved() {
        return saved;
    }

    private JPanel buildFieldsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        panel.add(new JLabel("Padrão rítmico:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(comboPreset, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        panel.add(new JLabel("Timbre:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(comboProgram, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        panel.add(new JLabel("Intensidade:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(comboIntensity, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        panel.add(new JLabel("Duração (% do compasso):"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(sliderDuration, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        panel.add(chkSustain, gbc);

        return panel;
    }

    private JPanel buildButtonsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        JButton btnCancel = new JButton("Cancelar");
        btnCancel.addActionListener(_ -> dispose());

        JButton btnSave = new JButton("Salvar");
        btnSave.addActionListener(_ -> saveAndClose());

        panel.add(btnCancel);
        panel.add(btnSave);
        return panel;
    }

    private int findProgramIndex(int program) {
        for (int i = 0; i < PROGRAM_NAMES.length; i++) {
            if (PROGRAM_NAMES[i].startsWith(program + " ")) {
                return i;
            }
        }
        return 0;
    }

    private int extractProgram(String name) {
        if (name == null) {
            return config.getProgramChange();
        }
        try {
            return Integer.parseInt(name.split(" ")[0]);
        } catch (NumberFormatException e) {
            return config.getProgramChange();
        }
    }

    private void saveAndClose() {
        config.setIntensity((Intensity) comboIntensity.getSelectedItem());
        config.setNoteDurationPercent(sliderDuration.getValue());
        config.setSustainEnabled(chkSustain.isSelected());
        config.setProgramChange(extractProgram((String) comboProgram.getSelectedItem()));

        String preset = (String) comboPreset.getSelectedItem();
        if (preset != null) {
            config.applyPreset(preset, timeInfo);
        }

        saved = true;
        dispose();
    }
}
