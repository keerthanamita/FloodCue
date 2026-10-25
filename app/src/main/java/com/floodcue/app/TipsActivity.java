package com.floodcue.app;

import android.os.Bundle;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Flood safety tips, grouped by priority: before, during and after a flood.
 *
 * All content is bundled in the app, so the screen works fully offline.
 * Life-threatening tips are rendered in the theme's error colour and marked
 * with a warning symbol so they stand out in both light and dark themes.
 */
public class TipsActivity extends AppCompatActivity {

    private static final class Tip {
        final String text;
        final boolean warning;

        Tip(String text, boolean warning) {
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
                    new Tip("Know your risk: find evacuation routes and high-ground shelters near you.", false),
                    new Tip("Pack a go-bag: flashlight, spare batteries, first-aid kit, medicines, drinking water, dry food and a power bank.", false),
                    new Tip("Keep IDs, insurance cards, emergency contacts and important documents in a waterproof bag.", false),
                    new Tip("Keep phones and power banks charged, and store emergency food and clean drinking water.", false),
                    new Tip("Move valuables, electronics, vehicles and pets to higher ground before water rises.", false),
                    new Tip("Learn how to switch off electricity and gas safely — only ever do it when it is safe to do so.", true),
                    new Tip("Arrange early evacuation help for children, elderly people, pregnant women and people with disabilities.", false),
                    new Tip("Keep a battery or hand-crank radio and learn your local warning signals.", false),
                    new Tip("Agree one family meeting point and one out-of-area contact everyone can reach.", false),
                    new Tip("Save FloodCue's emergency numbers, and test the siren and SOS screens once before the monsoon.", false),
                    new Tip("Keep the vehicle fuelled during flood season — you may need to leave at short notice.", false),
            }),

            new Category(R.string.tips_category_during, new Tip[]{
                    new Tip("Move to higher ground immediately — do not wait to see how high the water will rise.", true),
                    new Tip("Never walk, swim or drive through floodwater. Avoid flooded roads, bridges and underpasses.", true),
                    new Tip("Avoid touching electrical equipment, sockets or power lines that are near water.", true),
                    new Tip("Moving water is stronger than it looks: 15 cm can knock you off your feet and about 60 cm can float a car.", true),
                    new Tip("Keep children away from floodwater and fast-flowing drains.", true),
                    new Tip("Follow official evacuation instructions and avoid contact with contaminated floodwater.", false),
                    new Tip("Drink only safe water and avoid food contaminated by floodwater.", false),
                    new Tip("If trapped, move to a safe elevated place and contact emergency services when possible.", false),
                    new Tip("Signal for rescue: wave a bright cloth by day; at night flash a flashlight in repeated SOS patterns or use a phone torch.", false),
                    new Tip("Use a whistle, mirror or other visible signal to attract rescuers — they spot these before they spot you.", false),
                    new Tip("No phone signal? Keep retrying emergency calls when possible and signal nearby rescuers without putting yourself at risk.", false),
                    new Tip("Do not light fires or create smoke indoors or near flammable materials. Use smoke signals only when it is safe and cannot start a fire.", false),
                    new Tip("Turn off electricity and gas at the main before water reaches sockets, only if it is safe to do so.", true),
                    new Tip("Keep your phone dry and save its battery — you may need it to call for help.", false),
                    new Tip("Go to the highest floor; move onto the roof only as a last resort.", false),
                    new Tip("Listen to official alerts on a radio if the power and internet are out.", false),
            }),

            new Category(R.string.tips_category_after, new Tip[]{
                    new Tip("Return home only when authorities declare it safe.", false),
                    new Tip("Stay away from damaged buildings, fallen power lines and electrical equipment exposed to water.", true),
                    new Tip("Do not switch on any electrical appliance until it has been inspected and declared safe.", true),
                    new Tip("Discard food contaminated by floodwater and drink only safe, treated water.", true),
                    new Tip("Wear protective footwear and gloves while cleaning up.", false),
                    new Tip("Photograph property damage for insurance if it is safe, and seek medical help for injuries or contaminated-water exposure.", false),
                    new Tip("Wash your hands and any cuts thoroughly after contact with floodwater.", false),
                    new Tip("Dry and ventilate buildings quickly to stop mould from growing.", false),
                    new Tip("Boil or treat drinking water until the supply is declared safe.", false),
                    new Tip("Watch for snakes, insects or animals brought in by the floodwater.", false),
            }),
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tips);

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
                TextView row = new TextView(this);
                row.setText((tip.warning ? "⚠️ " : "• ") + tip.text);
                row.setTextColor(getColor(tip.warning
                        ? R.color.error_color : R.color.text_primary));
                row.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                row.setLineSpacing(dp(2), 1f);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                rowParams.topMargin = dp(10);
                row.setLayoutParams(rowParams);
                card.addView(row);
            }

            container.addView(card);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
