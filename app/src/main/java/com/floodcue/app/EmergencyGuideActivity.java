package com.floodcue.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;

/**
 * Emergency Survival Guide: situation-based instructions for floods.
 *
 * The most urgent situations are listed first, warnings are highlighted in
 * the theme's error colour, and all content is bundled so the guide works
 * fully offline. FloodCue only repeats general public-safety guidance —
 * official instructions from emergency services always come first.
 */
public class EmergencyGuideActivity extends FloodCueActivity {

    private TtsSpeaker ttsSpeaker;

    /** One styled block of content inside a situation detail page. */
    private static final class Block {
        final int header;        // string resource, so the survival language applies
        final boolean numbered;   // render lines as 1., 2., 3. …
        final boolean warning;    // render header and lines as critical warnings
        final int[] lines;

        Block(int header, boolean numbered, boolean warning, int[] lines) {
            this.header = header;
            this.numbered = numbered;
            this.warning = warning;
            this.lines = lines;
        }
    }

    private static final class Situation {
        final int title;
        final int subtitle;
        final boolean showSosButton; // offer the SOS screen at the end of the detail page
        final Block[] blocks;

        Situation(int title, int subtitle, boolean showSosButton, Block[] blocks) {
            this.title = title;
            this.subtitle = subtitle;
            this.showSosButton = showSosButton;
            this.blocks = blocks;
        }
    }

