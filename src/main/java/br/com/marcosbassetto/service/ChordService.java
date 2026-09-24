package br.com.marcosbassetto.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ChordService {

    public List<Integer> getMidiNotes(String chordName) {
        if (chordName == null || chordName.isBlank()) {
            return List.of(60, 64, 67); // Fallback: C Maior
        }

        String cleanChord = chordName.trim();

        String[] parts = cleanChord.split("/");

        String mainChordPart = parts[0];
        List<String> slashTokens = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isBlank()) {
                slashTokens.add(parts[i].trim());
            }
        }

        String rootStr = parseRoot(mainChordPart);
        if (rootStr == null) {
            return List.of(60, 64, 67); // Fallback para cifra inválida
        }

        int rootMidi = parseRootToMidi(rootStr);
        String remainder = mainChordPart.substring(rootStr.length());

        Integer bassMidi = null;
        List<String> extensionTokens = new ArrayList<>();

        for (String token : slashTokens) {
            if (isNoteName(token)) {
                int bassRootMidi = parseRootToMidi(token);
                if (bassRootMidi != -1) {
                    bassMidi = bassRootMidi - 12;
                }
            } else {
                extensionTokens.add(token);
            }
        }

        Set<Integer> intervals = new LinkedHashSet<>();
        intervals.add(0);

        boolean isDiminished = false;
        boolean isAugmented = false;
        boolean isSus2 = false;
        boolean isSus4 = false;
        boolean isMinor = false;

        if (remainder.contains("sus2")) {
            isSus2 = true;
            remainder = remainder.replace("sus2", "");
        } else if (remainder.contains("sus4") || remainder.contains("sus")) {
            isSus4 = true;
            remainder = remainder.replace("sus4", "").replace("sus", "");
        } else if (remainder.contains("dim") || remainder.contains("°")) {
            isDiminished = true;
            remainder = remainder.replace("dim", "").replace("°", "");
        } else if (remainder.contains("aug") || remainder.contains("+")) {
            isAugmented = true;
            remainder = remainder.replace("aug", "").replace("+", "");
        } else if (!remainder.startsWith("maj") && !remainder.startsWith("Maj")
                && (remainder.startsWith("m") || remainder.startsWith("min"))) {
            isMinor = true;
            remainder = remainder.replaceFirst("min|m", "");
        }

        if (isSus2) {
            intervals.add(2);
            intervals.add(7);
        } else if (isSus4) {
            intervals.add(5);
            intervals.add(7);
        } else if (isDiminished) {
            intervals.add(3);
            intervals.add(6);
        } else if (isAugmented) {
            intervals.add(4);
            intervals.add(8);
        } else if (isMinor) {
            intervals.add(3);
            intervals.add(7);
        } else {
            intervals.add(4);
            intervals.add(7);
        }

        if (remainder.contains("maj7") || remainder.contains("M7")) {
            intervals.add(11);
            remainder = remainder.replace("maj7", "").replace("M7", "");
        } else if (isDiminished && remainder.contains("7")) {
            intervals.add(9);
            remainder = remainder.replace("7", "");
        } else if (remainder.contains("7")) {
            intervals.add(10);
            remainder = remainder.replace("7", "");
        }

        parseAndAddExtension(remainder, intervals);

        for (String extToken : extensionTokens) {
            parseAndAddExtension(extToken, intervals);
        }

        List<Integer> chordNotes = new ArrayList<>();

        if (bassMidi != null) {
            chordNotes.add(bassMidi);
        }

        for (int interval : intervals) {
            chordNotes.add(rootMidi + interval);
        }

        return chordNotes;
    }

    private void parseAndAddExtension(String str, Set<Integer> intervals) {
        if (str == null || str.isBlank()) return;

        if (str.contains("13") || str.contains("add13")) {
            intervals.add(21);
        } else if (str.contains("11") || str.contains("add11")) {
            intervals.add(17);
        } else if (str.contains("9") || str.contains("add9")) {
            intervals.add(14);
        } else if (str.contains("6")) {
            intervals.add(9);
        }
    }

    private String parseRoot(String chord) {
        if (chord == null || chord.isEmpty()) return null;

        if (chord.length() >= 2) {
            char secondChar = chord.charAt(1);
            if (secondChar == '#' || secondChar == 'b' || secondChar == 'B') {
                return chord.substring(0, 2);
            }
        }
        return chord.substring(0, 1);
    }

    private boolean isNoteName(String token) {
        if (token == null || token.isBlank()) return false;
        String normalized = token.trim().toUpperCase();
        return normalized.matches("^[A-G][#B]?$");
    }

    private int parseRootToMidi(String root) {
        if (root == null || root.isBlank()) return -1;

        String normalized = root.trim().toUpperCase();
        if (normalized.length() == 2 && (normalized.charAt(1) == 'B' || normalized.charAt(1) == 'b')) {
            normalized = normalized.charAt(0) + "B";
        }

        switch (normalized) {
            case "C":  return 60;
            case "C#":
            case "DB": return 61;
            case "D":  return 62;
            case "D#":
            case "EB": return 63;
            case "E":  return 64;
            case "F":  return 65;
            case "F#":
            case "GB": return 66;
            case "G":  return 67;
            case "G#":
            case "AB": return 68;
            case "A":  return 69;
            case "A#":
            case "BB": return 70;
            case "B":  return 71;
            default:   return -1;
        }
    }
}