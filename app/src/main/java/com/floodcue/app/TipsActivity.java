package com.floodcue.app;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

/**
 * Flood safety tips, grouped by priority: before, during and after a flood.
 *
 * All content is bundled in the app, so the screen works fully offline.
 * Life-threatening tips are rendered in the theme's error colour and marked
 * with a warning symbol so they stand out in both light and dark themes.
 */
public class TipsActivity extends FloodCueActivity {

    private TextToSpeech textToSpeech;
    private boolean ttsReady;
    private ImageButton speakingButton;

    private static final class Tip {
        final int text;          // string resource, so the survival language applies
        final boolean warning;

        Tip(int text, boolean warning) {
            this.text = text;
            this.warning = warning;
        }
    }

    private static final class Category {
        final int titleRes;
        final Tip[] tips;

        Category(int titleRes, Tip[] tips) {
            this.titleRes = titleRes;
            this.tips = tips;
        }
    }

    private static final Category[] CATEGORIES = {
            new Category(R.string.tips_category_before, new Tip[]{
                    new Tip(R.string.tip_before_1, false),
                    new Tip(R.string.tip_before_2, false),
                    new Tip(R.string.tip_before_3, false),
                    new Tip(R.string.tip_before_4, false),
                    new Tip(R.string.tip_before_5, false),
                    new Tip(R.string.tip_before_6, true),
                    new Tip(R.string.tip_before_7, false),
                    new Tip(R.string.tip_before_8, false),
                    new Tip(R.string.tip_before_9, false),
                    new Tip(R.string.tip_before_10, false),
                    new Tip(R.string.tip_before_11, false),
            }),

            new Category(R.string.tips_category_during, new Tip[]{
                    new Tip(R.string.tip_during_1, true),
                    new Tip(R.string.tip_during_2, true),
                    new Tip(R.string.tip_during_3, true),
                    new Tip(R.string.tip_during_4, true),
                    new Tip(R.string.tip_during_5, true),
                    new Tip(R.string.tip_during_6, false),
                    new Tip(R.string.tip_during_7, false),
                    new Tip(R.string.tip_during_8, false),
                    new Tip(R.string.tip_during_9, false),
                    new Tip(R.string.tip_during_10, false),
                    new Tip(R.string.tip_during_11, false),
                    new Tip(R.string.tip_during_12, false),
                    new Tip(R.string.tip_during_13, true),
                    new Tip(R.string.tip_during_14, false),
                    new Tip(R.string.tip_during_15, false),
                    new Tip(R.string.tip_during_16, false),
            }),

            new Category(R.string.tips_category_after, new Tip[]{
                    new Tip(R.string.tip_after_1, false),
                    new Tip(R.string.tip_after_2, true),
                    new Tip(R.string.tip_after_3, true),
                    new Tip(R.string.tip_after_4, true),
                    new Tip(R.string.tip_after_5, false),
                    new Tip(R.string.tip_after_6, false),
                    new Tip(R.string.tip_after_7, false),
                    new Tip(R.string.tip_after_8, false),
                    new Tip(R.string.tip_after_9, false),
                    new Tip(R.string.tip_after_10, false),
            }),
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tips);

        textToSpeech = new TextToSpeech(this, status -> {
            ttsReady = status == TextToSpeech.SUCCESS;
            if (ttsReady) {
                Locale selected = Locale.forLanguageTag(
                        LocaleManager.survivalLanguage(this));
                int result = textToSpeech.setLanguage(selected);
                if (result == TextToSpeech.LANG_MISSING_DATA
                        || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    ttsReady = false;
                }
            }
        });

        findViewById(R.id.buttonBackToHomeFromTips).setOnClickListener(v -> finish());

        LinearLayout container = findViewById(R.id.tipsContainer);
        int spacing = dp(12);
        int pad = dp(18);

        for (Category category : CATEGORIES) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.bg_screen_card);
            card.setPadding(pad, pad, pad, pad);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.bottomMargin = spacing;
            card.setLayoutParams(cardParams);

            TextView title = new TextView(this);
            title.setText(category.titleRes);
            title.setTextColor(getColor(R.color.text_primary));
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
            card.addView(title);

            for (Tip tip : category.tips) {
                String tipText = getString(tip.text);
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);

                TextView text = new TextView(this);
                text.setText((tip.warning ? "⚠️ " : "• ") + tipText);
                text.setTextColor(getColor(tip.warning
                        ? R.color.error_color : R.color.text_primary));
                text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                text.setLineSpacing(dp(2), 1f);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                rowParams.topMargin = dp(10);
                row.setLayoutParams(rowParams);

                LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                textParams.rightMargin = dp(8);
                row.addView(text, textParams);

                ImageButton speaker = new ImageButton(this);
                speaker.setImageResource(R.drawable.ic_speaker);
                speaker.setContentDescription(getString(R.string.tips_speak));
                speaker.setBackgroundResource(R.drawable.bg_link_ripple);
                speaker.setPadding(dp(10), dp(10), dp(10), dp(10));
                speaker.setColorFilter(getColor(R.color.button_primary));
                speaker.setMinimumWidth(dp(44));
                speaker.setMinimumHeight(dp(44));
                speaker.setOnClickListener(v -> speakTip(tipText, speaker));
                row.addView(speaker);
                card.addView(row);
            }

            container.addView(card);
        }
    }

    /** The tips are survival information, so they follow the survival language. */
    @Override
    protected boolean usesSurvivalLanguage() {
        return true;
    }

    private void speakTip(String text, ImageButton button) {
        if (!ttsReady || textToSpeech == null) {
            return;
        }
        if (speakingButton != null && speakingButton != button) {
            speakingButton.setAlpha(1f);
        }
        if (textToSpeech.isSpeaking()) {
            textToSpeech.stop();
        }
        speakingButton = button;
        button.setAlpha(0.55f);
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null,
                "tips_" + button.hashCode());
        button.postDelayed(() -> button.setAlpha(1f),
                Math.max(1200, text.length() * 55L));
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