    /** Ordered most-urgent first, as required for emergency use. */
    private static final Situation[] SITUATIONS = {
            new Situation(R.string.guide_situation_1_title,
                    R.string.guide_situation_1_subtitle, false, new Block[]{
                    new Block(R.string.guide_header_what_to_do, true, false, new int[]{
                            R.string.guide_situation_1_block_1_line_1,
                            R.string.guide_situation_1_block_1_line_2,
                            R.string.guide_situation_1_block_1_line_3,
                            R.string.guide_situation_1_block_1_line_4,
                            R.string.guide_situation_1_block_1_line_5,
                            R.string.guide_situation_1_block_1_line_6,
                    }),
                    new Block(R.string.guide_header_never, false, true, new int[]{
                            R.string.guide_situation_1_block_2_line_1,
                            R.string.guide_situation_1_block_2_line_2,
                            R.string.guide_situation_1_block_2_line_3,
                    }),
            }),

            new Situation(R.string.guide_situation_2_title,
                    R.string.guide_situation_2_subtitle, false, new Block[]{
                    new Block(R.string.guide_header_what_to_do, true, false, new int[]{
                            R.string.guide_situation_2_block_1_line_1,
                            R.string.guide_situation_2_block_1_line_2,
                            R.string.guide_situation_2_block_1_line_3,
                            R.string.guide_situation_2_block_1_line_4,
                            R.string.guide_situation_2_block_1_line_5,
                            R.string.guide_situation_2_block_1_line_6,
                            R.string.guide_situation_2_block_1_line_7,
                    }),
                    new Block(R.string.guide_header_important_warnings, false, true, new int[]{
                            R.string.guide_situation_2_block_2_line_1,
                            R.string.guide_situation_2_block_2_line_2,
                            R.string.guide_situation_2_block_2_line_3,
                    }),
            }),

            new Situation(R.string.guide_situation_3_title,
                    R.string.guide_situation_3_subtitle, false, new Block[]{
                    new Block(R.string.guide_header_what_to_do, true, false, new int[]{
                            R.string.guide_situation_3_block_1_line_1,
                            R.string.guide_situation_3_block_1_line_2,
                            R.string.guide_situation_3_block_1_line_3,
                            R.string.guide_situation_3_block_1_line_4,
                            R.string.guide_situation_3_block_1_line_5,
                    }),
                    new Block(R.string.guide_header_never, false, true, new int[]{
                            R.string.guide_situation_3_block_2_line_1,
                            R.string.guide_situation_3_block_2_line_2,
                            R.string.guide_situation_3_block_2_line_3,
                    }),
                    new Block(R.string.guide_situation_3_block_3_header, false, false, new int[]{
                            R.string.guide_situation_3_block_3_line_1,
                    }),
            }),

            new Situation(R.string.guide_situation_4_title,
                    R.string.guide_situation_4_subtitle, false, new Block[]{
                    new Block(R.string.guide_header_what_to_do, true, false, new int[]{
                            R.string.guide_situation_4_block_1_line_1,
                            R.string.guide_situation_4_block_1_line_2,
                            R.string.guide_situation_4_block_1_line_3,
                            R.string.guide_situation_4_block_1_line_4,
                            R.string.guide_situation_4_block_1_line_5,
                            R.string.guide_situation_4_block_1_line_6,
                    }),
                    new Block(R.string.guide_header_never, false, true, new int[]{
                            R.string.guide_situation_4_block_2_line_1,
                            R.string.guide_situation_4_block_2_line_2,
                    }),
            }),

            new Situation(R.string.guide_situation_5_title,
                    R.string.guide_situation_5_subtitle, false, new Block[]{
                    new Block(R.string.guide_header_what_to_do, true, false, new int[]{
                            R.string.guide_situation_5_block_1_line_1,
                            R.string.guide_situation_5_block_1_line_2,
                            R.string.guide_situation_5_block_1_line_3,
                            R.string.guide_situation_5_block_1_line_4,
                            R.string.guide_situation_5_block_1_line_5,
                            R.string.guide_situation_5_block_1_line_6,
                            R.string.guide_situation_5_block_1_line_7,
                            R.string.guide_situation_5_block_1_line_8,
                    }),
                    new Block(R.string.guide_situation_5_block_2_header, false, true, new int[]{
                            R.string.guide_situation_5_block_2_line_1,
                    }),
            }),

            new Situation(R.string.guide_situation_6_title,
                    R.string.guide_situation_6_subtitle, false, new Block[]{
                    new Block(R.string.guide_header_what_to_do, true, false, new int[]{
                            R.string.guide_situation_6_block_1_line_1,
                            R.string.guide_situation_6_block_1_line_2,
                            R.string.guide_situation_6_block_1_line_3,
                            R.string.guide_situation_6_block_1_line_4,
                            R.string.guide_situation_6_block_1_line_5,
                            R.string.guide_situation_6_block_1_line_6,
                    }),
                    new Block(R.string.guide_situation_6_block_2_header, false, false, new int[]{
                            R.string.guide_situation_6_block_2_line_1,
                    }),
            }),

            new Situation(R.string.guide_situation_7_title,
                    R.string.guide_situation_7_subtitle, false, new Block[]{
                    new Block(R.string.guide_header_what_to_do, true, false, new int[]{
                            R.string.guide_situation_7_block_1_line_1,
                            R.string.guide_situation_7_block_1_line_2,
                            R.string.guide_situation_7_block_1_line_3,
                            R.string.guide_situation_7_block_1_line_4,
                            R.string.guide_situation_7_block_1_line_5,
                            R.string.guide_situation_7_block_1_line_6,
                            R.string.guide_situation_7_block_1_line_7,
                    }),
                    new Block(R.string.guide_situation_7_block_2_header, false, true, new int[]{
                            R.string.guide_situation_7_block_2_line_1,
                    }),
            }),

            new Situation(R.string.guide_situation_8_title,
                    R.string.guide_situation_8_subtitle, false, new Block[]{
                    new Block(R.string.guide_header_what_to_do, true, false, new int[]{
                            R.string.guide_situation_8_block_1_line_1,
                            R.string.guide_situation_8_block_1_line_2,
                            R.string.guide_situation_8_block_1_line_3,
                            R.string.guide_situation_8_block_1_line_4,
                            R.string.guide_situation_8_block_1_line_5,
                            R.string.guide_situation_8_block_1_line_6,
                            R.string.guide_situation_8_block_1_line_7,
                    }),
                    new Block(R.string.guide_situation_8_block_2_header, false, false, new int[]{
                            R.string.guide_situation_8_block_2_line_1,
                    }),
            }),

            new Situation(R.string.guide_situation_9_title,
                    R.string.guide_situation_9_subtitle, false, new Block[]{
                    new Block(R.string.guide_header_what_to_do, true, false, new int[]{
                            R.string.guide_situation_9_block_1_line_1,
                            R.string.guide_situation_9_block_1_line_2,
                            R.string.guide_situation_9_block_1_line_3,
                            R.string.guide_situation_9_block_1_line_4,
                            R.string.guide_situation_9_block_1_line_5,
                            R.string.guide_situation_9_block_1_line_6,
                            R.string.guide_situation_9_block_1_line_7,
                    }),
                    new Block(R.string.guide_situation_9_block_2_header, false, false, new int[]{
                            R.string.guide_situation_9_block_2_line_1,
                    }),
            }),

            new Situation(R.string.guide_situation_10_title,
                    R.string.guide_situation_10_subtitle, false, new Block[]{
                    new Block(R.string.guide_situation_10_block_1_header, false, false, new int[]{
                            R.string.guide_situation_10_block_1_line_1,
                            R.string.guide_situation_10_block_1_line_2,
                            R.string.guide_situation_10_block_1_line_3,
                            R.string.guide_situation_10_block_1_line_4,
                            R.string.guide_situation_10_block_1_line_5,
                    }),
                    new Block(R.string.guide_situation_10_block_2_header, false, false, new int[]{
                            R.string.guide_situation_10_block_2_line_1,
                            R.string.guide_situation_10_block_2_line_2,
                            R.string.guide_situation_10_block_2_line_3,
                            R.string.guide_situation_10_block_2_line_4,
                    }),
                    new Block(R.string.guide_situation_10_block_3_header, false, false, new int[]{
                            R.string.guide_situation_10_block_3_line_1,
                            R.string.guide_situation_10_block_3_line_2,
                            R.string.guide_situation_10_block_3_line_3,
                    }),
                    new Block(R.string.guide_header_never, false, true, new int[]{
                            R.string.guide_situation_10_block_4_line_1,
                            R.string.guide_situation_10_block_4_line_2,
                    }),
            }),

            new Situation(R.string.guide_situation_11_title,
                    R.string.guide_situation_11_subtitle, false, new Block[]{
                    new Block(R.string.guide_header_what_to_do, true, false, new int[]{
                            R.string.guide_situation_11_block_1_line_1,
                            R.string.guide_situation_11_block_1_line_2,
                            R.string.guide_situation_11_block_1_line_3,
                            R.string.guide_situation_11_block_1_line_4,
                            R.string.guide_situation_11_block_1_line_5,
                    }),
                    new Block(R.string.guide_header_never, false, true, new int[]{
                            R.string.guide_situation_11_block_2_line_1,
                            R.string.guide_situation_11_block_2_line_2,
                            R.string.guide_situation_11_block_2_line_3,
                    }),
            }),

            new Situation(R.string.guide_situation_12_title,
                    R.string.guide_situation_12_subtitle, false, new Block[]{
                    new Block(R.string.guide_header_what_to_do, true, false, new int[]{
                            R.string.guide_situation_12_block_1_line_1,
                            R.string.guide_situation_12_block_1_line_2,
                            R.string.guide_situation_12_block_1_line_3,
                            R.string.guide_situation_12_block_1_line_4,
                            R.string.guide_situation_12_block_1_line_5,
                            R.string.guide_situation_12_block_1_line_6,
                    }),
                    new Block(R.string.guide_header_never, false, true, new int[]{
                            R.string.guide_situation_12_block_2_line_1,
                    }),
            }),

            new Situation(R.string.guide_situation_13_title,
                    R.string.guide_situation_13_subtitle, false, new Block[]{
                    new Block(R.string.guide_header_what_to_do, true, false, new int[]{
                            R.string.guide_situation_13_block_1_line_1,
                            R.string.guide_situation_13_block_1_line_2,
                            R.string.guide_situation_13_block_1_line_3,
                            R.string.guide_situation_13_block_1_line_4,
                            R.string.guide_situation_13_block_1_line_5,
                            R.string.guide_situation_13_block_1_line_6,
                    }),
                    new Block(R.string.guide_situation_13_block_2_header, false, true, new int[]{
                            R.string.guide_situation_13_block_2_line_1,
                    }),
            }),

            new Situation(R.string.guide_situation_14_title,
                    R.string.guide_situation_14_subtitle, false, new Block[]{
                    new Block(R.string.guide_header_what_to_do, true, false, new int[]{
                            R.string.guide_situation_14_block_1_line_1,
                            R.string.guide_situation_14_block_1_line_2,
                            R.string.guide_situation_14_block_1_line_3,
                            R.string.guide_situation_14_block_1_line_4,
                            R.string.guide_situation_14_block_1_line_5,
                    }),
            }),

            new Situation(R.string.guide_situation_15_title,
                    R.string.guide_situation_15_subtitle, false, new Block[]{
                    new Block(R.string.guide_header_what_to_do, true, false, new int[]{
                            R.string.guide_situation_15_block_1_line_1,
                            R.string.guide_situation_15_block_1_line_2,
                            R.string.guide_situation_15_block_1_line_3,
                            R.string.guide_situation_15_block_1_line_4,
                            R.string.guide_situation_15_block_1_line_5,
                            R.string.guide_situation_15_block_1_line_6,
                            R.string.guide_situation_15_block_1_line_7,
                            R.string.guide_situation_15_block_1_line_8,
                    }),
            }),

            new Situation(R.string.guide_situation_16_title,
                    R.string.guide_situation_16_subtitle, true, new Block[]{
                    new Block(R.string.guide_situation_16_block_1_header, false, false, new int[]{
                            R.string.guide_situation_16_block_1_line_1,
                            R.string.guide_situation_16_block_1_line_2,
                            R.string.guide_situation_16_block_1_line_3,
                            R.string.guide_situation_16_block_1_line_4,
                            R.string.guide_situation_16_block_1_line_5,
                            R.string.guide_situation_16_block_1_line_6,
                    }),
                    new Block(R.string.guide_situation_16_block_2_header, false, false, new int[]{
                            R.string.guide_situation_16_block_2_line_1,
                    }),
                    new Block(R.string.guide_situation_16_block_3_header, false, false, new int[]{
                            R.string.guide_situation_16_block_3_line_1,
                    }),
            }),
    };

