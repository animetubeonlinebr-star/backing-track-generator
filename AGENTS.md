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
- `model.guitar` — `GuitarConfig` (pattern + strum offset + articulation + velocity levels + program), `GuitarRhythmPattern` (per-step `AttackType` grid with presets), `GuitarArticulation` (`BATIDA`/`DEDILHADO`/`MISTA`).
- `model.bass` — `BassConfig` (preset + velocity level + program), `BassRhythmPattern` (per-step `BassNoteType` grid with presets).
- `model.keyboard` — `KeyboardConfig` (preset + velocity level + program + sustain), `KeyboardRhythmPattern` (per-step `AttackType` grid with presets).
- `model.performance` — `VelocityLevel` (`FRACO`/`MEDIO`/`ALTO`, each with an average) and `VelocityHumanizer` (random velocity around an average, in steps of 5, clamped to 1–127).

## Export: one file per instrument
`generate()` asks for a **directory** (not a filename) and writes one file per enabled instrument using fixed names — `bass.midi`, `guitar.midi`, `keyboard.midi`, `drums.midi` (see `MidiGenerationService.Instrument`). The files are complementary: opened together they form one backing track, so they must all share BPM, time signature and tick length. That consistency comes from `createBaseSequence()`, which every file starts from, and `totalTicks(...)`, both derived from the single `BackingTrack`. Adding a header in only one file (or computing `totalTicks` differently per instrument) desynchronizes the set — the tests `everyFileCarriesTheSameTempoAndTimeSignature` and `everyFileContainsOnlyItsOwnInstrumentChannelAndHarmonyHeader` guard it.
- `generateTo(File)` is the non-interactive path used by tests; it creates the directory if needed and returns the files written. `createSequence()` still builds the full mix (harmony + all enabled instruments) and is the side-effect-free path for tests.
- Each individual file contains the shared control track (tempo + time signature) plus only its own instrument; the harmony track (channel 0) belongs to the mix and is not duplicated into every file.

## Rhythm as a pattern (not per-note expression)
For backing tracks each instrument is defined by WHEN and WHAT it plays, not HOW each note is expressed. There is deliberately no pitch bend / vibrato / slide / slap.
- `GuitarRhythmPattern` is a per-step `AttackType` grid: `STRUM_DOWN`, `STRUM_UP`, `PICK`, `NONE`. Presets: `BATIDA_BASICA`, `BATIDA_BALADA`, `BATIDA_ROCK`, `DEDILHADO`, `REGGAE`.
- `BassRhythmPattern` is a per-step `BassNoteType` grid: `ROOT`, `FIFTH`, `OCTAVE`, `NONE`. Presets: `FUNDAMENTAL_SIMPLES`, `FUNDAMENTAL_E_QUINTA`, `CAMINHANTE`, `REGGAE`. The guitar only decides "hit or not"; the bass also decides *which chord degree* — that is the key difference.
- `KeyboardRhythmPattern` is a per-step `AttackType` grid: `NONE`, `BLOCK`, `ARP_UP`, `ARP_DOWN`. Presets: `PAD_SUSTENTADO`, `BLOCO_RITMICO`, `ARPEJO_UP`, `ARPEJO_DOWN`.
- Patterns are independent of the chord, so swapping the progression keeps the groove.
- Both rhythm patterns follow the same safety rules: constructor fills with NONE, `setX` bounds-checks, and presets are written relative to `beatsPerMeasure()`/`stepsPerBeat()` so any meter works.

## Sustained vs. articulated notes
The keyboard's identity is that it can hold a chord. Two rules make that work:
- A BLOCK note is held until the **next attack** (or the measure end) — not for one step. Holding a pad for a single 16th step turns it into a blip, and the bug hides whenever the sustain pedal is on. The hold is scaled by a fixed `SUSTAINED_DURATION_PERCENT` (95%), because the duration is no longer user-editable.
- The sustain pedal (CC 64) is per measure: on at the downbeat, off near the measure end. The release is always emitted, including in a partial final measure, otherwise the pedal stays stuck on.
- Arpeggio sub-notes must be bounded by `totalTicks` — the loop gate only checks the arpeggio's first note, so later notes can otherwise land past the end of the piece.

