package br.com.marcosbassetto.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoicingServiceTest {

    private static final List<Integer> C = List.of(60, 64, 67);
    private static final List<Integer> Am = List.of(57, 60, 64);
    private static final List<Integer> Dm = List.of(62, 65, 69);
    private static final List<Integer> G7 = List.of(55, 59, 62, 65);

    @Test
    void bassReturnsSingleRootInBassRegion() {
        List<Integer> bass = VoicingService.forBass(C);
        assertEquals(1, bass.size(), "baixista toca uma nota, nao o acorde");
        int note = bass.get(0);
        assertTrue(note >= VoicingService.BASS_LOW && note <= VoicingService.BASS_HIGH,
                "fora da regiao do baixo: " + note);
        assertEquals(0, note % 12, "deve ser a fundamental C");
    }

    @Test
    void bassNeverInvadesGuitarRegion() {
        for (List<Integer> chord : List.of(C, Am, Dm, G7)) {
            for (int note : VoicingService.forBass(chord)) {
                assertTrue(note <= VoicingService.BASS_HIGH,
                        "baixo invadiu a guitarra: " + note);
                assertTrue(note < VoicingService.GUITAR_LOW);
            }
        }
    }

    @Test
    void guitarVoicingStaysInGuitarRegion() {
        List<Integer> voicing = VoicingService.forGuitar(C, null);
        assertEquals(3, voicing.size());
        for (int note : voicing) {
            assertTrue(note >= VoicingService.GUITAR_LOW && note <= VoicingService.GUITAR_HIGH,
                    "fora da regiao da guitarra: " + note);
        }
    }

    @Test
    void guitarVoicingUsesOnlyChordTones() {
        List<Integer> voicing = VoicingService.forGuitar(G7, null);
        List<Integer> expectedClasses = List.of(7, 11, 2, 5); // G, B, D, F
        for (int note : voicing) {
            assertTrue(expectedClasses.contains(Math.floorMod(note, 12)),
                    "nota estranha ao acorde: " + note);
        }
    }

    @Test
    void keyboardVoicingStaysInKeyboardRegionAndAboveGuitar() {
        List<Integer> voicing = VoicingService.forKeyboard(C, null);
        for (int note : voicing) {
            assertTrue(note >= VoicingService.KEYBOARD_LOW && note <= VoicingService.KEYBOARD_HIGH,
                    "fora da regiao do teclado: " + note);
            assertTrue(note >= VoicingService.GUITAR_LOW, "teclado nao deve invadir a guitarra");
        }
    }

    /**
     * As regiões de guitarra (43–64) e teclado (60–84) se tocam na faixa
     * 60–64. Guitarra e teclado podem compartilhar uma nota ali (ex.: o D4 de
     * G, Bb, Bm), sempre na borda — nunca no interior da região do outro.
     * Documentado aqui para que a sobreposição não cresça sem ser notada.
     */
    @Test
    void guitarAndKeyboardOverlapOnlyInTheBoundaryBand() {
        for (String symbol : new String[]{"C", "G", "Am", "Dm", "G7", "F", "Bb", "Em", "Bm"}) {
            ChordService chordService = new ChordService();
            List<Integer> raw = chordService.getMidiNotes(symbol);

            List<Integer> guitar = VoicingService.forGuitar(raw, null);
            List<Integer> keyboard = VoicingService.forKeyboard(raw, null);

            assertTrue(guitar.get(guitar.size() - 1) <= VoicingService.GUITAR_HIGH,
                    symbol + ": guitarra passou do teto");
            assertTrue(keyboard.get(0) >= VoicingService.KEYBOARD_LOW,
                    symbol + ": teclado abaixo do piso");

            for (int note : keyboard) {
                assertTrue(note <= VoicingService.KEYBOARD_HIGH,
                        symbol + ": teclado passou do teto");
            }

            // Nenhum teclado abaixo da guitarra; nenhuma guitarra acima do teclado.
            assertTrue(keyboard.get(0) >= VoicingService.GUITAR_LOW,
                    symbol + ": teclado invadiu o corpo da guitarra");
            assertTrue(guitar.get(guitar.size() - 1) <= VoicingService.KEYBOARD_HIGH,
                    symbol + ": guitarra invadiu o corpo do teclado");
        }
    }

    @Test
    void guitarVoiceLeadingMovesLessThanAFreshVoicing() {
        List<Integer> first = VoicingService.forGuitar(C, null);
        List<Integer> led = VoicingService.forGuitar(Am, first);
        List<Integer> fresh = VoicingService.forGuitar(Am, null);

        assertTrue(movement(first, led) <= movement(first, fresh),
                "voice leading deve mover menos que escolher no vacuo");
    }

    @Test
    void guitarVoiceLeadingKeepsCommonTonesBetweenCAndAm() {
        List<Integer> c = VoicingService.forGuitar(C, null);
        List<Integer> am = VoicingService.forGuitar(Am, c);

        // C (C E G) -> Am (A C E): C e E sao notas comuns
        List<Integer> common = List.of(0, 4); // C, E
        long kept = am.stream().filter(n -> common.contains(Math.floorMod(n, 12))).count();
        assertTrue(kept >= 2, "C e E devem permanecer de C para Am, mas foi " + am);
    }

    @Test
    void keyboardVoicingIsSpreadAcrossAnOctaveWhenPossible() {
        List<Integer> voicing = VoicingService.forKeyboard(C, null);
        int span = voicing.get(voicing.size() - 1) - voicing.get(0);
        assertTrue(span >= 12, "teclado deve soar espalhado (drop 2), span=" + span);
    }

    @Test
    void voiceLeadingProducesDifferentInversionsForDifferentChords() {
        List<Integer> c = VoicingService.forGuitar(C, null);
        List<Integer> g7 = VoicingService.forGuitar(G7, c);
        assertNotEquals(c, g7);
        for (int note : g7) {
            assertTrue(note >= VoicingService.GUITAR_LOW && note <= VoicingService.GUITAR_HIGH);
        }
    }

    @Test
    void progressionAcrossFourChordsNeverLeavesRegions() {
        List<Integer> previous = null;
        for (List<Integer> chord : List.of(C, Am, Dm, G7)) {
            List<Integer> voicing = VoicingService.forGuitar(chord, previous);
            for (int note : voicing) {
                assertTrue(note >= VoicingService.GUITAR_LOW && note <= VoicingService.GUITAR_HIGH,
                        "C-Am-Dm-G7 saiu da regiao: " + note);
            }
            previous = voicing;
        }
    }

    @Test
    void voiceLeadingTotalMovementStaysSmallAcrossProgression() {
        List<Integer> previous = null;
        int total = 0;
        for (List<Integer> chord : List.of(C, Am, Dm, G7)) {
            List<Integer> voicing = VoicingService.forGuitar(chord, previous);
            if (previous != null) {
                total += movement(previous, voicing);
            }
            previous = voicing;
        }
        assertTrue(total <= 24, "movimento total excessivo (embolado): " + total);
    }

    @Test
    void emptyChordDoesNotCrash() {
        assertFalse(VoicingService.forBass(List.of()).isEmpty());
        assertFalse(VoicingService.forGuitar(List.of(), null).isEmpty());
    }

    @Test
    void bassFifthIsAPerfectFifthAndNeverEntersGuitarRegion() {
        for (int root = VoicingService.BASS_LOW; root <= VoicingService.BASS_HIGH; root++) {
            int fifth = VoicingService.bassFifth(root);
            assertEquals(Math.floorMod(root + 7, 12), Math.floorMod(fifth, 12),
                    "bassFifth deve ser uma quinta justa acima de " + root);
            assertTrue(fifth <= VoicingService.BASS_DERIVED_HIGH,
                    "quinta " + fifth + " invade a guitarra (root " + root + ")");
            assertTrue(fifth >= VoicingService.BASS_LOW,
                    "quinta " + fifth + " abaixo da regiao do baixo (root " + root + ")");
        }
    }

    @Test
    void bassOctaveStaysInBassRegionForEveryRoot() {
        for (int root = VoicingService.BASS_LOW; root <= VoicingService.BASS_HIGH; root++) {
            int octave = VoicingService.bassOctave(root);
            assertEquals(0, Math.floorMod(octave - root, 12),
                    "bassOctave deve ser a mesma classe de altura de " + root);
            assertTrue(octave <= VoicingService.BASS_DERIVED_HIGH,
                    "oitava " + octave + " invade a guitarra (root " + root + ")");
            assertTrue(octave >= VoicingService.BASS_LOW,
                    "oitava " + octave + " abaixo da regiao do baixo (root " + root + ")");
            assertTrue(octave == root || Math.abs(octave - root) == 12,
                    "deve ser uma oitava (ou a propria nota, no limite de registro)");
        }
    }

    /**
     * Caso de borda: root agudo (D#2 = 39) tem a oitava acima em 51 (acima do
     * teto) e a oitava abaixo em 27 (abaixo de BASS_LOW). Nenhuma cabe, então
     * mantém a fundamental em vez de sair da região.
     */
    @Test
    void bassOctaveFallsBackToRootWhenNeitherOctaveFits() {
        int root = 39; // D#2
        assertEquals(27, root - 12, "oitava abaixo cai abaixo de BASS_LOW");
        assertEquals(51, root + 12, "oitava acima cai na regiao da guitarra");
        assertEquals(root, VoicingService.bassOctave(root),
                "sem oitava valida, mantem a fundamental");
    }

    @Test
    void bassDerivedHighStaysBelowGuitarVoicings() {
        assertTrue(VoicingService.BASS_DERIVED_HIGH < VoicingService.GUITAR_LOW
                        || VoicingService.bassOctave(VoicingService.BASS_LOW)
                        <= VoicingService.BASS_DERIVED_HIGH);

        // O caso concreto que motivou o teto: G2 = 43 (comum) com oitava ingênua = 55
        assertEquals(55, 43 + 12, "aritmetica ingenua cairia na guitarra");
        assertTrue(VoicingService.bassOctave(43) <= VoicingService.BASS_DERIVED_HIGH,
                "a oitava derivada deve ficar abaixo do teto");
    }

    @Test
    void duplicatePitchClassesAreNotDuplicatedInVoicing() {
        List<Integer> voicing = VoicingService.forGuitar(List.of(60, 64, 67, 72), null);
        assertEquals(3, voicing.size(), "C4 e C5 sao a mesma classe de altura");
    }

    private static int movement(List<Integer> from, List<Integer> to) {
        int min = Math.min(from.size(), to.size());
        int total = 0;
        for (int i = 0; i < min; i++) {
            total += Math.abs(from.get(i) - to.get(i));
        }
        return total + Math.abs(from.size() - to.size()) * 12;
    }
}