    private ScrollView listScroll;
    private ScrollView detailScroll;
    private LinearLayout detailContainer;
    private TextView backButton;
    private boolean showingDetail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_guide);
        ttsSpeaker = new TtsSpeaker(this);

        listScroll = findViewById(R.id.guideListScroll);
        detailScroll = findViewById(R.id.guideDetailScroll);
        detailContainer = findViewById(R.id.guideDetailContainer);
        backButton = findViewById(R.id.buttonBackFromGuide);

        backButton.setOnClickListener(v -> {
            if (showingDetail) {
                showList();
            } else {
                finish();
            }
        });

        findViewById(R.id.buttonGuideSos).setOnClickListener(v ->
                startActivity(new Intent(this, SosActivity.class)));

        buildCards();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (showingDetail) {
                    showList();
                } else {
                    finish();
                }
            }
        });

        if (savedInstanceState != null && savedInstanceState.getBoolean("showing_detail")) {
            int index = savedInstanceState.getInt("detail_index", -1);
            if (index >= 0 && index < SITUATIONS.length) {
                showSituation(SITUATIONS[index]);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("showing_detail", showingDetail);
        if (showingDetail) {
            outState.putInt("detail_index", currentSituationIndex);
        }
    }

    private int currentSituationIndex = -1;

    private void buildCards() {
        LinearLayout container = findViewById(R.id.guideCardsContainer);
        int pad = dp(16);

        for (int i = 0; i < SITUATIONS.length; i++) {
            final Situation situation = SITUATIONS[i];
            final int index = i;

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.bg_screen_card);
            card.setPadding(pad, pad, pad, pad);
            card.setClickable(true);
            card.setFocusable(true);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.topMargin = dp(8);
            card.setLayoutParams(cardParams);

            TextView title = new TextView(this);
            title.setText(getString(situation.title));
            title.setTextColor(getColor(R.color.text_primary));
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
            card.addView(title);

            TextView subtitle = new TextView(this);
            subtitle.setText(getString(situation.subtitle));
            subtitle.setTextColor(getColor(R.color.text_secondary));
            subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            subtitleParams.topMargin = dp(4);
            subtitle.setLayoutParams(subtitleParams);
            card.addView(subtitle);

            card.setOnClickListener(v -> showSituationAt(index));
            container.addView(card);
        }
    }

    private void showSituationAt(int index) {
        currentSituationIndex = index;
        showSituation(SITUATIONS[index]);
    }

    private void showSituation(Situation situation) {
        detailContainer.removeAllViews();
        showingDetail = true;
        currentSituationIndex = java.util.Arrays.asList(SITUATIONS).indexOf(situation);

        TextView title = new TextView(this);
        title.setText(getString(situation.title));
        title.setTextColor(getColor(R.color.text_primary));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        detailContainer.addView(title);

        for (Block block : situation.blocks) {
            TextView header = new TextView(this);
            header.setText(getString(block.header));
            header.setTextColor(getColor(block.warning
                    ? R.color.error_color : R.color.text_secondary));
            header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            header.setTypeface(header.getTypeface(), android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            headerParams.topMargin = dp(18);
            header.setLayoutParams(headerParams);
            detailContainer.addView(header);

            for (int i = 0; i < block.lines.length; i++) {
                String line = getString(block.lines[i]);
                TextView row = new TextView(this);
                row.setText(block.numbered ? (i + 1) + ". " + line : line);
                row.setTextColor(getColor(block.warning
                        ? R.color.error_color : R.color.text_primary));
                row.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                row.setLineSpacing(dp(2), 1f);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                rowParams.topMargin = dp(8);
                row.setLayoutParams(rowParams);
                detailContainer.addView(row);

                ImageButton speaker = new ImageButton(this);
                speaker.setImageResource(R.drawable.ic_speaker);
                speaker.setContentDescription(getString(R.string.tips_speak));
                speaker.setBackgroundResource(R.drawable.bg_link_ripple);
                speaker.setPadding(dp(10), dp(6), dp(10), dp(6));
                speaker.setColorFilter(getColor(R.color.button_primary));
                speaker.setOnClickListener(v -> ttsSpeaker.speak(line));
                LinearLayout.LayoutParams speakerParams = new LinearLayout.LayoutParams(
                        dp(44), dp(44));
                speakerParams.gravity = android.view.Gravity.END;
                detailContainer.addView(speaker, speakerParams);
            }
        }

        if (situation.showSosButton) {
            Button sosButton = new Button(this);
            sosButton.setText(R.string.guide_sos_button);
            sosButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            sosButton.setAllCaps(false);
            sosButton.setTextColor(getColor(R.color.button_primary_text));
            sosButton.setBackgroundResource(R.drawable.bg_button_primary);
            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
            buttonParams.topMargin = dp(24);
            sosButton.setLayoutParams(buttonParams);
            sosButton.setOnClickListener(v ->
                    startActivity(new Intent(this, SosActivity.class)));
            detailContainer.addView(sosButton);
        }

        backButton.setText(R.string.guide_back_to_list);
        listScroll.setVisibility(android.view.View.GONE);
        detailScroll.setVisibility(android.view.View.VISIBLE);
        detailScroll.scrollTo(0, 0);
    }

    private void showList() {
        showingDetail = false;
        currentSituationIndex = -1;
        backButton.setText(R.string.guide_back_title);
        detailScroll.setVisibility(android.view.View.GONE);
        listScroll.setVisibility(android.view.View.VISIBLE);
        listScroll.scrollTo(0, 0);
    }

    /** The guide is survival information, so it follows the survival language. */
    @Override
    protected boolean usesSurvivalLanguage() {
        return true;
    }

    @Override
    protected void onDestroy() {
        if (ttsSpeaker != null) {
            ttsSpeaker.shutdown();
        }
        super.onDestroy();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
