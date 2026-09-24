package br.com.marcosbassetto.presentation.components;

import br.com.marcosbassetto.model.drum.DrumConfig;
import br.com.marcosbassetto.model.drum.DrumInstrument;
import br.com.marcosbassetto.model.drum.DrumPattern;
import br.com.marcosbassetto.model.music.TimeSignatureInfo;
import br.com.marcosbassetto.presentation.theme.AppTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class SequencerPanel extends JPanel {

    private final DrumConfig drumConfig;
    private int playheadStep = -1; // -1 indica parada. Quando em reprodução: 0 a totalSteps - 1

    public SequencerPanel(DrumConfig drumConfig) {
        this.drumConfig = drumConfig;
        setBackground(AppTheme.COLOR_SURFACE);

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleMouseClick(e.getX(), e.getY());
            }
        };

        addMouseListener(mouseHandler);
    }

    public void setPlayheadStep(int step) {
        this.playheadStep = step;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        TimeSignatureInfo info = drumConfig.getTimeSignatureInfo();
        int totalWidth = AppTheme.INSTRUMENT_COL_WIDTH
                + (info.totalSteps() * AppTheme.STEP_WIDTH)
                + ((info.beatsPerMeasure() - 1) * AppTheme.BEAT_GAP) + 30;
        int totalHeight = AppTheme.HEADER_HEIGHT
                + (DrumInstrument.values().length * AppTheme.ROW_HEIGHT) + 20;

        return new Dimension(totalWidth, totalHeight);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        TimeSignatureInfo timeInfo = drumConfig.getTimeSignatureInfo();
        DrumPattern pattern = drumConfig.getPattern();

        int startX = AppTheme.INSTRUMENT_COL_WIDTH;
        int startY = AppTheme.HEADER_HEIGHT;

        drawHeadersAndSeparators(g2, timeInfo, startX);
        drawGridAndCells(g2, timeInfo, pattern, startX, startY);

        if (playheadStep >= 0 && playheadStep < timeInfo.totalSteps()) {
            drawPlayhead(g2, timeInfo, startX, startY);
        }
    }

    private void drawHeadersAndSeparators(Graphics2D g2, TimeSignatureInfo timeInfo, int startX) {
        int stepsPerBeat = timeInfo.stepsPerBeat();
        int gridHeight = DrumInstrument.values().length * AppTheme.ROW_HEIGHT;

        for (int beat = 0; beat < timeInfo.beatsPerMeasure(); beat++) {
            int beatStartX = calculateStepX(beat * stepsPerBeat, startX, stepsPerBeat);
            int beatWidth = stepsPerBeat * AppTheme.STEP_WIDTH;

            g2.setFont(AppTheme.FONT_BEAT);
            g2.setColor(AppTheme.COLOR_BEAT_SEPARATOR);
            FontMetrics fm = g2.getFontMetrics();
            String beatText = "BEAT " + (beat + 1);
            int textX = beatStartX + (beatWidth - fm.stringWidth(beatText)) / 2;
            g2.drawString(beatText, textX, 22);

            for (int sub = 0; sub < stepsPerBeat; sub++) {
                int stepIndex = (beat * stepsPerBeat) + sub;
                int stepX = calculateStepX(stepIndex, startX, stepsPerBeat);

                g2.setFont(sub == 0 ? AppTheme.FONT_BEAT : AppTheme.FONT_SUBDIVISION);
                g2.setColor(sub == 0 ? AppTheme.COLOR_TEXT_PRIMARY : AppTheme.COLOR_TEXT_MUTED);
                String subText = String.valueOf(sub + 1);
                int subX = stepX + (AppTheme.STEP_WIDTH - g2.getFontMetrics().stringWidth(subText)) / 2;
                g2.drawString(subText, subX, 42);
            }

            if (beat > 0) {
                int sepX = beatStartX - (AppTheme.BEAT_GAP / 2);
                g2.setColor(AppTheme.COLOR_BEAT_SEPARATOR);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(sepX, 10, sepX, AppTheme.HEADER_HEIGHT + gridHeight);
            }
        }
    }

    private void drawGridAndCells(Graphics2D g2, TimeSignatureInfo timeInfo, DrumPattern pattern, int startX, int startY) {
        DrumInstrument[] instruments = DrumInstrument.values();

        for (int row = 0; row < instruments.length; row++) {
            DrumInstrument inst = instruments[row];
            int y = startY + (row * AppTheme.ROW_HEIGHT);

            boolean isEnabled = drumConfig.isInstrumentEnabled(inst);
            g2.setFont(AppTheme.FONT_LABEL);
            g2.setColor(isEnabled ? AppTheme.COLOR_TEXT_PRIMARY : AppTheme.COLOR_TEXT_MUTED);
            g2.drawString(inst.getLabelWithMidi(), 10, y + (AppTheme.STEP_HEIGHT / 2) + 5);

            boolean[] steps = pattern.getSteps(inst);

            for (int step = 0; step < timeInfo.totalSteps(); step++) {
                int x = calculateStepX(step, startX, timeInfo.stepsPerBeat());

                if (isEnabled && steps[step]) {
                    g2.setColor(AppTheme.COLOR_CELL_ON);
                    g2.fillRoundRect(x + 2, y + 2, AppTheme.STEP_WIDTH - 4, AppTheme.STEP_HEIGHT - 4, 6, 6);
                } else {
                    g2.setColor(AppTheme.COLOR_CELL_OFF);
                    g2.fillRoundRect(x + 2, y + 2, AppTheme.STEP_WIDTH - 4, AppTheme.STEP_HEIGHT - 4, 6, 6);
                }

                g2.setColor(AppTheme.COLOR_GRID_BORDER);
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(x + 2, y + 2, AppTheme.STEP_WIDTH - 4, AppTheme.STEP_HEIGHT - 4, 6, 6);
            }
        }
    }

    private void drawPlayhead(Graphics2D g2, TimeSignatureInfo timeInfo, int startX, int startY) {
        int x = calculateStepX(playheadStep, startX, timeInfo.stepsPerBeat());
        int gridHeight = DrumInstrument.values().length * AppTheme.ROW_HEIGHT;

        g2.setColor(AppTheme.COLOR_PLAYHEAD);
        g2.fillRect(x + 1, startY, AppTheme.STEP_WIDTH - 2, gridHeight);
    }

    private void handleMouseClick(int mouseX, int mouseY) {
        TimeSignatureInfo timeInfo = drumConfig.getTimeSignatureInfo();
        int startX = AppTheme.INSTRUMENT_COL_WIDTH;
        int startY = AppTheme.HEADER_HEIGHT;

        if (mouseX < startX) {
            int row = (mouseY - startY) / AppTheme.ROW_HEIGHT;
            if (row >= 0 && row < DrumInstrument.values().length) {
                DrumInstrument inst = DrumInstrument.values()[row];
                drumConfig.setInstrumentEnabled(inst, !drumConfig.isInstrumentEnabled(inst));
                repaint();
            }
            return;
        }

        for (int step = 0; step < timeInfo.totalSteps(); step++) {
            int x = calculateStepX(step, startX, timeInfo.stepsPerBeat());

            if (mouseX >= x && mouseX <= x + AppTheme.STEP_WIDTH) {
                int row = (mouseY - startY) / AppTheme.ROW_HEIGHT;
                if (row >= 0 && row < DrumInstrument.values().length) {
                    DrumInstrument inst = DrumInstrument.values()[row];
                    boolean currentState = drumConfig.getPattern().getStep(inst, step);
                    drumConfig.getPattern().setStep(inst, step, !currentState);
                    repaint();
                }
                break;
            }
        }
    }

    private int calculateStepX(int stepIndex, int startX, int stepsPerBeat) {
        int beatIndex = stepIndex / stepsPerBeat;
        return startX + (stepIndex * AppTheme.STEP_WIDTH) + (beatIndex * AppTheme.BEAT_GAP);
    }
}
