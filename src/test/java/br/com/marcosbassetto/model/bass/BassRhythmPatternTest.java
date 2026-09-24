package br.com.marcosbassetto.model.bass;

import br.com.marcosbassetto.model.bass.BassRhythmPattern.BassNoteType;
import br.com.marcosbassetto.model.music.TimeSignatureInfo;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BassRhythmPatternTest {

    private static List<BassNoteType> grid(BassRhythmPattern pattern) {
        List<BassNoteType> steps = new ArrayList<>();
        for (int i = 0; i < pattern.getTotalSteps(); i++) {
            steps.add(pattern.getNoteType(i));
        }
        return steps;
    }

    private static BassRhythmPattern preset(String signature, String preset) {
        TimeSignatureInfo info = TimeSignatureInfo.parse(signature);
        return new BassRhythmPattern(info.totalSteps(), info, preset);
    }

    @Test
    void constructorFillsWithNoneNotNulls() {
        BassRhythmPattern pattern = new BassRhythmPattern(16);
        for (int i = 0; i < 16; i++) {
            assertNotNull(pattern.getNoteType(i));
            assertEquals(BassNoteType.NONE, pattern.getNoteType(i));
        }
    }

    @Test
    void getNoteTypeOutsideRangeReturnsNone() {
        BassRhythmPattern pattern = new BassRhythmPattern(8);
        assertEquals(BassNoteType.NONE, pattern.getNoteType(-1));
        assertEquals(BassNoteType.NONE, pattern.getNoteType(999));
    }

    @Test
    void setNoteTypeOutsideRangeIsIgnored() {
        BassRhythmPattern pattern = new BassRhythmPattern(8);
        pattern.setNoteType(50, BassNoteType.ROOT);
        assertEquals(BassNoteType.NONE, pattern.getNoteType(0));
    }

    @Test
    void setNoteTypeIgnoresNull() {
        BassRhythmPattern pattern = new BassRhythmPattern(4);
        pattern.setNoteType(0, null);
        assertEquals(BassNoteType.NONE, pattern.getNoteType(0));
    }

    @Test
    void fundamentalSimplesInFourFour() {
        BassRhythmPattern p = preset("4/4", BassRhythmPattern.PRESET_FUNDAMENTAL_SIMPLES);
        assertEquals(16, p.getTotalSteps());
        assertEquals(List.of(
                BassNoteType.ROOT, BassNoteType.NONE, BassNoteType.NONE, BassNoteType.NONE,
                BassNoteType.ROOT, BassNoteType.NONE, BassNoteType.NONE, BassNoteType.NONE,
                BassNoteType.ROOT, BassNoteType.NONE, BassNoteType.NONE, BassNoteType.NONE,
                BassNoteType.ROOT, BassNoteType.NONE, BassNoteType.NONE, BassNoteType.NONE),
                grid(p));
    }

    @Test
    void fundamentalSimplesInThreeFour() {
        BassRhythmPattern p = preset("3/4", BassRhythmPattern.PRESET_FUNDAMENTAL_SIMPLES);
        assertEquals(12, p.getTotalSteps());
        assertEquals(3, grid(p).stream().filter(t -> t == BassNoteType.ROOT).count());
    }

    @Test
    void fundamentalSimplesInSixEightHasTwoBeats() {
        BassRhythmPattern p = preset("6/8", BassRhythmPattern.PRESET_FUNDAMENTAL_SIMPLES);
        assertEquals(12, p.getTotalSteps());
        assertEquals(BassNoteType.ROOT, p.getNoteType(0));
        assertEquals(BassNoteType.ROOT, p.getNoteType(6));
        assertEquals(2, grid(p).stream().filter(t -> t == BassNoteType.ROOT).count());
    }

    @Test
    void fundamentalEQuintaAlternates() {
        BassRhythmPattern p = preset("4/4", BassRhythmPattern.PRESET_FUNDAMENTAL_E_QUINTA);
        assertEquals(List.of(
                BassNoteType.ROOT, BassNoteType.NONE, BassNoteType.NONE, BassNoteType.NONE,
                BassNoteType.FIFTH, BassNoteType.NONE, BassNoteType.NONE, BassNoteType.NONE,
                BassNoteType.ROOT, BassNoteType.NONE, BassNoteType.NONE, BassNoteType.NONE,
                BassNoteType.FIFTH, BassNoteType.NONE, BassNoteType.NONE, BassNoteType.NONE),
                grid(p));
    }

    @Test
    void caminhanteCyclesRootFifthOctaveFifth() {
        BassRhythmPattern p = preset("4/4", BassRhythmPattern.PRESET_CAMINHANTE);
        assertEquals(BassNoteType.ROOT, p.getNoteType(0));
        assertEquals(BassNoteType.FIFTH, p.getNoteType(4));
        assertEquals(BassNoteType.OCTAVE, p.getNoteType(8));
        assertEquals(BassNoteType.FIFTH, p.getNoteType(12));
    }

    @Test
    void caminhanteInFiveFour() {
        BassRhythmPattern p = preset("5/4", BassRhythmPattern.PRESET_CAMINHANTE);
        assertEquals(20, p.getTotalSteps());
        assertEquals(BassNoteType.ROOT, p.getNoteType(0));
        assertEquals(BassNoteType.FIFTH, p.getNoteType(4));
        assertEquals(BassNoteType.OCTAVE, p.getNoteType(8));
        assertEquals(BassNoteType.FIFTH, p.getNoteType(12));
        assertEquals(BassNoteType.ROOT, p.getNoteType(16));
    }

    @Test
    void reggaeHasSyncopation() {
        BassRhythmPattern p = preset("4/4", BassRhythmPattern.PRESET_REGGAE);
        assertEquals(BassNoteType.ROOT, p.getNoteType(0), "tempo 1");
        assertEquals(BassNoteType.ROOT, p.getNoteType(6), "contratempo do tempo 2");
        assertEquals(BassNoteType.NONE, p.getNoteType(4), "tempo 2 nao ataca");
    }

    @Test
    void allPresetsWorkInOddMeters() {
        for (String sig : new String[]{"2/4", "3/4", "4/4", "5/4", "6/8", "7/8", "12/8", "3/8"}) {
            TimeSignatureInfo info = TimeSignatureInfo.parse(sig);
            for (String preset : BassRhythmPattern.ALL_PRESETS) {
                BassRhythmPattern p = new BassRhythmPattern(info.totalSteps(), info, preset);
                assertEquals(info.totalSteps(), p.getTotalSteps(), sig + "/" + preset);
                assertEquals(preset, p.getAppliedPreset());
            }
        }
    }

    @Test
    void unknownPresetFallsBackToFundamentalSimples() {
        BassRhythmPattern p = preset("4/4", "NAO_EXISTE");
        assertEquals(BassRhythmPattern.PRESET_FUNDAMENTAL_SIMPLES, p.getAppliedPreset());
        assertEquals(BassNoteType.ROOT, p.getNoteType(0));
    }

    @Test
    void applyPresetWithNullTimeSignatureDoesNotThrow() {
        BassRhythmPattern p = new BassRhythmPattern(8);
        p.applyPreset(BassRhythmPattern.PRESET_CAMINHANTE, null);
        assertEquals(BassNoteType.NONE, p.getNoteType(0));
    }

    @Test
    void resizeWithReapplyPresetKeepsTheSamePreset() {
        TimeSignatureInfo fourFour = TimeSignatureInfo.parse("4/4");
        BassRhythmPattern p = new BassRhythmPattern(16, fourFour, BassRhythmPattern.PRESET_REGGAE);

        TimeSignatureInfo threeFour = TimeSignatureInfo.parse("3/4");
        p.resizeSteps(threeFour.totalSteps(), threeFour, true);

        assertEquals(12, p.getTotalSteps());
        assertEquals(BassRhythmPattern.PRESET_REGGAE, p.getAppliedPreset());
        assertEquals(BassNoteType.ROOT, p.getNoteType(0));
        assertEquals(BassNoteType.ROOT, p.getNoteType(6), "contratempo do tempo 2");
    }

    @Test
    void resizeWithoutReapplyPreservesManualEdits() {
        TimeSignatureInfo fourFour = TimeSignatureInfo.parse("4/4");
        BassRhythmPattern p = new BassRhythmPattern(16, fourFour, BassRhythmPattern.PRESET_FUNDAMENTAL_SIMPLES);
        p.setNoteType(1, BassNoteType.OCTAVE);

        p.resizeSteps(20, fourFour, false);

        assertEquals(20, p.getTotalSteps());
        assertEquals(BassNoteType.OCTAVE, p.getNoteType(1), "edicao manual preservada");
        assertEquals(BassNoteType.NONE, p.getNoteType(19), "novos passos comecam em NONE");
    }

    @Test
    void clearResetsEverythingToNone() {
        BassRhythmPattern p = preset("4/4", BassRhythmPattern.PRESET_CAMINHANTE);
        p.clear();
        assertEquals(0, grid(p).stream().filter(t -> t != BassNoteType.NONE).count());
    }
}
