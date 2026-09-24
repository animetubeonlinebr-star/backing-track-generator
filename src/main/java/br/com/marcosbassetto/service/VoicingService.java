package br.com.marcosbassetto.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Traduz as notas cruas do {@link ChordService} (que vêm em Dó central) em
 * voicings apropriados para cada instrumento.
 *
 * <p>Não transpõe notas individualmente até "caberem" — isso destruiria o
 * voicing. Em vez disso reconstrói o acorde dentro da região do instrumento e,
 * quando existe um voicing anterior, escolhe a inversão que se move menos
 * (voice leading).
 */
public final class VoicingService {

    // Regiões preferidas (não limites rígidos)
    public static final int BASS_LOW = 28;      // E1
    public static final int BASS_HIGH = 43;     // G2

    public static final int GUITAR_LOW = 43;    // G2
    public static final int GUITAR_HIGH = 64;   // E4

    public static final int KEYBOARD_LOW = 60;  // C4
    public static final int KEYBOARD_HIGH = 84; // C6

    /**
     * Teto para as notas DERIVADAS do baixo (quinta/oitava). A fundamental
     * cabe em BASS_HIGH, mas um intervalo acima dela poderia invadir o
     * registro da guitarra (voicings em ~52–62).
     */
    public static final int BASS_DERIVED_HIGH = 50;

    private VoicingService() {
    }

    /**
     * Baixo: apenas a fundamental, na região grave. Um baixista toca a nota
     * fundamental — não o acorde inteiro.
     */
    public static List<Integer> forBass(List<Integer> chordNotes) {
        if (chordNotes == null || chordNotes.isEmpty()) {
            return List.of(BASS_LOW);
        }
        int note = chordNotes.stream().min(Integer::compare).orElse(BASS_LOW);
        note = foldInto(note, BASS_LOW, BASS_HIGH);
        return List.of(note);
    }

    /** Guitarra: acorde fechado na região média, com voice leading. */
    public static List<Integer> forGuitar(List<Integer> chordNotes, List<Integer> previousVoicing) {
        return buildVoicing(chordNotes, previousVoicing, GUITAR_LOW, GUITAR_HIGH);
    }

    /**
     * Quinta do baixo, uma quinta justa acima da fundamental. Se isso subir
     * demais e invadir a guitarra, a quinta desce uma oitava — continua sendo
     * a quinta (classe de altura +7) na região do baixo.
     */
    public static int bassFifth(int root) {
        int up = root + 7;
        return up <= BASS_DERIVED_HIGH ? up : up - 12;
    }

    /**
     * Oitava do baixo: uma oitava acima quando couber, senão uma abaixo.
     * Para fundamentais muito agudas (D#2 = 39) nenhuma das duas respeita a
     * região do baixo, então mantém a fundamental — o baixo nunca sai de
     * BASS_LOW..BASS_DERIVED_HIGH.
     */
    public static int bassOctave(int root) {
        int up = root + 12;
        if (up <= BASS_DERIVED_HIGH) {
            return up;
        }
        int down = root - 12;
        return down >= BASS_LOW ? down : root;
    }

    /** Teclado: acorde espalhado (drop 2) na região aguda, com voice leading. */
    public static List<Integer> forKeyboard(List<Integer> chordNotes, List<Integer> previousVoicing) {
        List<Integer> voiced = buildVoicing(chordNotes, previousVoicing, KEYBOARD_LOW, KEYBOARD_HIGH);
        return spreadVoicing(voiced, KEYBOARD_LOW, KEYBOARD_HIGH);
    }

    private static int foldInto(int note, int low, int high) {
        int result = note;
        while (result - 12 >= low) {
            result -= 12;
        }
        while (result < low) {
            result += 12;
        }
        while (result > high) {
            result -= 12;
        }
        return result;
    }

    /**
     * Constrói um voicing dentro de [low, high]. Quando há voicing anterior,
     * escolhe a inversão de menor distância; caso contrário, a mais central.
     * O registro de destino tem prioridade sobre a condução de vozes.
     */
    private static List<Integer> buildVoicing(List<Integer> chordNotes,
                                              List<Integer> previousVoicing,
                                              int low, int high) {
        List<Integer> pitchClasses = distinctPitchClasses(chordNotes);
        if (pitchClasses.isEmpty()) {
            return List.of(low);
        }

        int center = (low + high) / 2;
        List<Integer> best = null;
        int bestScore = Integer.MAX_VALUE;

        for (int base = low - 12; base <= high; base += 12) {
            for (int inversion = 0; inversion < pitchClasses.size(); inversion++) {
                List<Integer> candidate = buildCandidate(pitchClasses, inversion, base);
                if (candidate.get(0) < low || candidate.get(candidate.size() - 1) > high) {
                    continue;
                }

                int score = previousVoicing == null
                        ? Math.abs(candidate.get(0) - center)
                        : voiceDistance(previousVoicing, candidate);

                if (score < bestScore) {
                    bestScore = score;
                    best = candidate;
                }
            }
        }

        return best != null ? best : List.of(foldInto(chordNotes.get(0), low, high));
    }

    private static List<Integer> distinctPitchClasses(List<Integer> chordNotes) {
        if (chordNotes == null || chordNotes.isEmpty()) {
            return List.of();
        }
        Set<Integer> result = new LinkedHashSet<>();
        for (int note : chordNotes) {
            result.add(Math.floorMod(note, 12));
        }
        return new ArrayList<>(result);
    }

    /** Empilha as classes de altura a partir de {@code inversion}, acima de base. */
    private static List<Integer> buildCandidate(List<Integer> pitchClasses, int inversion, int base) {
        List<Integer> result = new ArrayList<>();
        int start = base - Math.floorMod(base, 12) + pitchClasses.get(inversion);
        while (start < base) {
            start += 12;
        }
        result.add(start);

        int current = start;
        for (int i = 1; i < pitchClasses.size(); i++) {
            int pc = pitchClasses.get((inversion + i) % pitchClasses.size());
            int next = current + 1;
            while (Math.floorMod(next, 12) != pc) {
                next++;
            }
            result.add(next);
            current = next;
        }
        return result;
    }

    /** Distância nota-a-nota entre dois voicings; penaliza diferença de tamanho. */
    private static int voiceDistance(List<Integer> a, List<Integer> b) {
        int min = Math.min(a.size(), b.size());
        int distance = 0;
        for (int i = 0; i < min; i++) {
            distance += Math.abs(a.get(i) - b.get(i));
        }
        distance += Math.abs(a.size() - b.size()) * 12;
        return distance;
    }

    /**
     * Espalha o voicing (drop 2). Se o espalhamento sair da região do
     * instrumento — o que faria o teclado invadir a guitarra — mantém o
     * voicing fechado.
     */
    private static List<Integer> spreadVoicing(List<Integer> notes, int low, int high) {
        if (notes.size() < 3) {
            return notes;
        }
        List<Integer> spread = new ArrayList<>(notes);
        spread.set(1, spread.get(1) - 12);
        spread.sort(Integer::compare);
        if (spread.get(0) < low || spread.get(spread.size() - 1) > high) {
            return notes;
        }
        return spread;
    }
}