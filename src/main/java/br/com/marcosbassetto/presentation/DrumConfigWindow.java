package br.com.marcosbassetto.presentation;

import br.com.marcosbassetto.model.drum.DrumConfig;
import br.com.marcosbassetto.model.music.Intensity;
import br.com.marcosbassetto.model.music.TimeSignatureInfo;
import br.com.marcosbassetto.presentation.components.SequencerPanel;

import javax.swing.*;
import java.awt.*;

public class DrumConfigWindow extends JDialog {

    private final DrumConfig drumConfig;
    private final SequencerPanel sequencerPanel;

    public DrumConfigWindow(Window owner, DrumConfig drumConfig) {
        super(owner, "Configuração de Bateria e Ritmo", ModalityType.APPLICATION_MODAL);
        this.drumConfig = drumConfig;

        setLayout(new BorderLayout(10, 10));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));

        JButton btnMidiConfig = new JButton("Mapeamento MIDI...");
        btnMidiConfig.addActionListener(_ -> openMidiConfigWindow());
        topPanel.add(btnMidiConfig);

        topPanel.add(new JLabel("Intensidade:"));
        JComboBox<Intensity> cmbIntensity = new JComboBox<>(Intensity.values());
        cmbIntensity.setSelectedItem(drumConfig.getIntensity());
        cmbIntensity.addActionListener(_ ->
                drumConfig.setIntensity((Intensity) cmbIntensity.getSelectedItem()));
        topPanel.add(cmbIntensity);

        sequencerPanel = new SequencerPanel(drumConfig);
        JScrollPane scrollPane = new JScrollPane(sequencerPanel);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        JButton btnResetPattern = new JButton("Restaurar Padrão");
        btnResetPattern.addActionListener(_ -> resetPattern());

        JButton btnClear = new JButton("Limpar Grid");
        btnClear.addActionListener(_ -> clearPattern());

        JButton btnClose = new JButton("Concluir");
        btnClose.addActionListener(_ -> dispose());

        bottomPanel.add(btnResetPattern);
        bottomPanel.add(btnClear);
        bottomPanel.add(btnClose);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        pack();
        setMinimumSize(new Dimension(650, 420));
        setLocationRelativeTo(owner);
    }

    public DrumConfigWindow(Window owner, DrumConfig drumConfig, String currentMeasure) {
        this(owner, drumConfig);
        if (currentMeasure != null && !currentMeasure.isBlank()) {
            try {
                TimeSignatureInfo newInfo = TimeSignatureInfo.parse(currentMeasure);
                String currentSig = this.drumConfig.getTimeSignatureInfo().getSignature();
                String newSig = newInfo.getSignature();

                if (!currentSig.equals(newSig)) {
                    this.drumConfig.setTimeSignatureInfo(newInfo, true);
                    sequencerPanel.repaint();
                }
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(
                        this,
                        "Compasso inválido fornecido: " + currentMeasure + ".\n" + e.getMessage(),
                        "Erro de Compasso",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    private void openMidiConfigWindow() {
        DrumMidiConfigWindow midiWindow = new DrumMidiConfigWindow(this, drumConfig, () -> sequencerPanel.repaint());
        midiWindow.setVisible(true);
    }

    private void resetPattern() {
        drumConfig.getPattern().applyDefaultPattern(drumConfig.getTimeSignatureInfo());
        sequencerPanel.repaint();
    }

    private void clearPattern() {
        drumConfig.getPattern().clear();
        sequencerPanel.repaint();
    }
}
