package com.floodcue.app;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

/** Small lifecycle-safe TTS wrapper for survival-content screens. */
final class TtsSpeaker {
    private final TextToSpeech engine;
    private boolean ready;

    TtsSpeaker(Context context) {
        final TextToSpeech[] holder = new TextToSpeech[1];
        holder[0] = new TextToSpeech(context, status -> {
            ready = status == TextToSpeech.SUCCESS;
            if (ready) {
                Locale locale = Locale.forLanguageTag(
                        LocaleManager.survivalLanguage(context));
                int result = holder[0].setLanguage(locale);
                ready = result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED;
            }
        });
        engine = holder[0];
    }

    void speak(String text) {
        if (ready) {
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null,
                    "floodcue_" + text.hashCode());
        }
    }

    void shutdown() {
        engine.stop();
        engine.shutdown();
    }
}
