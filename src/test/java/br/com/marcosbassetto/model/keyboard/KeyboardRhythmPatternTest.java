package br.com.marcosbassetto.model.keyboard;

import br.com.marcosbassetto.model.keyboard.KeyboardRhythmPattern.AttackType;
import br.com.marcosbassetto.model.music.TimeSignatureInfo;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyboardRhythmPatternTest {

    private static List<AttackType> grid(KeyboardRhythmPattern pattern) {
        List<AttackType> steps = new ArrayList<>();
        for (int i = 0; i < pattern.getTotalSteps(); i++) {
            steps.add(pattern.getAttack(i));
        }
        return steps;
    }

    private static long count(KeyboardRhythmPattern pattern, AttackType type) {
        return grid(pattern).stream().filter(t -> t == type).count();
    }

    private static KeyboardRhythmPattern preset(String signature, String name) {
        TimeSignatureInfo info = TimeSignatureInfo.parse(signature);
        return new KeyboardRhythmPattern(info.totalSteps(), info, name);
    }

    @Test
    void constructorFillsWithNoneNotNulls() {
        KeyboardRhythmPattern pattern = new KeyboardRhythmPattern(16);
        for (int i = 0; i < 16; i++) {
            assertNotNull(pattern.getAttack(i));
            assertEquals(AttackType.NONE, pattern.getAttack(i));
        }
    }

    @Test
    void getAttackOutsideRangeReturnsNone() {
        KeyboardRhythmPattern pattern = new KeyboardRhythmPattern(8);
        assertEquals(AttackType.NONE, pattern.getAttack(-1));
        assertEquals(AttackType.NONE, pattern.getAttack(999));
    }

    @Test
    void setAttackOutsideRangeIsIgnored() {
        KeyboardRhythmPattern pattern = new KeyboardRhythmPattern(8);
        pattern.setAttack(50, AttackType.BLOCK);
        assertEquals(AttackType.NONE, pattern.getAttack(0));
    }

    @Test
    void setAttackIgnoresNull() {
        KeyboardRhythmPattern pattern = new KeyboardRhythmPattern(4);
        pattern.setAttack(0, null);
        assertEquals(AttackType.NONE, pattern.getAttack(0));
    }

    @Test
    void padSustentadoHasSingleBlockOnBeatOne() {
        KeyboardRhythmPattern p = preset("4/4", KeyboardRhythmPattern.PRESET_PAD_SUSTENTADO);
        assertEquals(16, p.getTotalSteps());
        assertEquals(AttackType.BLOCK, p.getAttack(0));
        assertEquals(1, count(p, AttackType.BLOCK), "pad deve ter um unico ataque");
    }

    @Test
    void blocoRitmicoInFourFour() {
        KeyboardRhythmPattern p = preset("4/4", KeyboardRhythmPattern.PRESET_BLOCO_RITMICO);
        assertEquals(AttackType.BLOCK, p.getAttack(0), "tempo 1");
        assertEquals(AttackType.BLOCK, p.getAttack(8), "tempo 3");
        assertEquals(AttackType.BLOCK, p.getAttack(10), "contratempo do 3");
        assertEquals(3, count(p, AttackType.BLOCK));
    }

    @Test
    void blocoRitmicoInTwoFourFallsBackToBeatTwo() {
        KeyboardRhythmPattern p = preset("2/4", KeyboardRhythmPattern.PRESET_BLOCO_RITMICO);
        assertEquals(8, p.getTotalSteps());
        assertEquals(AttackType.BLOCK, p.getAttack(0), "tempo 1");
        assertEquals(AttackType.BLOCK, p.getAttack(4), "tempo 2");
        assertEquals(2, count(p, AttackType.BLOCK));
    }

    @Test
    void arpejoUpAttacksOncePerBeat() {
        KeyboardRhythmPattern p = preset("4/4", KeyboardRhythmPattern.PRESET_ARPEJO_UP);
        for (int beat = 0; beat < 4; beat++) {
            assertEquals(AttackType.ARP_UP, p.getAttack(beat * 4), "tempo " + (beat + 1));
        }
        assertEquals(4, count(p, AttackType.ARP_UP));
        assertEquals(0, count(p, AttackType.BLOCK));
    }

    @Test
    void arpejoDownAttacksOncePerBeat() {
        KeyboardRhythmPattern p = preset("4/4", KeyboardRhythmPattern.PRESET_ARPEJO_DOWN);
        assertEquals(4, count(p, AttackType.ARP_DOWN));
        assertEquals(0, count(p, AttackType.ARP_UP));
    }

    @Test
    void arpejoUpInCompoundMeterUsesPulseNotEighth() {
        KeyboardRhythmPattern p = preset("6/8", KeyboardRhythmPattern.PRESET_ARPEJO_UP);
        assertEquals(12, p.getTotalSteps());
        // 6/8 tem 2 pulsos de semínima pontuada (6 passos cada)
        assertEquals(AttackType.ARP_UP, p.getAttack(0));
        assertEquals(AttackType.ARP_UP, p.getAttack(6));
        assertEquals(2, count(p, AttackType.ARP_UP));
    }

    @Test
    void allPresetsWorkInEveryMeter() {
        for (String sig : new String[]{"2/4", "3/4", "4/4", "5/4", "6/8", "7/8", "12/8", "3/8"}) {
            TimeSignatureInfo info = TimeSignatureInfo.parse(sig);
            for (String name : KeyboardRhythmPattern.ALL_PRESETS) {
                KeyboardRhythmPattern p =
                        new KeyboardRhythmPattern(info.totalSteps(), info, name);
                assertEquals(info.totalSteps(), p.getTotalSteps(), sig + "/" + name);
                assertEquals(name, p.getAppliedPreset());
                // Reggae e skank: ataca so no contratempo, nunca no tempo 1.
                assertTrue(count(p, AttackType.NONE) < info.totalSteps(),
                        sig + "/" + name + " nao pode ser silencio total");
            }
        }
    }

    @Test
    void unknownPresetFallsBackToPadAndNormalizesTheName() {
        KeyboardRhythmPattern p = preset("4/4", "NAO_EXISTE");
        assertEquals(KeyboardRhythmPattern.PRESET_PAD_SUSTENTADO, p.getAppliedPreset(),
                "preset desconhecido nao pode ficar registrado como aplicado");
        assertEquals(AttackType.BLOCK, p.getAttack(0));
    }

    @Test
    void presetNameIsCaseInsensitive() {
        KeyboardRhythmPattern p = preset("4/4", "arpejo_up");
        assertEquals(KeyboardRhythmPattern.PRESET_ARPEJO_UP, p.getAppliedPreset());
        assertEquals(AttackType.ARP_UP, p.getAttack(0));
    }

    @Test
    void applyPresetWithNullInfoDoesNotThrow() {
        KeyboardRhythmPattern p = new KeyboardRhythmPattern(8);
        p.applyPreset(KeyboardRhythmPattern.PRESET_ARPEJO_UP, null);
        assertEquals(AttackType.NONE, p.getAttack(0));
    }

    @Test
    void resizeWithReapplyKeepsThePresetInANewMeter() {
        TimeSignatureInfo fourFour = TimeSignatureInfo.parse("4/4");
        KeyboardRhythmPattern p =
                new KeyboardRhythmPattern(16, fourFour, KeyboardRhythmPattern.PRESET_ARPEJO_UP);

        TimeSignatureInfo threeFour = TimeSignatureInfo.parse("3/4");
        p.resizeSteps(threeFour.totalSteps(), threeFour, true);

        assertEquals(12, p.getTotalSteps());
        assertEquals(KeyboardRhythmPattern.PRESET_ARPEJO_UP, p.getAppliedPreset());
        assertEquals(3, count(p, AttackType.ARP_UP), "3 tempos em 3/4");
    }

    @Test
    void resizeWithoutReapplyPreservesManualEdits() {
        TimeSignatureInfo fourFour = TimeSignatureInfo.parse("4/4");
        KeyboardRhythmPattern p =
                new KeyboardRhythmPattern(16, fourFour, KeyboardRhythmPattern.PRESET_PAD_SUSTENTADO);
        p.setAttack(1, AttackType.BLOCK);

        p.resizeSteps(20, fourFour, false);

        assertEquals(20, p.getTotalSteps());
        assertEquals(AttackType.BLOCK, p.getAttack(1), "edicao manual preservada");
        assertEquals(AttackType.NONE, p.getAttack(19));
    }

    @Test
    void clearResetsEverythingToNone() {
        KeyboardRhythmPattern p = preset("4/4", KeyboardRhythmPattern.PRESET_ARPEJO_DOWN);
        p.clear();
        assertEquals(0, count(p, AttackType.ARP_DOWN));
        assertTrue(grid(p).stream().allMatch(t -> t == AttackType.NONE));
    }
}
