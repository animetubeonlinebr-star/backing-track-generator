package br.com.marcosbassetto.presentation.theme;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Trava que toda janela de instrumento aplique o seu próprio ícone.
 *
 * <p>O guard lê o bytecode compilado em vez de abrir as janelas porque
 * {@code JDialog} não pode ser instanciado sem display: a suíte roda headless.
 * O nome do método e o do instrumento ficam no constant pool da classe, então
 * uma janela esquecida — que foi exatamente a regressão encontrada no
 * {@code DrumMidiConfigWindow} — falha aqui.
 */
class WindowIconWiringTest {

    private static final Map<String, String> JANELA_PARA_INSTRUMENTO = new LinkedHashMap<>();

    static {
        JANELA_PARA_INSTRUMENTO.put("BassConfigWindow", "bass");
        JANELA_PARA_INSTRUMENTO.put("DrumConfigWindow", "drum");
        JANELA_PARA_INSTRUMENTO.put("DrumMidiConfigWindow", "drum");
        JANELA_PARA_INSTRUMENTO.put("GuitarConfigWindow", "guitar");
        JANELA_PARA_INSTRUMENTO.put("KeyboardConfigWindow", "keyboard");
    }

    @Test
    void everyInstrumentWindowAppliesItsIcon() throws IOException {
        for (Map.Entry<String, String> entry : JANELA_PARA_INSTRUMENTO.entrySet()) {
            String janela = entry.getKey();
            String instrumento = entry.getValue();
            String bytecode = readClassBytes("br.com.marcosbassetto.presentation." + janela);

            assertTrue(bytecode.contains("setIconImages"),
                    janela + " nao aplica icone (setIconImages ausente)");
            assertTrue(bytecode.contains(instrumento),
                    janela + " deveria usar o icone '" + instrumento + "'");
        }
    }

    private static String readClassBytes(String className) throws IOException {
        String path = "/" + className.replace('.', '/') + ".class";
        try (InputStream in = WindowIconWiringTest.class.getResourceAsStream(path)) {
            assertTrue(in != null, "classe compilada nao encontrada: " + path);
            return new String(in.readAllBytes(), StandardCharsets.ISO_8859_1);
        }
    }
}
