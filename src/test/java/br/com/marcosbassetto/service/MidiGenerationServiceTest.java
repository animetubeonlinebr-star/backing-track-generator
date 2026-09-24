package br.com.marcosbassetto.service;

import br.com.marcosbassetto.model.bass.BassConfig;
import br.com.marcosbassetto.model.bass.BassRhythmPattern;
import br.com.marcosbassetto.model.drum.DrumConfig;
import br.com.marcosbassetto.model.guitar.GuitarConfig;
import br.com.marcosbassetto.model.guitar.GuitarRhythmPattern;
import br.com.marcosbassetto.model.keyboard.KeyboardConfig;
import br.com.marcosbassetto.model.keyboard.KeyboardRhythmPattern;
import br.com.marcosbassetto.model.music.BackingTrack;
import br.com.marcosbassetto.model.music.TimeSignatureInfo;
import org.junit.jupiter.api.Test;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MidiGenerationServiceTest {

    private static final int CHANNEL_HARMONY = 0;
    private static final int CHANNEL_BASS = 1;
    private static final int CHANNEL_GUITAR = 2;
    private static final int CHANNEL_KEYBOARD = 3;
    private static final int CHANNEL_DRUMS = 9;

    private static BackingTrack backingTrack(String measure) {
        return new BackingTrack("C - Am - Dm - G", "120", measure, "0", "8");
    }

    /** Gera com bateria desligada e baixo/teclado conforme as flags. */
    private static Sequence generate(boolean drums, boolean guitar, boolean bass,
                                     boolean keyboard, BackingTrack backingTrack,
                                     GuitarConfig guitarConfig, BassConfig bassConfig,
                                     KeyboardConfig keyboardConfig) throws Exception {
        MidiGenerationService service = new MidiGenerationService(
                backingTrack, new DrumConfig(backingTrack.getMeasure()),
                guitarConfig, bassConfig, keyboardConfig,
                drums, guitar, bass, keyboard);
        return service.createSequence();
    }

    private static Sequence generate(boolean drums, boolean guitar, boolean bass,
                                     BackingTrack backingTrack, GuitarConfig guitarConfig,
                                     BassConfig bassConfig) throws Exception {
        return generate(drums, guitar, bass, false, backingTrack, guitarConfig, bassConfig, null);
    }

    private static Sequence generate(boolean drums, boolean guitar,
                                     BackingTrack backingTrack, GuitarConfig guitarConfig)
            throws Exception {
        return generate(drums, guitar, false, backingTrack, guitarConfig, null);
    }

    private static Set<Integer> channelsUsed(Sequence sequence) {
        Set<Integer> channels = new HashSet<>();
        for (Track track : sequence.getTracks()) {
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                if (event.getMessage() instanceof ShortMessage sm) {
                    channels.add(sm.getChannel());
                }
            }
        }
        return channels;
    }

    private static long noteOnCount(Sequence sequence, int channel) {
        long count = 0;
        for (Track track : sequence.getTracks()) {
            for (int i = 0; i < track.size(); i++) {
                if (track.get(i).getMessage() instanceof ShortMessage sm
                        && sm.getCommand() == ShortMessage.NOTE_ON
                        && sm.getData2() > 0
                        && sm.getChannel() == channel) {
                    count++;
                }
            }
        }
        return count;
    }

    @Test
    void guitarDisabledProducesNoGuitarChannelEvents() throws Exception {
        GuitarConfig guitarConfig = new GuitarConfig();
        guitarConfig.setPreset(GuitarRhythmPattern.PRESET_BATIDA_ROCK);

        Sequence sequence = generate(true, false, backingTrack("4/4"), guitarConfig);

        assertFalse(channelsUsed(sequence).contains(CHANNEL_GUITAR),
                "sem guitarra habilitada nao deve haver eventos no canal 2");
    }

    @Test
    void guitarEnabledAddsGuitarTrackOnChannel2() throws Exception {
        GuitarConfig guitarConfig = new GuitarConfig();
        guitarConfig.setPreset(GuitarRhythmPattern.PRESET_BATIDA_ROCK);

        Sequence sequence = generate(true, true, backingTrack("4/4"), guitarConfig);

        assertTrue(channelsUsed(sequence).contains(CHANNEL_GUITAR));
        assertTrue(noteOnCount(sequence, CHANNEL_GUITAR) > 0);
    }

    @Test
    void guitarAndDrumsDoNotShareChannels() throws Exception {
        GuitarConfig guitarConfig = new GuitarConfig();
        guitarConfig.setPreset(GuitarRhythmPattern.PRESET_BATIDA_ROCK);

        Sequence sequence = generate(true, true, backingTrack("4/4"), guitarConfig);
        Set<Integer> channels = channelsUsed(sequence);

        assertTrue(channels.contains(CHANNEL_HARMONY), "harmonia no canal 0");
        assertTrue(channels.contains(CHANNEL_GUITAR), "guitarra no canal 2");
        assertTrue(channels.contains(CHANNEL_DRUMS), "bateria no canal 9");
    }

    @Test
    void enablingGuitarAddsExactlyOneTrack() throws Exception {
        GuitarConfig guitarConfig = new GuitarConfig();
        guitarConfig.setPreset(GuitarRhythmPattern.PRESET_BATIDA_ROCK);

        Sequence withoutGuitar = generate(true, false, backingTrack("4/4"), new GuitarConfig());
        Sequence withGuitar = generate(true, true, backingTrack("4/4"), guitarConfig);

        assertEquals(withoutGuitar.getTracks().length + 1, withGuitar.getTracks().length);
    }

    @Test
    void serviceRespectsMeasureChangeBetweenGenerations() throws Exception {
        GuitarConfig guitarConfig = new GuitarConfig();
        guitarConfig.setPreset(GuitarRhythmPattern.PRESET_BATIDA_ROCK);

        generate(true, true, backingTrack("4/4"), guitarConfig);
        Sequence threeFour = generate(true, true, backingTrack("3/4"), guitarConfig);

        assertEquals(12, guitarConfig.getPattern().getTotalSteps(),
                "grid deve acompanhar a mudanca de compasso");
        assertTrue(noteOnCount(threeFour, CHANNEL_GUITAR) > 0);
    }

    @Test
    void nullGuitarConfigStillGeneratesWithoutCrash() throws Exception {
        Sequence sequence = generate(false, true, backingTrack("4/4"), null);
        assertTrue(sequence.getTracks().length > 0);
    }

    @Test
    void bassDisabledProducesNoBassChannelEvents() throws Exception {
        Sequence sequence = generate(true, true, false, backingTrack("4/4"),
                new GuitarConfig(), new BassConfig(TimeSignatureInfo.parse("4/4")));

        assertFalse(channelsUsed(sequence).contains(CHANNEL_BASS),
                "sem baixo habilitado nao deve haver eventos no canal 1");
    }

    @Test
    void bassEnabledAddsTrackOnChannel1() throws Exception {
        Sequence sequence = generate(true, true, true, backingTrack("4/4"),
                new GuitarConfig(), new BassConfig(TimeSignatureInfo.parse("4/4")));

        assertTrue(channelsUsed(sequence).contains(CHANNEL_BASS));
        assertTrue(noteOnCount(sequence, CHANNEL_BASS) > 0);
    }

    @Test
    void allFourInstrumentsUseDistinctChannels() throws Exception {
        Sequence sequence = generate(true, true, true, backingTrack("4/4"),
                new GuitarConfig(), new BassConfig(TimeSignatureInfo.parse("4/4")));
        Set<Integer> channels = channelsUsed(sequence);

        assertTrue(channels.contains(CHANNEL_HARMONY));
        assertTrue(channels.contains(CHANNEL_BASS));
        assertTrue(channels.contains(CHANNEL_GUITAR));
        assertTrue(channels.contains(CHANNEL_DRUMS));
    }

    @Test
    void enablingBassAddsExactlyOneTrack() throws Exception {
        BassConfig bassConfig = new BassConfig(TimeSignatureInfo.parse("4/4"));

        Sequence without = generate(true, false, false, backingTrack("4/4"),
                new GuitarConfig(), null);
        Sequence with = generate(true, false, true, backingTrack("4/4"),
                new GuitarConfig(), bassConfig);

        assertEquals(without.getTracks().length + 1, with.getTracks().length);
    }

    /**
     * Regressão de registro: com todos os instrumentos ligados, o baixo não
     * pode tocar dentro da região da guitarra.
     */
    @Test
    void bassAndGuitarRegistersDoNotOverlapInFullMix() throws Exception {
        Sequence sequence = generate(true, true, true, backingTrack("4/4"),
                new GuitarConfig(), new BassConfig(TimeSignatureInfo.parse("4/4")));

        for (Track track : sequence.getTracks()) {
            for (int i = 0; i < track.size(); i++) {
                if (track.get(i).getMessage() instanceof ShortMessage sm
                        && sm.getCommand() == ShortMessage.NOTE_ON
                        && sm.getData2() > 0
                        && sm.getChannel() == CHANNEL_BASS) {
                    assertTrue(sm.getData1() <= VoicingService.BASS_DERIVED_HIGH,
                            "baixo em " + sm.getData1() + " invade a guitarra");
                }
            }
        }
    }

    @Test
    void bassServiceRespectsMeasureChangeBetweenGenerations() throws Exception {
        BassConfig bassConfig = new BassConfig(TimeSignatureInfo.parse("4/4"));
        bassConfig.applyPreset(BassRhythmPattern.PRESET_CAMINHANTE,
                TimeSignatureInfo.parse("4/4"));

        generate(false, false, true, backingTrack("4/4"), new GuitarConfig(), bassConfig);
        Sequence threeFour = generate(false, false, true, backingTrack("3/4"),
                new GuitarConfig(), bassConfig);

        assertEquals(12, bassConfig.getPattern().getTotalSteps());
        assertTrue(noteOnCount(threeFour, CHANNEL_BASS) > 0);
    }

    @Test
    void keyboardDisabledProducesNoKeyboardChannelEvents() throws Exception {
        Sequence sequence = generate(false, false, false, false, backingTrack("4/4"),
                new GuitarConfig(), null, new KeyboardConfig(TimeSignatureInfo.parse("4/4")));

        assertFalse(channelsUsed(sequence).contains(CHANNEL_KEYBOARD));
    }

    @Test
    void keyboardEnabledAddsTrackOnChannel3() throws Exception {
        Sequence sequence = generate(false, false, false, true, backingTrack("4/4"),
                new GuitarConfig(), null, new KeyboardConfig(TimeSignatureInfo.parse("4/4")));

        assertTrue(channelsUsed(sequence).contains(CHANNEL_KEYBOARD));
        assertTrue(noteOnCount(sequence, CHANNEL_KEYBOARD) > 0);
    }

    @Test
    void allFiveInstrumentsUseDistinctChannels() throws Exception {
        Sequence sequence = generate(true, true, true, true, backingTrack("4/4"),
                new GuitarConfig(), new BassConfig(TimeSignatureInfo.parse("4/4")),
                new KeyboardConfig(TimeSignatureInfo.parse("4/4")));
        Set<Integer> channels = channelsUsed(sequence);

        assertTrue(channels.contains(CHANNEL_HARMONY));
        assertTrue(channels.contains(CHANNEL_BASS));
        assertTrue(channels.contains(CHANNEL_GUITAR));
        assertTrue(channels.contains(CHANNEL_KEYBOARD));
        assertTrue(channels.contains(CHANNEL_DRUMS));
    }

    @Test
    void enablingKeyboardAddsExactlyOneTrack() throws Exception {
        KeyboardConfig keyboardConfig = new KeyboardConfig(TimeSignatureInfo.parse("4/4"));

        Sequence without = generate(false, false, false, false, backingTrack("4/4"),
                new GuitarConfig(), null, null);
        Sequence with = generate(false, false, false, true, backingTrack("4/4"),
                new GuitarConfig(), null, keyboardConfig);

        assertEquals(without.getTracks().length + 1, with.getTracks().length);
    }

    @Test
    void nullKeyboardConfigStillGeneratesWithoutCrash() throws Exception {
        Sequence sequence = generate(false, true, true, true, backingTrack("4/4"),
                new GuitarConfig(), new BassConfig(TimeSignatureInfo.parse("4/4")), null);

        assertTrue(noteOnCount(sequence, CHANNEL_KEYBOARD) > 0);
    }

    /** Regressão: baixo, guitarra e teclado não podem se sobrepor em registro. */
    @Test
    void allInstrumentRegistersStaySeparatedInFullMix() throws Exception {
        Sequence sequence = generate(true, true, true, true, backingTrack("4/4"),
                new GuitarConfig(), new BassConfig(TimeSignatureInfo.parse("4/4")),
                new KeyboardConfig(TimeSignatureInfo.parse("4/4")));

        for (Track track : sequence.getTracks()) {
            for (int i = 0; i < track.size(); i++) {
                if (track.get(i).getMessage() instanceof ShortMessage sm
                        && sm.getCommand() == ShortMessage.NOTE_ON
                        && sm.getData2() > 0) {
                    if (sm.getChannel() == CHANNEL_BASS) {
                        assertTrue(sm.getData1() <= VoicingService.BASS_DERIVED_HIGH,
                                "baixo em " + sm.getData1() + " invade a guitarra");
                    }
                    if (sm.getChannel() == CHANNEL_KEYBOARD) {
                        assertTrue(sm.getData1() >= VoicingService.KEYBOARD_LOW,
                                "teclado em " + sm.getData1() + " abaixo da regiao");
                        assertTrue(sm.getData1() <= VoicingService.KEYBOARD_HIGH,
                                "teclado em " + sm.getData1() + " acima da regiao");
                    }
                }
            }
        }
    }

    @Test
    void keyboardServiceRespectsMeasureChangeBetweenGenerations() throws Exception {
        KeyboardConfig keyboardConfig = new KeyboardConfig(TimeSignatureInfo.parse("4/4"));
        keyboardConfig.applyPreset(KeyboardRhythmPattern.PRESET_ARPEJO_UP,
                TimeSignatureInfo.parse("4/4"));

        generate(false, false, false, true, backingTrack("4/4"),
                new GuitarConfig(), null, keyboardConfig);
        Sequence threeFour = generate(false, false, false, true, backingTrack("3/4"),
                new GuitarConfig(), null, keyboardConfig);

        assertEquals(12, keyboardConfig.getPattern().getTotalSteps());
        assertTrue(noteOnCount(threeFour, CHANNEL_KEYBOARD) > 0);
    }
}