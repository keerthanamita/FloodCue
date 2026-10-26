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
public class ChecklistActivity extends FloodCueActivity {

    private static final String PREFS_NAME = "floodcue_checklist";
    private static final String KEY_PREFIX = "checklist_";

    private static final class Item {
        final String id;
        final int text;          // string resource, so the survival language applies
        final boolean essential;

        Item(String id, int text, boolean essential) {
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
                    new Item("s1i1", R.string.checklist_item_s1i1, true),
                    new Item("s1i2", R.string.checklist_item_s1i2, false),
                    new Item("s1i3", R.string.checklist_item_s1i3, true),
                    new Item("s1i4", R.string.checklist_item_s1i4, true),
                    new Item("s1i5", R.string.checklist_item_s1i5, true),
                    new Item("s1i6", R.string.checklist_item_s1i6, false),
                    new Item("s1i7", R.string.checklist_item_s1i7, false),
            }),

            new Section(R.string.checklist_section_gobag,
                    R.string.checklist_note_gobag, true, false, new Item[]{
                    new Item("s2i1", R.string.checklist_item_s2i1, true),
                    new Item("s2i2", R.string.checklist_item_s2i2, true),
                    new Item("s2i3", R.string.checklist_item_s2i3, true),
                    new Item("s2i4", R.string.checklist_item_s2i4, true),
                    new Item("s2i5", R.string.checklist_item_s2i5, true),
                    new Item("s2i6", R.string.checklist_item_s2i6, true),
                    new Item("s2i7", R.string.checklist_item_s2i7, false),
                    new Item("s2i8", R.string.checklist_item_s2i8, false),
                    new Item("s2i9", R.string.checklist_item_s2i9, false),
                    new Item("s2i10", R.string.checklist_item_s2i10, false),
                    new Item("s2i11", R.string.checklist_item_s2i11, false),
                    new Item("s2i12", R.string.checklist_item_s2i12, false),
                    new Item("s2i13", R.string.checklist_item_s2i13, false),
            }),

            new Section(R.string.checklist_section_phone,
                    R.string.checklist_note_phone, false, false, new Item[]{
                    new Item("s3i1", R.string.checklist_item_s3i1, true),
                    new Item("s3i2", R.string.checklist_item_s3i2, false),
                    new Item("s3i3", R.string.checklist_item_s3i3, false),
                    new Item("s3i4", R.string.checklist_item_s3i4, false),
                    new Item("s3i5", R.string.checklist_item_s3i5, false),
                    new Item("s3i6", R.string.checklist_item_s3i6, false),
                    new Item("s3i7", R.string.checklist_item_s3i7, true),
                    new Item("s3i8", R.string.checklist_item_s3i8, true),
                    new Item("s3i9", R.string.checklist_item_s3i9, true),
                    new Item("s3i10", R.string.checklist_item_s3i10, false),
            }),

            new Section(R.string.checklist_section_home,
                    R.string.checklist_note_home_safety, false, true, new Item[]{
                    new Item("s4i1", R.string.checklist_item_s4i1, false),
                    new Item("s4i2", R.string.checklist_item_s4i2, false),
                    new Item("s4i3", R.string.checklist_item_s4i3, false),
                    new Item("s4i4", R.string.checklist_item_s4i4, false),
                    new Item("s4i5", R.string.checklist_item_s4i5, false),
                    new Item("s4i6", R.string.checklist_item_s4i6, false),
                    new Item("s4i7", R.string.checklist_item_s4i7, false),
                    new Item("s4i8", R.string.checklist_item_s4i8, false),
            }),

            new Section(R.string.checklist_section_family,
                    R.string.checklist_note_family, false, false, new Item[]{
                    new Item("s5i1", R.string.checklist_item_s5i1, false),
                    new Item("s5i2", R.string.checklist_item_s5i2, false),
                    new Item("s5i3", R.string.checklist_item_s5i3, false),
                    new Item("s5i4", R.string.checklist_item_s5i4, false),
                    new Item("s5i5", R.string.checklist_item_s5i5, false),
                    new Item("s5i6", R.string.checklist_item_s5i6, false),
                    new Item("s5i7", R.string.checklist_item_s5i7, false),
                    new Item("s5i8", R.string.checklist_item_s5i8, false),
            }),

            new Section(R.string.checklist_section_evac,
                    R.string.checklist_note_evac_critical, false, true, new Item[]{
                    new Item("s6i1", R.string.checklist_item_s6i1, true),
                    new Item("s6i2", R.string.checklist_item_s6i2, true),
                    new Item("s6i3", R.string.checklist_item_s6i3, true),
                    new Item("s6i4", R.string.checklist_item_s6i4, false),
                    new Item("s6i5", R.string.checklist_item_s6i5, false),
                    new Item("s6i6", R.string.checklist_item_s6i6, false),
                    new Item("s6i7", R.string.checklist_item_s6i7, false),
                    new Item("s6i8", R.string.checklist_item_s6i8, false),
                    new Item("s6i9", R.string.checklist_item_s6i9, false),
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
            box.setText(getString(item.text));
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

    /** The checklist is survival information, so it follows the survival language. */
    @Override
    protected boolean usesSurvivalLanguage() {
        return true;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
