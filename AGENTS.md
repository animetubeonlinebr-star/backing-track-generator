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
- `model.music` — `BackingTrack` (DTO, all fields `String`), `TimeSignatureInfo` (record; validates `N/D`, computes beats/steps/ticks, detects compound meters), `Intensity` (baixa/média/forte), `Instrument` (per-instrument MIDI file names), `StylePreset` (Blues/Bossa Nova/Jazz/Reggae/Rock), `Register` (per-instrument pitch range).
- `model.drum` — `DrumConfig` (grid + intensity + style), `DrumPattern` (boolean grid; `applyDefaultPattern` and `applyStylePattern`), `DrumInstrument` (enum with default MIDI notes).
- `model.guitar` — `GuitarConfig` (pattern + strum offset + intensity + program + register), `GuitarRhythmPattern` (per-step `AttackType` grid with presets).
- `model.bass` — `BassConfig` (preset + intensity + note duration + program + register), `BassRhythmPattern` (per-step `BassNoteType` grid with presets).
- `model.keyboard` — `KeyboardConfig` (preset + intensity + program + duration + sustain + register), `KeyboardRhythmPattern` (per-step `AttackType` grid with presets).

## Style presets (menu PRESETS)
`StylePreset` (in `model.music`) is the single source of truth for a musical style. Each constant bundles the generated measure, per-instrument intensity, per-instrument program, per-instrument **register**, and the rhythm pattern name for each instrument. The five styles are `Blues` (12/8), `Bossa Nova`, `Jazz`, `Reggae`, `Rock`.
- The `PRESETS` menu in `MainWindow` sits next to `INSTRUMENT`. Selecting a style writes the measure into `txtMeasure` and calls `applyStyle(style, timeInfo)` on all four configs, so the whole app is reconfigured in one action.
- The styles are deliberately **non-overlapping in pitch**: bass ≤ 40, guitar 43–64 (Bossa starts at 45), keyboard ≥ 65. That is how "guitar outside the bass" and "keyboard outside both" is guaranteed. `StylePresetTest.instrumentRegistersNeverOverlapInAnyStyle` and `bassStaysBelowGuitarAndKeyboardInEveryStyle` enforce it.
- `Register` is a record (`low`, `high`) in `model.music`. `VoicingService` takes it as a parameter, so a style can move an instrument without touching the service. The legacy constants `GUITAR_LOW`/`KEYBOARD_LOW`/etc. still exist as the *reference* regions — under them guitar (43–64) and keyboard (60–84) still touch in 60–64, which is why the style registers are used for anything style-driven.
- `BASS_DERIVED_HIGH` is now `Register.BASS.high()`, and the bass degree helpers (`bassFifth`, `bassOctave`, `bassThird`, `bassSixth`, `bassSeventh`) take the register too. Otherwise a fifth/octave above a root at 40 would land at 47/52 — inside the guitar.
- `BassNoteType` gained `THIRD`, `SIXTH` and `SEVENTH` for the walking blues. `THIRD`/`SIXTH` pick the minor variant when the chord asks for it (so a Cm bass gets Eb, not E); `SEVENTH` is the minor seventh, which is what blues wants.
- Drum styles live in `DrumPattern.applyStylePattern(style, timeInfo)` — one named levada per style, all written relative to `beatsPerMeasure()`/`stepsPerBeat()`. `DrumConfig.applyStyle` is the entry point; `applyDefaultPattern` stays untouched for the pre-style default.
- Applying a style is idempotent (covered by test): repeated application of the same style lands on the same grids and presets.
- Each style produces a distinct combination — `eachStyleIsDistinctFromTheOthers` and `drumPatternIsAdjustedPerStyle` fail if two styles collapse onto the same configuration.

## Rhythm as a pattern (not per-note expression)
For backing tracks each instrument is defined by WHEN and WHAT it plays, not HOW each note is expressed. There is deliberately no pitch bend / vibrato / slide / slap.
- `GuitarRhythmPattern` is a per-step `AttackType` grid: `STRUM_DOWN`, `STRUM_UP`, `PICK`, `NONE`. Presets: `BATIDA_BASICA`, `BATIDA_BALADA`, `BATIDA_ROCK`, `DEDILHADO`, `REGGAE`, `BLUES` (shuffle), `BOSSA_NOVA` (sincopado).
- `BassRhythmPattern` is a per-step `BassNoteType` grid: `NONE`, `ROOT`, `THIRD`, `FIFTH`, `SIXTH`, `SEVENTH`, `OCTAVE`. Presets: `FUNDAMENTAL_SIMPLES`, `FUNDAMENTAL_E_QUINTA`, `CAMINHANTE`, `REGGAE`, `BLUES_SHUFFLE`, `WALKING_BLUES`. The guitar only decides "hit or not"; the bass also decides *which chord degree* — that is the key difference.
- `KeyboardRhythmPattern` is a per-step `AttackType` grid: `NONE`, `BLOCK`, `ARP_UP`, `ARP_DOWN`. Presets: `PAD_SUSTENTADO`, `BLOCO_RITMICO`, `ARPEJO_UP`, `ARPEJO_DOWN`, `BOSSA_NOVA`, `REGGAE` (skank, só contratempo).
- Patterns are independent of the chord, so swapping the progression keeps the groove.
- Both rhythm patterns follow the same safety rules: constructor fills with NONE, `setX` bounds-checks, and presets are written relative to `beatsPerMeasure()`/`stepsPerBeat()` so any meter works.

