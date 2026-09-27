package br.com.marcosbassetto.presentation.theme;

import javax.swing.ImageIcon;
import java.awt.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    /**
     * Tamanhos usuais de ícone de janela: barra de título, barra de tarefas e
     * telas com escala alta. O sistema escolhe o mais adequado.
     */
    private static final int[] ICON_SIZES = {16, 24, 32, 48, 64};

    private static final Map<String, List<Image>> ICON_CACHE = new ConcurrentHashMap<>();

    /**
     * Carrega as imagens de {@code /img/<nome>.jpg} já reduzidas aos tamanhos
     * usuais de ícone de janela.
     *
     * <p>Os arquivos de origem são grandes (1024×1024), então reduzi-los aqui
     * evita manter um bitmap de vários MB em memória por janela aberta. O
     * resultado é cacheado por nome.
     *
     * @return lista de imagens, ou lista vazia quando o recurso não existe — o
     *         chamador apenas ignora o ícone em vez de quebrar
     */
    public static List<Image> loadWindowIcons(String nome) {
        return ICON_CACHE.computeIfAbsent(nome, AppTheme::scaleWindowIcons);
    }

    private static List<Image> scaleWindowIcons(String nome) {
        URL url = AppTheme.class.getResource("/img/" + nome + ".jpg");
        if (url == null) {
            System.err.println("Imagem não encontrada em /img/" + nome + ".jpg");
            return List.of();
        }
        Image original = new ImageIcon(url).getImage();
        List<Image> icons = new ArrayList<>(ICON_SIZES.length);
        for (int size : ICON_SIZES) {
            Image scaled = original.getScaledInstance(size, size, Image.SCALE_SMOOTH);
            // getScaledInstance devolve uma imagem preguiçosa: getWidth(null)
            // retorna -1 até ela ser materializada. Envolver num ImageIcon
            // força a carga, senão o seletor de ícone do sistema pode não
            // reconhecer o tamanho.
            icons.add(new ImageIcon(scaled).getImage());
        }
        return icons;
    }
}