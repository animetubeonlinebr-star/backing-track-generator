package br.com.marcosbassetto.presentation.theme;

import java.awt.*;

public final class AppTheme {

    private AppTheme() {}

    public static final Color COLOR_BG = new Color(248, 249, 250);
    public static final Color COLOR_SURFACE = Color.WHITE;
    public static final Color COLOR_PRIMARY = new Color(0, 102, 204);
    public static final Color COLOR_TEXT_PRIMARY = new Color(33, 37, 41);
    public static final Color COLOR_TEXT_MUTED = new Color(108, 117, 125);

    public static final Color COLOR_GRID_BORDER = new Color(222, 226, 230);
    public static final Color COLOR_BEAT_SEPARATOR = new Color(0, 102, 204);
    public static final Color COLOR_CELL_OFF = new Color(238, 242, 246);
    public static final Color COLOR_CELL_ON = new Color(40, 167, 69); // Verde de ativação
    public static final Color COLOR_CELL_HOVER = new Color(204, 229, 255);
    public static final Color COLOR_PLAYHEAD = new Color(255, 153, 0, 160); // Laranja semi-transparente

    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_BEAT = new Font("Segoe UI", Font.BOLD, 11);
    public static final Font FONT_SUBDIVISION = new Font("Segoe UI", Font.PLAIN, 10);
    public static final Font FONT_LABEL = new Font("Segoe UI", Font.BOLD, 12);

    public static final int STEP_WIDTH = 26;
    public static final int STEP_HEIGHT = 26;
    public static final int ROW_HEIGHT = 36;
    public static final int BEAT_GAP = 14;
    public static final int INSTRUMENT_COL_WIDTH = 140;
    public static final int HEADER_HEIGHT = 60;
}