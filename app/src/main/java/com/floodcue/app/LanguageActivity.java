
package com.floodcue.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class LanguageActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "FloodCuePrefs";

    private TextView selectedAppLanguage;
    private TextView selectedSurvivalLanguage;
    private MaterialButton buttonLanguageNext;
    private SharedPreferences preferences;

    private String appCode = "en";
    private String survivalCode = "en";
    private boolean editMode;

    private final String[] names = {
            "English", "Telugu", "Hindi",
            "Tamil", "Kannada", "Malayalam"
    };

    private final String[] nativeNames = {
            "English", "తెలుగు", "हिन्दी",
            "தமிழ்", "ಕನ್ನಡ", "മലയാളം"
    };

    private final String[] codes = {
            "en", "te", "hi", "ta", "kn", "ml"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language);

        preferences = getSharedPreferences(
                PREFS_NAME, MODE_PRIVATE
        );

        selectedAppLanguage =
                findViewById(R.id.selectedAppLanguage);

        selectedSurvivalLanguage =
                findViewById(R.id.selectedSurvivalLanguage);

        buttonLanguageNext =
                findViewById(R.id.buttonLanguageNext);

        editMode = getIntent().getBooleanExtra(
                "LANGUAGE_EDIT_MODE", false
        );

        appCode = preferences.getString("app_language", "en");
        survivalCode = preferences.getString(
                "survival_language", "en"
        );

        updateLabels();

        selectedAppLanguage.setOnClickListener(
                v -> showLanguagePicker(true)
        );

        selectedSurvivalLanguage.setOnClickListener(
                v -> showLanguagePicker(false)
        );

        buttonLanguageNext.setOnClickListener(
                v -> saveAndContinue()
        );
    }

    private void updateLabels() {
        selectedAppLanguage.setText(displayName(appCode));
        selectedSurvivalLanguage.setText(
                displayName(survivalCode)
        );
    }

    private String displayName(String code) {
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equals(code)) {
                return names[i] + "  ·  " + nativeNames[i];
            }
        }
        return "English";
    }

    private void showLanguagePicker(boolean appLanguage) {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(20), dp(20), dp(12));

        TextView heading = new TextView(this);
        heading.setText(appLanguage
                ? "Choose app language"
                : "Choose safety language");
        heading.setTextSize(21);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setTextColor(getColor(R.color.text_primary));

        content.addView(heading);

        TextView subtitle = new TextView(this);
        subtitle.setText("Search and select a language");
        subtitle.setTextSize(14);
        subtitle.setTextColor(getColor(R.color.text_secondary));

        LinearLayout.LayoutParams subtitleParams =
                new LinearLayout.LayoutParams(
                        -1, -2
                );
        subtitleParams.topMargin = dp(5);
        content.addView(subtitle, subtitleParams);

        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("Search language...");
        search.setTextSize(15);
        search.setPadding(dp(14), 0, dp(14), 0);
        search.setBackgroundResource(R.drawable.bg_input);

        LinearLayout.LayoutParams searchParams =
                new LinearLayout.LayoutParams(-1, dp(50));
        searchParams.topMargin = dp(16);
        content.addView(search, searchParams);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, dp(12), 0, dp(4));

        scrollView.addView(list);

        LinearLayout.LayoutParams listParams =
                new LinearLayout.LayoutParams(-1, dp(300));
        content.addView(scrollView, listParams);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(content)
                .create();

        String selectedCode = appLanguage ? appCode : survivalCode;

        populateLanguages(
                list, "", selectedCode, appLanguage, dialog, search
        );

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count) {
                populateLanguages(
                        list, s.toString(),
                        appLanguage ? appCode : survivalCode,
                        appLanguage, dialog, search
                );
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(
                    android.R.color.transparent
            );
        }
    }

    private void populateLanguages(
            LinearLayout list,
            String query,
            String selectedCode,
            boolean appLanguage,
            AlertDialog dialog,
            EditText search) {

        list.removeAllViews();

        String filter = query.trim().toLowerCase(Locale.ROOT);
        int count = 0;

        for (int i = 0; i < names.length; i++) {
            if (!matches(i, filter)) {
                continue;
            }

            count++;

            final int index = i;
            boolean selected = codes[i].equals(selectedCode);

            LinearLayout item = new LinearLayout(this);
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setPadding(dp(14), dp(12), dp(14), dp(12));
            item.setBackground(makeItemBackground(selected));
            item.setClickable(true);
            item.setFocusable(true);

            LinearLayout.LayoutParams itemParams =
                    new LinearLayout.LayoutParams(-1, -2);
            itemParams.bottomMargin = dp(8);
            item.setLayoutParams(itemParams);

            LinearLayout textGroup = new LinearLayout(this);
            textGroup.setOrientation(LinearLayout.VERTICAL);

            LinearLayout.LayoutParams textParams =
                    new LinearLayout.LayoutParams(0, -2, 1f);
            textGroup.setLayoutParams(textParams);

            TextView english = new TextView(this);
            english.setText(names[i]);
            english.setTextSize(16);
            english.setTextColor(getColor(R.color.text_primary));

            TextView nativeName = new TextView(this);
            nativeName.setText(nativeNames[i] + "  ·  " + codes[i]);
            nativeName.setTextSize(13);
            nativeName.setTextColor(
                    getColor(R.color.text_secondary)
            );

            textGroup.addView(english);
            textGroup.addView(nativeName);

            TextView check = new TextView(this);
            check.setText(selected ? "✓" : "");
            check.setTextSize(21);
            check.setTypeface(null, Typeface.BOLD);
            check.setTextColor(getColor(R.color.button_primary));
            check.setGravity(Gravity.CENTER);
            check.setMinWidth(dp(32));

            item.addView(textGroup);
            item.addView(check);

            item.setOnClickListener(v -> {
                if (appLanguage) {
                    appCode = codes[index];
                } else {
                    survivalCode = codes[index];
                }

                updateLabels();
                hideKeyboard(search);
                dialog.dismiss();
            });

            list.addView(item);
        }

        if (count == 0) {
            TextView empty = new TextView(this);
            empty.setText("No languages found.\nTry another search.");
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(12), dp(28), dp(12), dp(28));
            empty.setTextSize(14);
            empty.setTextColor(getColor(R.color.text_secondary));
            list.addView(empty);
        }
    }

    private boolean matches(int index, String query) {
        if (query.isEmpty()) {
            return true;
        }

        return names[index].toLowerCase(Locale.ROOT)
                .contains(query)
                || nativeNames[index].toLowerCase(Locale.ROOT)
                .contains(query)
                || codes[index].contains(query);
    }

    private GradientDrawable makeItemBackground(boolean selected) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(getColor(selected
                ? R.color.button_primary
                : R.color.text_secondary));

        // Keep the normal card subtle; selected state is marked by ✓.
        background.setColor(getColor(android.R.color.transparent));
        background.setCornerRadius(dp(14));

        background.setStroke(
                dp(1),
                getColor(selected
                        ? R.color.button_primary
                        : R.color.text_secondary)
        );

        return background;
    }

    private int dp(int value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    private void hideKeyboard(View view) {
        InputMethodManager manager =
                (InputMethodManager) getSystemService(
                        Context.INPUT_METHOD_SERVICE
                );

        if (manager != null) {
            manager.hideSoftInputFromWindow(
                    view.getWindowToken(), 0
            );
        }
    }

    private void saveAndContinue() {
        preferences.edit()
                .putString("app_language", appCode)
                .putString("survival_language", survivalCode)
                .putBoolean("language_selected", true)
                .apply();

        if (editMode) {
            setResult(RESULT_OK);
            finish();
            return;
        }

        Intent intent = new Intent(
                LanguageActivity.this,
                LoginActivity.class
        );

        startActivity(intent);
        overridePendingTransition(
                android.R.anim.fade_in,
                android.R.anim.fade_out
        );
        finish();
    }
}