## Sustained vs. articulated notes
The keyboard's identity is that it can hold a chord. Two rules make that work:
- A BLOCK note is held until the **next attack** (or the measure end), scaled by `noteDurationPercent` — not for one step. Holding a pad for a single 16th step turns it into a blip, and the bug hides whenever the sustain pedal is on.
- The sustain pedal (CC 64) is per measure: on at the downbeat, off near the measure end. The release is always emitted, including in a partial final measure, otherwise the pedal stays stuck on.
- Arpeggio sub-notes must be bounded by `totalTicks` — the loop gate only checks the arpeggio's first note, so later notes can otherwise land past the end of the piece.

## Registers and voice leading
`VoicingService` does NOT transpose each note until it "fits" — that destroys the voicing. It rebuilds the chord inside the instrument region, choosing the inversion with least movement from the previous voicing.
- Preferred regions: bass `E1–G2` (single root), guitar `G2–E4` (closed voicing), keyboard `C4–C6` (spread/drop-2). Drums are channel 9.
- `GuitarMidiService` carries `previousVoicing` measure to measure — that memory is what makes the voice leading work. The bass has no such state: it is monophonic and derives fifth/octave from the root, so there is nothing to conduct. `KeyboardMidiService` also carries `previousVoicing`.
- Register takes priority over voice leading: a voicing is never chosen outside its region.
- Guitar (43–64) and keyboard (60–84) touch in the band 60–64, so they can share a boundary note (e.g. the D4 of G, Bb, Bm). Raising `KEYBOARD_LOW` to remove the overlap would break the keyboard's drop-2 spread, because the dropped middle note would fall below the floor — so the overlap is documented and covered by `guitarAndKeyboardOverlapOnlyInTheBoundaryBand` instead of removed.
- Bass fifth/octave are derived via `VoicingService.bassFifth` / `bassOctave`, never with a naive `root + 7` / `root + 12`. A root at G2 (43) would otherwise put the octave at 55, inside the guitar region. Derived notes are capped at `BASS_DERIVED_HIGH` (50); when neither octave is valid (e.g. root 39), the root itself is repeated rather than leaving the region.
- Because `ChordService` returns pitch classes, the voicing layer is instrument-agnostic.

## Intensity and velocity humanization
Velocity is no longer a raw number in the UI. Each instrument exposes an **intensity** (`Intensity`: `BAIXA` 70%, `MEDIA` 85%, `FORTE` 100%) which scales a per-instrument base velocity, so the relative force between instruments is preserved (drums strongest, keyboard softest). The numeric velocity setters/sliders were removed; the editable field is now an intensity combo.
- The final velocity of every note is produced by `VelocityHumanizer`: random deviation (±8 by default) around the intensity mean, plus a phrasing accent (downbeat +5, chord bass notes stronger than the top voices, kick/snare stronger than hats).
- The mean stays near the chosen level (tests assert the average is within ~8 of the intensity mean), so "humanize" adds variation without changing the overall dynamic.
- `VelocityHumanizer` accepts a seeded `Random` for deterministic tests; tests that need exact accents construct it with `maximumDeviation = 0`.
- Instrument services take the humanizer as an optional constructor argument: `new GuitarMidiService(config, timeInfo, humanizer)`.

## Per-instrument MIDI export
`MidiGenerationService.generate()` writes the full mix (`backing_track.mid`) **and** one independent sequence per enabled instrument, named via the `Instrument` enum:
- `Guitar.midi`, `Bass.midi`, `keyboard.midi`, `drums.midi` (note the lowercase `k`/`d`, as requested).
- `createInstrumentSequences()` is public and side-effect free (like `createSequence()`), returning a `Map<Instrument, Sequence>`; `writeInstrumentFiles(File)` writes them next to the full-mix file.
- Each instrument sequence has its own control track (tempo + time signature) plus exactly one music track, so every file is a valid standalone MIDI. Only enabled instruments are exported.
- The instrument services expose an overload taking an existing `Track` (`generateGuitarTrack(..., Track)` etc.) so the orchestrator can target the right track; the old 4-arg signatures delegate to `sequence.createTrack()` for backwards compatibility.
- Drums are no longer written into a pre-created track — `createSequence()` now creates one track per instrument via `appendInstrumentTrack`.

## MIDI channel allocation
- 0 — harmony (chords)
- 1 — bass
- 2 — guitar
- 3 — keyboard
- 9 — drums (GM percussion)

## Conventions & gotchas
- `MidiGenerationService.createSequence()` is public and side-effect free — use it for tests; `generate()` opens a `JFileChooser` and is interactive.
- MIDI velocity/note values must be 0–127 or `ShortMessage` throws; clamp in config setters.
- `GuitarConfig.syncPatternTo` tracks the applied preset/size, not just the size, because the default is 16 steps which equals 4/4 — a size-only check silently skips applying the preset.
- `BassConfig.applyPreset` sets the preset before syncing, otherwise a simultaneous meter change re-applies the OLD preset via `resizeSteps(..., reapplyPreset=true)`.
- Preset-based grids ignore out-of-range steps and constructors fill with NONE, never `null`.
- The bass's `FIFTH` is always a perfect fifth (+7), so diminished/augmented chords get a non-chord tone. This is an accepted trade-off from the design, not an oversight — revisit if altered chords become common.
- UI strings and comments are in Portuguese; class/method names in English.
- Remaining stubs/placeholders: `DrumMidiConfigWindow` if empty, `model.music.Chord`, `presentation.ChordsSymbols`. All four instrument checkboxes in `MainWindow` are now wired. There is no keyboard option to reuse `VoicingService.forKeyboard` for anything else; the harmony track (channel 0) is a simple block-chord track and is a candidate to be replaced by the keyboard track.
- `gradlew` executable bit and Gradle config-cache are sensitive to the environment; prefer `--no-daemon` in CI/sandbox.
