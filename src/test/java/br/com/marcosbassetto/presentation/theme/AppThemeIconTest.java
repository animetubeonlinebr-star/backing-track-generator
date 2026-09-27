package br.com.marcosbassetto.presentation.theme;

import org.junit.jupiter.api.Test;

import java.awt.Image;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cada janela de instrumento usa um ícone próprio. O carregamento precisa
 * funcionar no classpath empacotado e devolver imagens já materializadas —
 * {@code getScaledInstance} é preguiçoso e devolveria {@code -1} de largura.
 */
class AppThemeIconTest {

    private static final String[] INSTRUMENTS = {"bass", "drum", "guitar", "keyboard"};

    @Test
    void eachInstrumentHasItsOwnIcon() {
        for (String instrument : INSTRUMENTS) {
            List<Image> icons = AppTheme.loadWindowIcons(instrument);
            assertFalse(icons.isEmpty(), instrument + " deveria ter icone");
        }
    }

    @Test
    void iconsAreAvailableInWindowSizes() {
        List<Image> icons = AppTheme.loadWindowIcons("bass");

        assertEquals(5, icons.size(), "16/24/32/48/64");
        assertEquals(16, icons.get(0).getWidth(null));
        assertEquals(64, icons.get(4).getWidth(null));
    }

    @Test
    void iconsAreSquareAndMaterialized() {
        for (String instrument : INSTRUMENTS) {
            for (Image icon : AppTheme.loadWindowIcons(instrument)) {
                int width = icon.getWidth(null);
                int height = icon.getHeight(null);

                // -1 indicaria imagem preguiçosa: o seletor do sistema nao
                // reconheceria o tamanho e escolheria o icone errado.
                assertTrue(width > 0, instrument + ": largura nao materializada");
                assertEquals(width, height, instrument + ": icone nao quadrado");
            }
        }
    }

    @Test
    void iconsAreDistinctPerInstrument() {
        Image bass = AppTheme.loadWindowIcons("bass").get(2);
        Image drum = AppTheme.loadWindowIcons("drum").get(2);
        Image guitar = AppTheme.loadWindowIcons("guitar").get(2);
        Image keyboard = AppTheme.loadWindowIcons("keyboard").get(2);

        assertNotSame(bass, drum);
        assertNotSame(bass, guitar);
        assertNotSame(bass, keyboard);
        assertNotSame(drum, guitar);
        assertNotSame(drum, keyboard);
        assertNotSame(guitar, keyboard);
    }

    @Test
    void repeatedCallsReuseTheSameInstance() {
        assertSame(AppTheme.loadWindowIcons("guitar"), AppTheme.loadWindowIcons("guitar"));
    }

    @Test
    void unknownInstrumentDegradesGracefully() {
        List<Image> icons = AppTheme.loadWindowIcons("nao-existe");
        assertNotNull(icons);
        assertTrue(icons.isEmpty(), "ausencia de imagem nao pode quebrar a janela");
    }

    private static void assertNotSame(Object a, Object b) {
        assertTrue(a != b, "icones de instrumentos diferentes nao podem ser o mesmo objeto");
    }
}
