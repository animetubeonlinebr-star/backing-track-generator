package br.com.marcosbassetto.model.music;

import br.com.marcosbassetto.model.bass.BassConfig;
import br.com.marcosbassetto.model.drum.DrumConfig;
import br.com.marcosbassetto.model.guitar.GuitarConfig;
import br.com.marcosbassetto.model.keyboard.KeyboardConfig;
import br.com.marcosbassetto.service.ChordService;
import br.com.marcosbassetto.service.VoicingService;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StylePresetTest {

    private static final List<String> PROGRESSION =
            List.of("C", "Am", "Dm", "G7", "F", "Bb", "Em", "Bm");

    @Test
    void exposesTheFiveRequestedStyles() {
        assertEquals(5, StylePreset.values().length);
        assertEquals(List.of("Blues", "Bossa Nova", "Jazz", "Reggae", "Rock"),
                List.of(StylePreset.allLabels()));
    }

    @Test
    void everyStyleHasAValidMeasureAndRegisters() {
        for (StylePreset style : StylePreset.values()) {
            assertNotNull(TimeSignatureInfo.parse(style.getMeasure()),
                    style + " sem compasso valido");
            assertNotNull(style.getIntensity());
            assertNotNull(style.getGuitarPattern());
            assertNotNull(style.getBassPattern());
            assertNotNull(style.getKeyboardPattern());
            assertTrue(style.getGuitarRegister().span() >= 12,
                    style + ": registro da guitarra estreito demais para um acorde");
            assertTrue(style.getKeyboardRegister().span() >= 12,
                    style + ": registro do teclado estreito demais para drop 2");
        }
    }

    /**
     * A regra central do pedido: a guitarra não ocupa o espaço do baixo, e o
     * teclado não ocupa o espaço de nenhum dos dois. Regiões disjuntas, em todos
     * os estilos.
     */
    @Test
    void instrumentRegistersNeverOverlapInAnyStyle() {
        for (StylePreset style : StylePreset.values()) {
            assertTrue(style.getBassRegister().high() < style.getGuitarRegister().low(),
                    style + ": baixo e guitarra se sobrepoem");
            assertTrue(style.getGuitarRegister().high() < style.getKeyboardRegister().low(),
                    style + ": guitarra e teclado se sobrepoem");
        }
    }

    @Test
    void eachStyleIsDistinctFromTheOthers() {
        Set<String> fingerprints = new HashSet<>();
        for (StylePreset style : StylePreset.values()) {
            String fingerprint = style.getMeasure() + "|" + style.getGuitarPattern()
                    + "|" + style.getBassPattern() + "|" + style.getKeyboardPattern();
            assertTrue(fingerprints.add(fingerprint),
                    style + " repete a configuracao de outro estilo");
        }
    }

    @Test
    void resolvesByDisplayedLabel() {
        assertEquals(StylePreset.BOSSA_NOVA, StylePreset.fromLabel("Bossa Nova"));
        assertEquals(StylePreset.ROCK, StylePreset.fromLabel("Rock"));
        assertEquals(null, StylePreset.fromLabel("Samba"));
        assertEquals(null, StylePreset.fromLabel(null));
        assertEquals("Blues", StylePreset.BLUES.toString());
    }

    /**
     * Aplica o estilo em configs reais e confere que as notas geradas respeitam
     * as regiões — a separação de altura acontece de fato no MIDI, não só no
     * papel.
     */
    @Test
    void generatedVoicingsRespectEachStyleRegister() {
        ChordService chordService = new ChordService();

        for (StylePreset style : StylePreset.values()) {
            TimeSignatureInfo timeInfo = TimeSignatureInfo.parse(style.getMeasure());
            GuitarConfig guitar = new GuitarConfig();
            guitar.applyStyle(style, timeInfo);
            BassConfig bass = new BassConfig(timeInfo);
            bass.applyStyle(style, timeInfo);
            KeyboardConfig keyboard = new KeyboardConfig(timeInfo);
            keyboard.applyStyle(style, timeInfo);

            for (String symbol : PROGRESSION) {
                List<Integer> raw = chordService.getMidiNotes(symbol);

                for (int note : VoicingService.forBass(raw, bass.getRegister())) {
                    assertTrue(bass.getRegister().contains(note),
                            style + "/" + symbol + ": baixo fora do registro: " + note);
                }
                for (int note : VoicingService.forGuitar(raw, null, guitar.getRegister())) {
                    assertTrue(guitar.getRegister().contains(note),
                            style + "/" + symbol + ": guitarra fora do registro: " + note);
                }
                for (int note : VoicingService.forKeyboard(raw, null, keyboard.getRegister())) {
                    assertTrue(keyboard.getRegister().contains(note),
                            style + "/" + symbol + ": teclado fora do registro: " + note);
                }
            }
        }
    }

    @Test
    void bassStaysBelowGuitarAndKeyboardInEveryStyle() {
        ChordService chordService = new ChordService();

        for (StylePreset style : StylePreset.values()) {
            for (String symbol : PROGRESSION) {
                List<Integer> raw = chordService.getMidiNotes(symbol);

                List<Integer> bass = VoicingService.forBass(raw, style.getBassRegister());
                List<Integer> guitar = VoicingService.forGuitar(raw, null, style.getGuitarRegister());
                List<Integer> keyboard =
                        VoicingService.forKeyboard(raw, null, style.getKeyboardRegister());

                int bassTop = bass.get(bass.size() - 1);
                int guitarBottom = guitar.get(0);
                int guitarTop = guitar.get(guitar.size() - 1);
                int keyboardBottom = keyboard.get(0);

                assertTrue(bassTop < guitarBottom,
                        style + "/" + symbol + ": baixo (" + bassTop
                                + ") alcanca a guitarra (" + guitarBottom + ")");
                assertTrue(bassTop < keyboardBottom,
                        style + "/" + symbol + ": baixo alcanca o teclado");
                assertTrue(guitarTop < keyboardBottom,
                        style + "/" + symbol + ": guitarra (" + guitarTop
                                + ") alcanca o teclado (" + keyboardBottom + ")");
            }
        }
    }

    @Test
    void styleIsAppliedToEveryInstrumentConfig() {
        TimeSignatureInfo blues = TimeSignatureInfo.parse(StylePreset.BLUES.getMeasure());

        GuitarConfig guitar = new GuitarConfig();
        guitar.applyStyle(StylePreset.BLUES, blues);
        assertEquals(StylePreset.BLUES.getGuitarPattern(), guitar.getPreset());
        assertEquals(StylePreset.BLUES.getIntensity(), guitar.getIntensity());
        assertEquals(StylePreset.BLUES.getGuitarRegister(), guitar.getRegister());

        BassConfig bass = new BassConfig(TimeSignatureInfo.parse("4/4"));
        bass.applyStyle(StylePreset.BLUES, blues);
        assertEquals(StylePreset.BLUES.getBassPattern(), bass.getPreset());
        assertEquals(StylePreset.BLUES.getIntensity(), bass.getIntensity());
        assertEquals(StylePreset.BLUES.getBassRegister(), bass.getRegister());

        KeyboardConfig keyboard = new KeyboardConfig(TimeSignatureInfo.parse("4/4"));
        keyboard.applyStyle(StylePreset.BLUES, blues);
        assertEquals(StylePreset.BLUES.getKeyboardPattern(), keyboard.getPreset());
        assertEquals(StylePreset.BLUES.getIntensity(), keyboard.getIntensity());
        assertEquals(StylePreset.BLUES.getKeyboardRegister(), keyboard.getRegister());
    }

    @Test
    void styleResizesInstrumentGridsToTheStyleMeasure() {
        TimeSignatureInfo twelveEight = TimeSignatureInfo.parse(StylePreset.BLUES.getMeasure());
        assertEquals(24, twelveEight.totalSteps(),
                "12/8 sao 4 pulsos de 6 passos");

        GuitarConfig guitar = new GuitarConfig();
        guitar.applyStyle(StylePreset.BLUES, twelveEight);
        assertEquals(24, guitar.getPattern().getTotalSteps());

        BassConfig bass = new BassConfig(TimeSignatureInfo.parse("4/4"));
        bass.applyStyle(StylePreset.BLUES, twelveEight);
        assertEquals(24, bass.getPattern().getTotalSteps());

        KeyboardConfig keyboard = new KeyboardConfig(TimeSignatureInfo.parse("4/4"));
        keyboard.applyStyle(StylePreset.BLUES, twelveEight);
        assertEquals(24, keyboard.getPattern().getTotalSteps());
    }

    @Test
    void drumPatternIsAdjustedPerStyle() {
        TimeSignatureInfo fourFour = TimeSignatureInfo.parse("4/4");
        Set<String> patterns = new HashSet<>();

        for (StylePreset style : StylePreset.values()) {
            DrumConfig drums = new DrumConfig(fourFour);
            drums.applyStyle(style, fourFour);
            patterns.add(fingerprint(drums));
            assertEquals(style.getIntensity(), drums.getIntensity());
        }

        assertEquals(StylePreset.values().length, patterns.size(),
                "cada estilo deve produzir uma levada de bateria diferente");
    }

    @Test
    void applyingAStyleTwiceIsIdempotent() {
        TimeSignatureInfo timeInfo = TimeSignatureInfo.parse("4/4");

        DrumConfig drums = new DrumConfig(timeInfo);
        GuitarConfig guitar = new GuitarConfig();
        BassConfig bass = new BassConfig(timeInfo);
        KeyboardConfig keyboard = new KeyboardConfig(timeInfo);

        for (StylePreset style : StylePreset.values()) {
            for (int pass = 0; pass < 2; pass++) {
                drums.applyStyle(style, timeInfo);
                guitar.applyStyle(style, timeInfo);
                bass.applyStyle(style, timeInfo);
                keyboard.applyStyle(style, timeInfo);
            }
            assertEquals(style.getGuitarPattern(), guitar.getPreset());
            assertEquals(style.getBassPattern(), bass.getPreset());
            assertEquals(style.getKeyboardPattern(), keyboard.getPreset());
        }
    }

    private static String fingerprint(DrumConfig drums) {
        StringBuilder builder = new StringBuilder();
        for (var instrument : br.com.marcosbassetto.model.drum.DrumInstrument.values()) {
            for (int step = 0; step < drums.getTotalSteps(); step++) {
                builder.append(drums.isHit(instrument, step) ? '1' : '0');
            }
            builder.append('|');
        }
        return builder.toString();
    }
}
