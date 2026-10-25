package com.floodcue.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.Button;
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
public class EmergencyGuideActivity extends AppCompatActivity {

    /** One styled block of content inside a situation detail page. */
    private static final class Block {
        final String header;      // e.g. "What to do", "Never", "During Daylight"
        final boolean numbered;   // render lines as 1., 2., 3. …
        final boolean warning;    // render header and lines as critical warnings
        final String[] lines;

        Block(String header, boolean numbered, boolean warning, String[] lines) {
            this.header = header;
            this.numbered = numbered;
            this.warning = warning;
            this.lines = lines;
        }
    }

    private static final class Situation {
        final String title;
        final String subtitle;
        final boolean showSosButton; // offer the SOS screen at the end of the detail page
        final Block[] blocks;

        Situation(String title, String subtitle, boolean showSosButton, Block[] blocks) {
            this.title = title;
            this.subtitle = subtitle;
            this.showSosButton = showSosButton;
            this.blocks = blocks;
        }
    }

    private static final String WHAT_TO_DO = "What to do";
    private static final String NEVER = "Never";
    private static final String IMPORTANT_WARNINGS = "Important warnings";

    /** Ordered most-urgent first, as required for emergency use. */
    private static final Situation[] SITUATIONS = {
            new Situation("🌊 Floodwater Is Rising Quickly",
                    "Act now — move to higher ground.", false, new Block[]{
                    new Block(WHAT_TO_DO, true, false, new String[]{
                            "Move to higher ground immediately if a safe route is available.",
                            "Follow official evacuation orders without delay.",
                            "Take your emergency go-bag only if it is readily accessible.",
                            "Help children and people who need assistance evacuate early, without putting yourself in danger.",
                            "If your escape route becomes flooded, do not attempt to cross it. Move to a safer elevated location and call for help.",
                            "Watch for further official instructions.",
                    }),
                    new Block(NEVER, false, true, new String[]{
                            "Walk, swim, or drive through floodwater.",
                            "Cross flooded bridges, underpasses, or drains.",
                            "Delay evacuation to collect belongings.",
                    }),
            }),

            new Situation("🏠 I Am Trapped Inside My Home",
                    "Move up, stay visible, call for help.", false, new Block[]{
                    new Block(WHAT_TO_DO, true, false, new String[]{
                            "If floodwater is approaching, evacuate early using a safe route when possible.",
                            "If escape is no longer safe, move to a sturdy, elevated location away from rising water.",
                            "Contact emergency services and share your exact location or GPS coordinates if available.",
                            "Send an SOS message to your emergency contacts if communication is available.",
                            "Signal rescuers using a bright cloth during daylight or a flashlight at night.",
                            "If safe, move away from electrical equipment and flooded rooms.",
                            "Keep children, elderly people, and people with disabilities close and assist them.",
                    }),
                    new Block(IMPORTANT_WARNINGS, false, true, new String[]{
                            "Do not enter fast-moving floodwater to escape.",
                            "Avoid entering enclosed roof spaces where rising water could trap you.",
                            "Access a roof only if it is safe and you have no safer option; remain visible to rescuers.",
                    }),
            }),

            new Situation("🚗 I Am Trapped in a Vehicle",
                    "Leave a sinking vehicle only when it is safe.", false, new Block[]{
                    new Block(WHAT_TO_DO, true, false, new String[]{
                            "Avoid driving into flooded roads, even if the water looks shallow.",
                            "If water begins surrounding your vehicle, assess the safest available escape route immediately.",
                            "If the vehicle is in rising water and escape is possible, leave promptly and move towards safe, higher ground.",
                            "If you cannot escape safely, contact emergency services and communicate your location.",
                            "Signal rescuers using a flashlight, bright cloth, or repeated visible signals when possible.",
                    }),
                    new Block(NEVER, false, true, new String[]{
                            "Assume your vehicle can safely cross floodwater.",
                            "Enter fast-moving water on foot.",
                            "Remain inside a vehicle that is being submerged when a safe escape is possible.",
                    }),
                    new Block("Note", false, false, new String[]{
                            "Vehicle escape depends on the situation. Do not delay escaping a rapidly submerging vehicle to make a phone call.",
                    }),
            }),

            new Situation("⚡ I Am Near Electrical Wires or Equipment",
                    "Assume every fallen wire is live.", false, new Block[]{
                    new Block(WHAT_TO_DO, true, false, new String[]{
                            "Stay away from fallen power lines, electrical poles, exposed wiring, and electrical equipment in water.",
                            "Assume fallen power lines are live.",
                            "Warn others to keep away if you can do so safely.",
                            "Report the hazard to the electricity provider or emergency services.",
                            "If your home is flooded, do not touch electrical switches or appliances while standing in water.",
                            "Have a qualified professional inspect affected electrical systems before using them again.",
                    }),
                    new Block(NEVER, false, true, new String[]{
                            "Walk through water near suspected live electrical equipment.",
                            "Attempt to move a fallen power line.",
                    }),
            }),

            new Situation("🩹 Someone Is Injured",
                    "Check safety first, then stop the bleeding.", false, new Block[]{
                    new Block(WHAT_TO_DO, true, false, new String[]{
                            "Check that the area is safe before approaching the injured person.",
                            "Contact emergency medical services when possible.",
                            "For heavy external bleeding, apply firm, continuous pressure with a clean cloth or dressing.",
                            "Keep the injured person as still and comfortable as possible.",
                            "If the person is unconscious, check whether they are breathing normally and follow emergency dispatcher instructions.",
                            "Do not give food or drink to an unconscious person.",
                            "Avoid moving someone with a suspected serious head, neck, or spinal injury unless staying there presents an immediate danger.",
                            "If the person is in immediate danger from rising water, prioritize getting to safety without exposing yourself to the same danger.",
                    }),
                    new Block("Important", false, true, new String[]{
                            "Do not enter dangerous floodwater to rescue someone without appropriate training and equipment. Call trained rescuers.",
                    }),
            }),

            new Situation("👨‍👩‍👧 My Family Member Is Missing",
                    "Call, coordinate, never search flooded areas alone.", false, new Block[]{
                    new Block(WHAT_TO_DO, true, false, new String[]{
                            "Contact the person using a phone call or SMS if possible.",
                            "Ask family members to share their last known safe location.",
                            "Inform emergency services if the person may be in danger.",
                            "Provide rescuers with the person's name, age, appearance, clothing, medical needs, and last known location.",
                            "Keep one family contact available to receive updates.",
                            "Do not enter flooded areas to search alone.",
                    }),
                    new Block("Family safety tip", false, false, new String[]{
                            "Agree on an emergency meeting point before floods occur, and ensure every family member knows important contact numbers.",
                    }),
            }),

            new Situation("🧒 A Child, Elderly Person, or Person With a Disability Needs Help",
                    "Evacuate early, calmly and together.", false, new Block[]{
                    new Block(WHAT_TO_DO, true, false, new String[]{
                            "Evacuate early when flooding threatens.",
                            "Explain instructions calmly and clearly.",
                            "Carry essential medicines, assistive devices, and identification if immediately accessible.",
                            "Help the person use a safe evacuation route.",
                            "Inform rescuers about mobility limitations, medical conditions, or communication needs.",
                            "Keep family members together when safe to do so.",
                            "Never leave someone who needs assistance in a dangerous location if you can safely help them.",
                    }),
                    new Block("Important", false, true, new String[]{
                            "Do not delay your own evacuation to collect nonessential belongings or equipment.",
                    }),
            }),

            new Situation("📵 My Phone Has No Network",
                    "SMS and emergency calls can still work without data.", false, new Block[]{
                    new Block(WHAT_TO_DO, true, false, new String[]{
                            "Move to a safer elevated location if possible; do not risk entering floodwater to find a signal.",
                            "Try an emergency call if available. Emergency calling availability depends on your region, device, and accessible cellular networks.",
                            "Try sending a short SMS to emergency contacts. It may be delivered if cellular SMS service becomes available.",
                            "If a compatible emergency communication or mesh-network app is already installed and other users are nearby, try it.",
                            "Use a flashlight, whistle, or bright cloth to attract nearby rescuers.",
                            "Conserve phone battery by reducing screen brightness and closing unnecessary apps.",
                            "If you have no working communication method, remain in the safest accessible location and continue signalling when rescuers may be nearby.",
                    }),
                    new Block("Important", false, false, new String[]{
                            "Normal SMS requires cellular service; mobile data is not required. Bluetooth or mesh networking works only when compatible devices and the required network conditions are available.",
                    }),
            }),

            new Situation("🔋 My Phone Battery Is Almost Dead",
                    "Spend your battery only on what can save you.", false, new Block[]{
                    new Block(WHAT_TO_DO, true, false, new String[]{
                            "Send your most important emergency message first, including your location, number of people with you, and immediate danger.",
                            "Make an emergency call if you need urgent rescue.",
                            "Reduce screen brightness and enable battery saver.",
                            "Close unnecessary applications and avoid watching videos or using nonessential features.",
                            "Switch off Bluetooth, Wi-Fi, and other features you do not need, but keep any communication method you are actively using enabled.",
                            "Use a power bank if available.",
                            "Keep your phone protected from water in a suitable waterproof pouch or sealed bag.",
                    }),
                    new Block("Emergency message example", false, false, new String[]{
                            "\u201cI need emergency assistance. My location is [GPS coordinates or landmark]. There are [number] people with me. We are at risk because of flooding. Please send help.\u201d",
                    }),
            }),

            new Situation("🔦 How to Attract Rescuers",
                    "Repeated signals beat one brief attempt.", false, new Block[]{
                    new Block("During Daylight", false, false, new String[]{
                            "• Wave a bright-coloured cloth from a visible, safe location.",
                            "• Use a mirror or another reflective object to flash sunlight towards rescuers.",
                            "• Call out for help when rescuers are nearby.",
                            "• Use a whistle in repeated bursts to attract attention.",
                            "• Display a clearly visible HELP sign if materials are available.",
                    }),
                    new Block("At Night", false, false, new String[]{
                            "• Use a flashlight or phone torch to create repeated visible signals.",
                            "• Use a whistle or call out when rescuers are close enough to hear.",
                            "• Keep yourself in a visible location without moving into dangerous areas.",
                            "• Conserve battery by switching the light off when it is not needed, then signal when you see or hear rescuers.",
                    }),
                    new Block("If You Have No Phone", false, false, new String[]{
                            "• Use a bright cloth, whistle, mirror, or reflective object.",
                            "• Make repeated signals rather than relying on one brief attempt.",
                            "• Stay where rescuers can see you, provided that location remains safe.",
                    }),
                    new Block(NEVER, false, true, new String[]{
                            "Enter floodwater, climb unstable structures, or stand near power lines just to attract attention.",
                            "Light fires or create smoke in enclosed spaces or near flammable materials. Smoke signals are not recommended during floods because of fire and inhalation risks.",
                    }),
            }),

            new Situation("🐍 I See Snakes or Other Animals in Floodwater",
                    "Keep your distance and never handle animals.", false, new Block[]{
                    new Block(WHAT_TO_DO, true, false, new String[]{
                            "Keep a safe distance from snakes and unfamiliar animals.",
                            "Avoid putting your hands or feet into water where you cannot see the ground.",
                            "Do not attempt to catch, handle, or kill animals.",
                            "Move away calmly towards a safer location.",
                            "If bitten by a snake, seek emergency medical help immediately and keep the affected person as still as possible.",
                    }),
                    new Block(NEVER, false, true, new String[]{
                            "Cut the wound, suck out venom, or apply a tourniquet.",
                            "Try to catch the snake for identification.",
                            "Enter floodwater to avoid an animal if doing so creates a greater danger.",
                    }),
            }),

            new Situation("🌧️ I Hear a Flood Warning but Water Has Not Arrived",
                    "Prepare before the water arrives.", false, new Block[]{
                    new Block(WHAT_TO_DO, true, false, new String[]{
                            "Monitor official weather and disaster-management announcements.",
                            "Prepare your emergency bag and charge your phone.",
                            "Identify the nearest safe evacuation destination and route.",
                            "Move essential medicines, documents, and valuables to a safer location if time permits.",
                            "Inform family members about your evacuation plan.",
                            "Evacuate immediately when authorities direct you to do so or when conditions make staying unsafe.",
                    }),
                    new Block(NEVER, false, true, new String[]{
                            "Wait until water reaches your home before preparing to leave.",
                    }),
            }),

            new Situation("🚰 I Have No Safe Drinking Water",
                    "Boil it, treat it, or find another source.", false, new Block[]{
                    new Block(WHAT_TO_DO, true, false, new String[]{
                            "Use sealed bottled water from a reliable source when available.",
                            "Follow official local water-safety advisories.",
                            "If authorities advise boiling and the water is not suspected to contain chemical contamination, bring it to a rolling boil for at least one minute; at elevations above approximately 2,000 metres, boil for three minutes.",
                            "If chemical contamination is suspected, boiling will not make the water safe. Use an alternative safe source.",
                            "Keep drinking water in clean, covered containers.",
                            "Do not drink floodwater or water that may be contaminated by sewage, fuel, or chemicals.",
                    }),
                    new Block("Important", false, true, new String[]{
                            "Water that looks clear is not necessarily safe to drink.",
                    }),
            }),

            new Situation("🍱 My Food May Be Contaminated",
                    "When in doubt, throw it out.", false, new Block[]{
                    new Block(WHAT_TO_DO, true, false, new String[]{
                            "Throw away food that touched floodwater unless it is in packaging officially confirmed safe for recovery.",
                            "Discard food with damaged seals, leaks, unusual smells, or signs of contamination.",
                            "Do not taste food to determine whether it is safe.",
                            "Use safe drinking water for food preparation.",
                            "Wash hands with soap and safe water before eating or handling food.",
                    }),
            }),

            new Situation("🏚️ I Have Returned Home After a Flood",
                    "Return only when authorities say it is safe.", false, new Block[]{
                    new Block(WHAT_TO_DO, true, false, new String[]{
                            "Return only after authorities declare the area safe.",
                            "Watch for structural damage, unstable walls, and contaminated water.",
                            "Stay away from fallen power lines and damaged electrical equipment.",
                            "Wear protective footwear and gloves during cleanup.",
                            "Do not use electrical appliances until the electrical system has been inspected and declared safe.",
                            "Discard food contaminated by floodwater.",
                            "Seek medical attention for injuries, fever, or symptoms following possible exposure to contaminated water.",
                            "Photograph damage for insurance purposes if it is safe to do so.",
                    }),
            }),

            new Situation("🆘 Emergency Contact and SOS Checklist",
                    "What to include in your emergency message.", true, new Block[]{
                    new Block("Include as many of these details as possible", false, false, new String[]{
                            "• Your name.",
                            "• Your GPS coordinates or a recognisable nearby landmark.",
                            "• The number of people needing help.",
                            "• Whether anyone is injured or needs urgent medical assistance.",
                            "• Whether water is rising or the location is unsafe.",
                            "• A safe way for rescuers or family members to contact you.",
                    }),
                    new Block("Example", false, false, new String[]{
                            "\u201cEMERGENCY — We need rescue assistance. Location: [coordinates/landmark]. People: [number]. Injuries: [details]. Water level: [rising/stable]. Contact: [phone number].\u201d",
                    }),
                    new Block("Note", false, false, new String[]{
                            "If GPS coordinates are unavailable, share the nearest road, building, landmark, or known location.",
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
            title.setText(situation.title);
            title.setTextColor(getColor(R.color.text_primary));
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
            card.addView(title);

            TextView subtitle = new TextView(this);
            subtitle.setText(situation.subtitle);
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
        title.setText(situation.title);
        title.setTextColor(getColor(R.color.text_primary));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        detailContainer.addView(title);

        for (Block block : situation.blocks) {
            TextView header = new TextView(this);
            header.setText(block.header);
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
                String line = block.lines[i];
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
