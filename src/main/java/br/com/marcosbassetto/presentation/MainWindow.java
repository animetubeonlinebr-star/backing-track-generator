package br.com.marcosbassetto.presentation;

import br.com.marcosbassetto.model.bass.BassConfig;
import br.com.marcosbassetto.model.drum.DrumConfig;
import br.com.marcosbassetto.model.guitar.GuitarConfig;
import br.com.marcosbassetto.model.keyboard.KeyboardConfig;
import br.com.marcosbassetto.model.music.BackingTrack;
import br.com.marcosbassetto.model.music.TimeSignatureInfo;
import br.com.marcosbassetto.service.MidiGenerationService;
import br.com.marcosbassetto.utils.TextFieldUtils;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

@SuppressWarnings("SpellCheckingInspection")
public class MainWindow {

    private final JFrame frmMainFrame;
    private final JTextField txtProgression;
    private final JTextField txtBpm;
    private final JTextField txtMeasure;
    private final JTextField txtDurationMin;
    private final JTextField txtDurationSeg;
    private final DrumConfig drumConfig;
    private final GuitarConfig guitarConfig;
    private final BassConfig bassConfig;
    private final KeyboardConfig keyboardConfig;

    private JCheckBox chkGuitar;
    private JCheckBox chkKeyboard;
    private JCheckBox chkDrums;
    private JCheckBox chkBass;

    public MainWindow() {
        drumConfig = new DrumConfig("4/4");
        guitarConfig = new GuitarConfig();

        TimeSignatureInfo initialInfo = TimeSignatureInfo.parse("4/4");
        bassConfig = new BassConfig(initialInfo);
        keyboardConfig = new KeyboardConfig(initialInfo);

        frmMainFrame = new JFrame("Gerador de BackingTrack");
        setupWindow();

        txtProgression = createTextField(200, 50, 500, 25);
        txtBpm = createNumericTextField(200, 100, 500, 25);
        txtMeasure = createTextField(200, 150, 500, 25);
        txtDurationMin = createNumericTextField(200, 200, 200, 25);
        txtDurationSeg = createNumericTextField(500, 200, 200, 25);

        setupMenuBar();
        setupLabelsAndFields();
        setupCheckBoxes();

        JButton btnGeneration = createGenerateButton();
        frmMainFrame.add(btnGeneration);

        frmMainFrame.setVisible(true);
    }

    private void setupWindow() {
        URL imgUrl = getClass().getResource("/img/backing-track.jpg");
        if (imgUrl != null) {
            frmMainFrame.setIconImage(new ImageIcon(imgUrl).getImage());
        } else {
            System.err.println("Imagem não encontrada em /img/backing-track.jpg");
        }
        frmMainFrame.setSize(800, 600);
        frmMainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frmMainFrame.setResizable(false);
        frmMainFrame.setLocationRelativeTo(null);
        frmMainFrame.setLayout(null);
    }

