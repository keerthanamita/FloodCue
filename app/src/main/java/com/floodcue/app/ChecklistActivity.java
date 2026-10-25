package com.floodcue.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * "Before a Flood" preparation checklist.
 *
 * Everything is stored on this phone in SharedPreferences, so the screen works
 * fully offline and no data ever leaves the device.
 *
 * Two layers, as requested:
 *  - Essential: phone readiness, emergency contacts, medicines, go-bag,
 *    offline map and evacuation plan.
 *  - Complete: all six sections, including home protection, family
 *    preparedness and additional supplies.
 */
public class ChecklistActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "floodcue_checklist";
    private static final String KEY_PREFIX = "checklist_";

    private static final class Item {
        final String id;
        final String text;
        final boolean essential;

        Item(String id, String text, boolean essential) {
            this.id = id;
            this.text = text;
            this.essential = essential;
        }
    }

    private static final class Section {
        final int titleRes;
        final int noteRes;          // 0 = no note under the title
        final boolean noteWithCounts; // note is formatted with done/total of this section
        final boolean warningNote;    // note rendered as a safety warning
        final Item[] items;

        Section(int titleRes, int noteRes, boolean noteWithCounts,
                boolean warningNote, Item[] items) {
            this.titleRes = titleRes;
            this.noteRes = noteRes;
            this.noteWithCounts = noteWithCounts;
            this.warningNote = warningNote;
            this.items = items;
        }
    }

    private static final Section[] SECTIONS = {
            new Section(R.string.checklist_section_informed,
                    R.string.checklist_note_informed, false, false, new Item[]{
                    new Item("s1i1", "Check official weather forecasts and flood warnings.", true),
                    new Item("s1i2", "Identify whether your home is in a flood-prone area.", false),
                    new Item("s1i3", "Know the nearest safe shelter and evacuation routes.", true),
                    new Item("s1i4", "Download your area's offline map in FloodCue.", true),
                    new Item("s1i5", "Save important emergency contact numbers.", true),
                    new Item("s1i6", "Learn the local evacuation signals and instructions.", false),
                    new Item("s1i7", "Discuss an emergency communication plan with your family.", false),
            }),

            new Section(R.string.checklist_section_gobag,
                    R.string.checklist_note_gobag, true, false, new Item[]{
                    new Item("s2i1", "Drinking water.", true),
                    new Item("s2i2", "Ready-to-eat, non-perishable food.", true),
                    new Item("s2i3", "First-aid kit.", true),
                    new Item("s2i4", "Required medicines and prescriptions.", true),
                    new Item("s2i5", "Flashlight and spare batteries.", true),
                    new Item("s2i6", "Fully charged power bank and charging cables.", true),
                    new Item("s2i7", "Battery-powered or hand-crank radio, if available.", false),
                    new Item("s2i8", "Extra clothes and a raincoat.", false),
                    new Item("s2i9", "Essential hygiene items and sanitary supplies.", false),
                    new Item("s2i10", "Copies of identification and important documents in a waterproof bag.", false),
                    new Item("s2i11", "Whistle for signalling rescuers.", false),
                    new Item("s2i12", "Cash in small denominations.", false),
                    new Item("s2i13", "Sturdy footwear.", false),
            }),

            new Section(R.string.checklist_section_phone,
                    R.string.checklist_note_phone, false, false, new Item[]{
                    new Item("s3i1", "Charge your phone fully.", true),
                    new Item("s3i2", "Save emergency contacts in your phone.", false),
                    new Item("s3i3", "Add family members' contact details to FloodCue.", false),
                    new Item("s3i4", "Add essential medical information, including allergies and required medicines.", false),
                    new Item("s3i5", "Download the offline map for your area.", false),
                    new Item("s3i6", "Verify that the downloaded map opens without internet.", false),
                    new Item("s3i7", "Learn how to activate the emergency siren.", true),
                    new Item("s3i8", "Understand how to send an SOS message.", true),
                    new Item("s3i9", "Check whether your emergency SMS feature has the necessary permissions.", true),
                    new Item("s3i10", "Carry a charged power bank if available.", false),
            }),

            new Section(R.string.checklist_section_home,
                    R.string.checklist_note_home_safety, false, true, new Item[]{
                    new Item("s4i1", "Move important documents and valuables to a higher location.", false),
                    new Item("s4i2", "Keep electrical appliances away from areas likely to flood.", false),
                    new Item("s4i3", "Know how to switch off electricity and gas safely, if instructed and safe to do so.", false),
                    new Item("s4i4", "Secure outdoor objects that could be swept away.", false),
                    new Item("s4i5", "Move essential supplies above expected flood levels.", false),
                    new Item("s4i6", "Keep doors and evacuation paths clear.", false),
                    new Item("s4i7", "Move vehicles to higher ground if safe and permitted.", false),
                    new Item("s4i8", "Follow official instructions about flood barriers and property protection.", false),
            }),

            new Section(R.string.checklist_section_family,
                    R.string.checklist_note_family, false, false, new Item[]{
                    new Item("s5i1", "Decide on a safe meeting point.", false),
                    new Item("s5i2", "Identify an out-of-area family contact.", false),
                    new Item("s5i3", "Ensure every family member knows how to contact the others.", false),
                    new Item("s5i4", "Prepare essential supplies for infants, older adults and people with disabilities, if applicable.", false),
                    new Item("s5i5", "Arrange necessary mobility aids and medical equipment.", false),
                    new Item("s5i6", "Plan how to evacuate pets, if applicable.", false),
                    new Item("s5i7", "Tell family members where the emergency go-bag is stored.", false),
                    new Item("s5i8", "Explain what to do if family members become separated.", false),
            }),

            new Section(R.string.checklist_section_evac,
                    R.string.checklist_note_evac_critical, false, true, new Item[]{
                    new Item("s6i1", "Follow instructions from local authorities.", true),
                    new Item("s6i2", "Leave early if evacuation is advised.", true),
                    new Item("s6i3", "Take your emergency go-bag.", true),
                    new Item("s6i4", "Take essential medicines and identification documents.", false),
                    new Item("s6i5", "Move to the designated safe shelter using an approved route.", false),
                    new Item("s6i6", "Help children and people who need assistance, when safe.", false),
                    new Item("s6i7", "Inform your designated emergency contact if possible.", false),
                    new Item("s6i8", "Do not walk or drive through floodwater.", false),
                    new Item("s6i9", "Do not wait to finish the checklist before evacuating.", false),
            }),
    };

    /** Live bindings so counts refresh without rebuilding the whole screen. */
    private static final class SectionBinding {
        final Section section;
        final List<Item> visible;
        final TextView countView;
        final TextView noteView;

        SectionBinding(Section section, List<Item> visible,
                       TextView countView, TextView noteView) {
            this.section = section;
            this.visible = visible;
            this.countView = countView;
            this.noteView = noteView;
        }
    }

    private SharedPreferences prefs;
    private LinearLayout container;
    private TextView overallProgress;
    private Button essentialButton;
    private Button completeButton;
    private boolean essentialMode = true;
    private final List<SectionBinding> bindings = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checklist);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        container = findViewById(R.id.checklistContainer);
        overallProgress = findViewById(R.id.checklistOverallProgress);
        essentialButton = findViewById(R.id.buttonChecklistEssential);
        completeButton = findViewById(R.id.buttonChecklistComplete);

        findViewById(R.id.buttonBackToHome).setOnClickListener(v -> finish());
        essentialButton.setOnClickListener(v -> setMode(true));
        completeButton.setOnClickListener(v -> setMode(false));

        if (savedInstanceState != null) {
            essentialMode = savedInstanceState.getBoolean("essential_mode", true);
        }
        setMode(essentialMode);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("essential_mode", essentialMode);
    }

    private void setMode(boolean essential) {
        essentialMode = essential;
        essentialButton.setBackgroundResource(essential
                ? R.drawable.bg_button_primary : R.drawable.bg_button_secondary);
        essentialButton.setTextColor(getColor(essential
                ? R.color.button_primary_text : R.color.text_primary));
        completeButton.setBackgroundResource(essential
                ? R.drawable.bg_button_secondary : R.drawable.bg_button_primary);
        completeButton.setTextColor(getColor(essential
                ? R.color.text_primary : R.color.button_primary_text));
        render();
    }

    private void render() {
        container.removeAllViews();
        bindings.clear();

        for (Section section : SECTIONS) {
            List<Item> visible = new ArrayList<>();
            for (Item item : section.items) {
                if (!essentialMode || item.essential) {
                    visible.add(item);
                }
            }
            if (visible.isEmpty()) {
                continue;
            }
            container.addView(buildSectionCard(section, visible));
        }
        refreshCounts();
    }

    private LinearLayout buildSectionCard(Section section, List<Item> visible) {
        int pad = dp(18);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_screen_card);
        card.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(12);
        card.setLayoutParams(cardParams);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText(section.titleRes);
        title.setTextColor(getColor(R.color.text_primary));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView count = new TextView(this);
        count.setTextColor(getColor(R.color.text_secondary));
        count.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        header.addView(count, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        card.addView(header);

        TextView note = null;
        if (section.noteRes != 0) {
            note = new TextView(this);
            note.setTextColor(getColor(section.warningNote
                    ? R.color.error_color : R.color.text_secondary));
            note.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            noteParams.topMargin = dp(6);
            note.setLayoutParams(noteParams);
            card.addView(note);
        }

        for (Item item : visible) {
            CheckBox box = new CheckBox(this);
            box.setText(item.text);
            box.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            box.setTextColor(getColor(R.color.text_primary));
            box.setGravity(android.view.Gravity.CENTER_VERTICAL);
            box.setMinHeight(dp(48));
            box.setPadding(0, dp(4), 0, dp(4));
            box.setChecked(isDone(item.id));
            final String key = KEY_PREFIX + item.id;
            box.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean(key, isChecked).apply();
                refreshCounts();
            });
            LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            boxParams.topMargin = dp(2);
            box.setLayoutParams(boxParams);
            card.addView(box);
        }

        bindings.add(new SectionBinding(section, visible, count, note));
        return card;
    }

    private void refreshCounts() {
        int total = 0;
        int done = 0;

        for (SectionBinding binding : bindings) {
            int sectionDone = 0;
            for (Item item : binding.visible) {
                if (isDone(item.id)) {
                    sectionDone++;
                }
            }
            total += binding.visible.size();
            done += sectionDone;

            binding.countView.setText(getString(
                    R.string.checklist_section_progress,
                    sectionDone, binding.visible.size()));

            if (binding.noteView != null && binding.section.noteWithCounts) {
                binding.noteView.setText(getString(
                        binding.section.noteRes,
                        sectionDone, binding.visible.size()));
            }
        }

        overallProgress.setText(getString(R.string.checklist_progress, done, total));
    }

    private boolean isDone(String itemId) {
        return prefs.getBoolean(KEY_PREFIX + itemId, false);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
