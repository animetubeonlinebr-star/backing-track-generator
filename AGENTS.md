# AGENTS.md

## Project
Java Swing desktop app that generates MIDI backing tracks (chord harmony + programmable drum grid) from a user-supplied chord progression. See `README`-less repo: entry point `br.com.marcosbassetto.MainApplication`.

## Build & test
JDK 25 is required (`.idea/misc.xml` sets language level JDK_26; JDK 21+ compiles fine).

```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
./gradlew --no-daemon test        # compile + run JUnit 5 tests
./gradlew --no-daemon build
```

- JUnit 6 BOM is configured in `build.gradle`; tests live in `src/test/java`.
- The Gradle wrapper (`gradlew`) must be executable (`chmod +x gradlew`).
- Quick compile-only check without Gradle: `javac -d out $(find src/main -name "*.java")`.

## Architecture
Layers under `src/main/java/br/com/marcosbassetto`:
- `presentation` — Swing UI. `MainWindow` (absolute positioning, `setLayout(null)`), `DrumConfigWindow`, `DrumMidiConfigWindow`, `GuitarConfigWindow`, `components/SequencerPanel` (custom-painted step grid), `theme/AppTheme` (colors/fonts/dimensions).
- `service` — MIDI generation. `MidiGenerationService` orchestrates: builds the `Sequence` (PPQ 480), tempo/time-signature meta events, harmony track, and delegates to `DrumMidiService` / `GuitarMidiService`. `ChordService` parses chord symbols to MIDI notes (root, slash bass, extensions; falls back to C major on invalid input). `VoicingService` maps raw chord notes to instrument registers with voice leading.
- `model.music` — `BackingTrack` (DTO, all fields `String`), `TimeSignatureInfo` (record; validates `N/D`, computes beats/steps/ticks, detects compound meters).
- `model.drum` — `DrumConfig`, `DrumPattern` (boolean grid), `DrumInstrument` (enum with default MIDI notes).
- `model.guitar` — `GuitarConfig` (pattern + strum offset + velocities + program), `GuitarRhythmPattern` (per-step `AttackType` grid with presets).
- `model.bass` — `BassConfig` (preset + velocity + note duration + program), `BassRhythmPattern` (per-step `BassNoteType` grid with presets).

## Rhythm as a pattern (not per-note expression)
For backing tracks each instrument is defined by WHEN and WHAT it plays, not HOW each note is expressed. There is deliberately no pitch bend / vibrato / slide / slap.
- `GuitarRhythmPattern` is a per-step `AttackType` grid: `STRUM_DOWN`, `STRUM_UP`, `PICK`, `NONE`. Presets: `BATIDA_BASICA`, `BATIDA_BALADA`, `BATIDA_ROCK`, `DEDILHADO`, `REGGAE`.
- `BassRhythmPattern` is a per-step `BassNoteType` grid: `ROOT`, `FIFTH`, `OCTAVE`, `NONE`. Presets: `FUNDAMENTAL_SIMPLES`, `FUNDAMENTAL_E_QUINTA`, `CAMINHANTE`, `REGGAE`. The guitar only decides "hit or not"; the bass also decides *which chord degree* — that is the key difference.
- Patterns are independent of the chord, so swapping the progression keeps the groove.
- Both rhythm patterns follow the same safety rules: constructor fills with NONE, `setX` bounds-checks, and presets are written relative to `beatsPerMeasure()`/`stepsPerBeat()` so any meter works.

## Registers and voice leading
`VoicingService` does NOT transpose each note until it "fits" — that destroys the voicing. It rebuilds the chord inside the instrument region, choosing the inversion with least movement from the previous voicing.
- Preferred regions: bass `E1–G2` (single root), guitar `G2–E4` (closed voicing), keyboard `C4–C6` (spread/drop-2). Drums are channel 9.
- `GuitarMidiService` carries `previousVoicing` measure to measure — that memory is what makes the voice leading work. The bass has no such state: it is monophonic and derives fifth/octave from the root, so there is nothing to conduct.
- Register takes priority over voice leading: a voicing is never chosen outside its region.
- Bass fifth/octave are derived via `VoicingService.bassFifth` / `bassOctave`, never with a naive `root + 7` / `root + 12`. A root at G2 (43) would otherwise put the octave at 55, inside the guitar region. Derived notes are capped at `BASS_DERIVED_HIGH` (50); when neither octave is valid (e.g. root 39), the root itself is repeated rather than leaving the region.
- Because `ChordService` returns pitch classes, the voicing layer is instrument-agnostic.

## MIDI channel allocation
- 0 — harmony (chords)
- 1 — bass
- 2 — guitar
- 9 — drums (GM percussion)

## Conventions & gotchas
- `MidiGenerationService.createSequence()` is public and side-effect free — use it for tests; `generate()` opens a `JFileChooser` and is interactive.
- MIDI velocity/note values must be 0–127 or `ShortMessage` throws; clamp in config setters.
- `GuitarConfig.syncPatternTo` tracks the applied preset/size, not just the size, because the default is 16 steps which equals 4/4 — a size-only check silently skips applying the preset.
- `BassConfig.applyPreset` sets the preset before syncing, otherwise a simultaneous meter change re-applies the OLD preset via `resizeSteps(..., reapplyPreset=true)`.
- Preset-based grids ignore out-of-range steps and constructors fill with NONE, never `null`.
- The bass's `FIFTH` is always a perfect fifth (+7), so diminished/augmented chords get a non-chord tone. This is an accepted trade-off from the design, not an oversight — revisit if altered chords become common.
- UI strings and comments are in Portuguese; class/method names in English.
- Remaining stubs/placeholders: `DrumMidiConfigWindow` if empty, `KeyboardMidiService`, `model.music.Chord`, `presentation.ChordsSymbols`. The Keyboard checkbox in `MainWindow` is still not wired to any generator — implementing it should reuse `VoicingService.forKeyboard`.
- `gradlew` executable bit and Gradle config-cache are sensitive to the environment; prefer `--no-daemon` in CI/sandbox.