    private void setupMenuBar() {
        JMenuBar mnbMenuBar = new JMenuBar();
        mnbMenuBar.setBounds(0, 0, 800, 30);

        JMenu mnuInstrument = new JMenu("INSTRUMENT");

        JMenuItem mitDrum = new JMenuItem("Drum");
        mitDrum.addActionListener(_ -> {
            String measureText = txtMeasure.getText();

            // Validação de campo em branco
            if (measureText == null || measureText.isBlank()) {
                JOptionPane.showMessageDialog(frmMainFrame, "É necessário informar o Compasso!");
                return;
            }

            // Validação rigorosa do compasso via TimeSignatureInfo
            if (!isMeasure(measureText)) {
                JOptionPane.showMessageDialog(frmMainFrame, "FORMATO DE COMPASSO INVÁLIDO ❌\nInforme um valor válido ex: 4/4, 6/8, 12/8.", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            DrumConfigWindow drumWindow = new DrumConfigWindow(frmMainFrame, drumConfig, measureText.trim());
            drumWindow.setVisible(true);
        });

        JMenuItem mitBass = new JMenuItem("Bass");
        mitBass.addActionListener(_ -> openBassConfig());

        JMenuItem mitGuitar = new JMenuItem("Guitar");
        mitGuitar.addActionListener(_ -> openGuitarConfig());

        JMenuItem mitKeyboard = new JMenuItem("Keyboard");
        mitKeyboard.addActionListener(_ -> openKeyboardConfig());

        mnuInstrument.add(mitDrum);
        mnuInstrument.add(mitBass);
        mnuInstrument.add(mitGuitar);
        mnuInstrument.add(mitKeyboard);
        mnbMenuBar.add(mnuInstrument);
        frmMainFrame.add(mnbMenuBar);
    }

    private void openGuitarConfig() {
        TimeSignatureInfo timeInfo = currentTimeSignatureOrWarn("configurar a guitarra");
        if (timeInfo == null) {
            return;
        }

        GuitarConfigWindow window = new GuitarConfigWindow(frmMainFrame, guitarConfig, timeInfo, null);
        window.setVisible(true);
    }

    private void openBassConfig() {
        TimeSignatureInfo timeInfo = currentTimeSignatureOrWarn("configurar o baixo");
        if (timeInfo == null) {
            return;
        }

        bassConfig.syncToTimeSignature(timeInfo);
        BassConfigWindow window = new BassConfigWindow(frmMainFrame, bassConfig, timeInfo);
        window.setVisible(true);
    }

    private void openKeyboardConfig() {
        TimeSignatureInfo timeInfo = currentTimeSignatureOrWarn("configurar o teclado");
        if (timeInfo == null) {
            return;
        }

        keyboardConfig.syncToTimeSignature(timeInfo);
        KeyboardConfigWindow window = new KeyboardConfigWindow(frmMainFrame, keyboardConfig, timeInfo);
        window.setVisible(true);
    }

    private TimeSignatureInfo currentTimeSignatureOrWarn(String action) {
        String measureText = txtMeasure.getText();
        if (measureText == null || measureText.isBlank() || !isMeasure(measureText)) {
            JOptionPane.showMessageDialog(frmMainFrame,
                    "Informe um compasso válido antes de " + action + ".\nEx: 4/4, 6/8.",
                    "Compasso inválido", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return TimeSignatureInfo.parse(measureText.trim());
    }

    private void setupLabelsAndFields() {
        frmMainFrame.add(createLabel("Progressão", 50, 50));
        frmMainFrame.add(txtProgression);

        frmMainFrame.add(createLabel("BPM", 50, 100));
        frmMainFrame.add(txtBpm);

        frmMainFrame.add(createLabel("Compasso", 50, 150));
        frmMainFrame.add(txtMeasure);

        frmMainFrame.add(createLabel("MIN", 50, 200));
        frmMainFrame.add(txtDurationMin);

        frmMainFrame.add(createLabel("SEG", 450, 200));
        frmMainFrame.add(txtDurationSeg);
    }

    private void setupCheckBoxes() {
        chkGuitar = createCheckBox("Guitar", 50, 300);
        chkKeyboard = createCheckBox("KeyBoard", 50, 330);
        chkDrums = createCheckBox("Drums", 50, 360);
        chkBass = createCheckBox("Bass", 50, 390);

        frmMainFrame.add(chkGuitar);
        frmMainFrame.add(chkKeyboard);
        frmMainFrame.add(chkDrums);
        frmMainFrame.add(chkBass);
    }

    private JButton createGenerateButton() {
        JButton btnGeneration = new JButton("Gerar");
        btnGeneration.setSize(180, 25);
        btnGeneration.setLocation(300, 500);
        btnGeneration.setFont(new Font("ChelthmITC BT", Font.BOLD, 18));

        btnGeneration.addActionListener(_ -> {
            if (isProgression(txtProgression.getText())
                    && isMeasure(txtMeasure.getText())
                    && isBpm(txtBpm.getText())
                    && isDuration(txtDurationMin.getText(), txtDurationSeg.getText())) {
                if (!chkDrums.isSelected() && !chkGuitar.isSelected()
                        && !chkBass.isSelected() && !chkKeyboard.isSelected()) {
                    JOptionPane.showMessageDialog(frmMainFrame,
                            "Selecione ao menos um instrumento para gerar.",
                            "Nenhum instrumento", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                BackingTrack backingTrack = new BackingTrack(
                        txtProgression.getText(),
                        txtBpm.getText(),
                        txtMeasure.getText(),
                        txtDurationMin.getText(),
                        txtDurationSeg.getText()
                );
                MidiGenerationService midiGenerationService = new MidiGenerationService(
                        backingTrack, drumConfig, guitarConfig, bassConfig, keyboardConfig,
                        chkDrums.isSelected(), chkGuitar.isSelected(),
                        chkBass.isSelected(), chkKeyboard.isSelected());
                midiGenerationService.generate();
            } else {
                JOptionPane.showMessageDialog(frmMainFrame, "DADOS INVÁLIDOS ❌", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        return btnGeneration;
    }

    private JLabel createLabel(String text, int x, int y) {
        JLabel label = new JLabel(text);
        label.setSize(new Dimension(200, 25));
        label.setLocation(x, y);
        label.setFont(new Font("ChelthmITC BK BT", Font.BOLD, 18));
        return label;
    }

    private JTextField createTextField(int x, int y, int width, int height) {
        JTextField textField = new JTextField();
        textField.setEditable(true);
        textField.setLocation(x, y);
        textField.setSize(new Dimension(width, height));
        textField.setFont(new Font("ChelthmITC BK BT", Font.BOLD, 18));
        return textField;
    }

    private JTextField createNumericTextField(int x, int y, int width, int height) {
        JTextField textField = createTextField(x, y, width, height);
        TextFieldUtils.onlyNumbers(textField);
        return textField;
    }

    private JCheckBox createCheckBox(String text, int x, int y) {
        JCheckBox checkBox = new JCheckBox(text);
        checkBox.setSelected(true);
        checkBox.setFont(new Font("ChelthmITC BK", Font.BOLD, 18));
        checkBox.setSize(200, 25);
        checkBox.setLocation(x, y);
        return checkBox;
    }

    private boolean isProgression(String progression) {
        if (progression == null || progression.isBlank()) {
            return false;
        }
        String[] chord = progression.trim().split("\\s*-\\s*");
        if (chord.length == 0) {
            return false;
        }
        for (String c : chord) {
            if (!isChord(c)) {
                return false;
            }
        }
        return true;
    }

    private boolean isChord(String chord) {
        if (chord == null || chord.isEmpty()) {
            return false;
        }
        chord = chord.trim();
        String regex = "^[A-G][#b]?(m|min|maj|dim|°|º|\\+|sus2|sus4|sus)?[0-9]*(/([A-G][#b]?|[0-9]+))*$";
        return chord.matches(regex);
    }

    private boolean isMeasure(String measure) {
        if (measure == null || measure.isBlank()) {
            return false;
        }
        try {
            TimeSignatureInfo.parse(measure);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isBpm(String bpm) {
        if (bpm == null || bpm.isBlank()) {
            return false;
        }
        try {
            int value = Integer.parseInt(bpm);
            return value >= 20 && value <= 300;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isDuration(String minutes, String seconds) {
        if (minutes == null || minutes.isBlank()
                || seconds == null || seconds.isBlank()) {
            return false;
        }
        try {
            int min = Integer.parseInt(minutes);
            int seg = Integer.parseInt(seconds);

            if (min < 0) {
                return false;
            }
            if (seg < 0 || seg > 59) {
                return false;
            }
            return min > 0 || seg > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
