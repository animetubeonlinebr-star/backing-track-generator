package br.com.marcosbassetto.presentation;

import br.com.marcosbassetto.model.bass.BassConfig;
import br.com.marcosbassetto.model.bass.BassRhythmPattern;
import br.com.marcosbassetto.model.music.Intensity;
import br.com.marcosbassetto.model.music.TimeSignatureInfo;

import javax.swing.*;
import java.awt.*;

/**
 * Janela de configuração do baixo: preset rítmico, intensidade (baixa/média/
 * forte), duração das notas (legato vs staccato) e program change (timbre).
 */
public class BassConfigWindow extends JDialog {

    private final BassConfig config;
    private final TimeSignatureInfo timeInfo;

    private final JComboBox<String> comboPreset;
    private final JComboBox<Intensity> comboIntensity;
    private final JSlider sliderDuration;
    private final JSpinner spinnerProgram;

    private boolean saved = false;

    public BassConfigWindow(Window owner, BassConfig config, TimeSignatureInfo timeInfo) {
        super(owner, "Configuração de Baixo", ModalityType.APPLICATION_MODAL);
        this.config = config;
        this.timeInfo = timeInfo;

        comboPreset = new JComboBox<>(BassRhythmPattern.ALL_PRESETS);
        comboPreset.setSelectedItem(config.getPattern().getAppliedPreset());

        comboIntensity = new JComboBox<>(Intensity.values());
        comboIntensity.setSelectedItem(config.getIntensity());

        sliderDuration = new JSlider(BassConfig.MIN_DURATION_PERCENT,
                BassConfig.MAX_DURATION_PERCENT, config.getNoteDurationPercent());
        sliderDuration.setMajorTickSpacing(20);
        sliderDuration.setMinorTickSpacing(5);
        sliderDuration.setPaintTicks(true);
        sliderDuration.setPaintLabels(true);

        spinnerProgram = new JSpinner(
                new SpinnerNumberModel(config.getProgramChange(), 0, 127, 1));

        setLayout(new BorderLayout(10, 10));
        add(buildFieldsPanel(), BorderLayout.CENTER);
        add(buildButtonsPanel(), BorderLayout.SOUTH);

        pack();
        setMinimumSize(new Dimension(460, 300));
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
        panel.add(new JLabel("Intensidade:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(comboIntensity, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        panel.add(new JLabel("Duração (% do passo):"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(sliderDuration, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        panel.add(new JLabel("Program Change (0–127):"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(spinnerProgram, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        JLabel hint = new JLabel("<html><i>33 = Finger, 34 = Pick, 35 = Fretless, "
                + "36 = Slap, 37 = Slap 2</i></html>");
        hint.setForeground(Color.GRAY);
        panel.add(hint, gbc);

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

    private void saveAndClose() {
        config.setIntensity((Intensity) comboIntensity.getSelectedItem());
        config.setNoteDurationPercent(sliderDuration.getValue());
        config.setProgramChange((int) spinnerProgram.getValue());

        String preset = (String) comboPreset.getSelectedItem();
        if (preset != null) {
            config.applyPreset(preset, timeInfo);
        }

        saved = true;
        dispose();
    }
}