## Duration and velocity (automatic)
Note duration is no longer a UI setting. The bass derives it from the meter (`stepTicks - ARTICULATION_GAP_TICKS`, i.e. almost a full step) and the keyboard from the span until the next attack, so the note lengths follow BPM/meter from the main form and the MIDI length comes from the min/seg duration there.
- Velocity is chosen as a `VelocityLevel` (`FRACO`/`MEDIO`/`ALTO`) and then humanized per note by `VelocityHumanizer`: a random offset in steps of 5 around the level's average, amplitude ±average/3, clamped to 1–127. A fixed velocity sounds mechanical; the sample mean still tracks the chosen level, which is the property the tests assert.
- `GuitarConfig` has three levels (down/up/pick) plus a `GuitarArticulation`. The service only reads the level matching the articulation in effect: `BATIDA` uses down/up and `DEDILHADO` uses pick; `MISTA` (the default) uses all three and leaves the preset untouched. `GuitarMidiService.effectiveAttack` rewrites the preset's attacks to match the chosen articulation (picks become down-strums in `BATIDA`, strums become picks in `DEDILHADO`), because the rhythm presets mix both textures and would otherwise reintroduce the disabled one.

## Registers and voice leading
`VoicingService` does NOT transpose each note until it "fits" — that destroys the voicing. It rebuilds the chord inside the instrument region, choosing the inversion with least movement from the previous voicing.
- Preferred regions: bass `E1–G2` (single root), guitar `G2–E4` (closed voicing), keyboard `C4–C6` (spread/drop-2). Drums are channel 9.
- `GuitarMidiService` carries `previousVoicing` measure to measure — that memory is what makes the voice leading work. The bass has no such state: it is monophonic and derives fifth/octave from the root, so there is nothing to conduct. `KeyboardMidiService` also carries `previousVoicing`.
- Register takes priority over voice leading: a voicing is never chosen outside its region.
- Guitar (43–64) and keyboard (60–84) touch in the band 60–64, so they can share a boundary note (e.g. the D4 of G, Bb, Bm). Raising `KEYBOARD_LOW` to remove the overlap would break the keyboard's drop-2 spread, because the dropped middle note would fall below the floor — so the overlap is documented and covered by `guitarAndKeyboardOverlapOnlyInTheBoundaryBand` instead of removed.
- Bass fifth/octave are derived via `VoicingService.bassFifth` / `bassOctave`, never with a naive `root + 7` / `root + 12`. A root at G2 (43) would otherwise put the octave at 55, inside the guitar region. Derived notes are capped at `BASS_DERIVED_HIGH` (50); when neither octave is valid (e.g. root 39), the root itself is repeated rather than leaving the region.
- Because `ChordService` returns pitch classes, the voicing layer is instrument-agnostic.

## MIDI channel allocation
- 0 — harmony (chords)
- 1 — bass
- 2 — guitar
- 3 — keyboard
- 9 — drums (GM percussion)

## Conventions & gotchas
- `MidiGenerationService.createSequence()` and `generateTo(File)` are public and side-effect free (besides writing files) — use them for tests; `generate()` opens a directory chooser and is interactive.
- MIDI velocity/note values must be 0–127 or `ShortMessage` throws; clamp in config setters. `VelocityHumanizer` clamps too, since the offsets are computed from the average.
- The UI combo boxes hold `VelocityLevel`/`GuitarArticulation` values directly, so `toString()` returns the Portuguese label shown to the user; `fromLabel` accepts either the label or the enum name for parsing.
- `GuitarConfig.syncPatternTo` tracks the applied preset/size, not just the size, because the default is 16 steps which equals 4/4 — a size-only check silently skips applying the preset.
- `BassConfig.applyPreset` sets the preset before syncing, otherwise a simultaneous meter change re-applies the OLD preset via `resizeSteps(..., reapplyPreset=true)`.
- Preset-based grids ignore out-of-range steps and constructors fill with NONE, never `null`.
- The bass's `FIFTH` is always a perfect fifth (+7), so diminished/augmented chords get a non-chord tone. This is an accepted trade-off from the design, not an oversight — revisit if altered chords become common.
- UI strings and comments are in Portuguese; class/method names in English.
- Remaining stubs/placeholders: `DrumMidiConfigWindow` if empty, `model.music.Chord`, `presentation.ChordsSymbols`. All four instrument checkboxes in `MainWindow` are now wired. There is no keyboard option to reuse `VoicingService.forKeyboard` for anything else; the harmony track (channel 0) is a simple block-chord track and is a candidate to be replaced by the keyboard track.
- `gradlew` executable bit and Gradle config-cache are sensitive to the environment; prefer `--no-daemon` in CI/sandbox.
