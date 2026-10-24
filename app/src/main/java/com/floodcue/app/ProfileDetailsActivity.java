package com.floodcue.app;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileDetailsActivity extends AppCompatActivity {

    private LinearLayout profileDetailsContent;
    private TextView profileDetailsEmpty;
    private TextView buttonEditProfile;
    private LinearLayout currentSection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_details);

        TextView buttonBackToSettings = findViewById(R.id.buttonBackToSettings);
        buttonEditProfile = findViewById(R.id.buttonEditProfile);
        profileDetailsContent = findViewById(R.id.profileDetailsContent);
        profileDetailsEmpty = findViewById(R.id.profileDetailsEmpty);

        buttonBackToSettings.setOnClickListener(v -> finish());
        buttonEditProfile.setOnClickListener(v -> openSetupForEdit());
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindProfile();
    }

    private void bindProfile() {
        profileDetailsContent.removeAllViews();
        currentSection = null;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            showEmpty(getString(R.string.profile_details_login_required));
            return;
        }

        UserProfile profile = ProfileStorage.getProfile(this, user.getUid());
        if (profile == null) {
            showEmpty(getString(R.string.profile_details_empty));
            return;
        }

        profileDetailsEmpty.setVisibility(View.GONE);
        profileDetailsContent.setVisibility(View.VISIBLE);
        buttonEditProfile.setVisibility(View.VISIBLE);

        addSection(R.string.profile_section_personal);
        addField(R.string.profile_label_full_name, profile.full_name);
        addField(R.string.profile_label_blood_group, profile.blood_group);
        addField(R.string.profile_label_allergies, profile.allergies);
        addField(R.string.profile_label_other_allergies, profile.other_allergies);
        addField(R.string.profile_label_medications, profile.medications);
        addField(R.string.profile_label_medical_notes, profile.medical_notes);

        addSection(R.string.profile_section_primary_contact);
        addField(R.string.profile_label_name, profile.primary_contact_name);
        addField(R.string.profile_label_relationship, profile.primary_contact_relationship);
        addField(R.string.profile_label_phone, profile.primary_contact_phone);
        addField(R.string.profile_label_alternate_phone, profile.primary_contact_alt_phone);

        addSection(R.string.profile_section_second_contact);
        addField(R.string.profile_label_name, profile.second_contact_name);
        addField(R.string.profile_label_relationship, profile.second_contact_relationship);
        addField(R.string.profile_label_phone, profile.second_contact_phone);
        addField(R.string.profile_label_alternate_phone, profile.second_contact_alt_phone);

        addSection(R.string.profile_section_third_contact);
        addField(R.string.profile_label_name, profile.third_contact_name);
        addField(R.string.profile_label_relationship, profile.third_contact_relationship);
        addField(R.string.profile_label_phone, profile.third_contact_phone);
        addField(R.string.profile_label_alternate_phone, profile.third_contact_alt_phone);

        addSection(R.string.profile_section_location);
        addField(R.string.profile_label_assistance, profile.special_assistance);
        addField(R.string.profile_label_country, profile.country);
        addField(R.string.profile_label_state, profile.state);
        addField(R.string.profile_label_district, profile.district);
        addField(R.string.profile_label_app_language, profile.app_language);
        addField(R.string.profile_label_survival_language, profile.survival_language);
    }

    private void showEmpty(String message) {
        profileDetailsContent.setVisibility(View.GONE);
        profileDetailsEmpty.setText(message);
        profileDetailsEmpty.setVisibility(View.VISIBLE);
        buttonEditProfile.setVisibility(View.VISIBLE);
    }

    private void addSection(int titleRes) {
        currentSection = new LinearLayout(this);
        currentSection.setOrientation(LinearLayout.VERTICAL);
        currentSection.setBackgroundResource(R.drawable.bg_screen_card);

        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        currentSection.setPadding(padding, padding, padding, padding);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        if (profileDetailsContent.getChildCount() > 0) {
            params.topMargin = (int) (16 * getResources().getDisplayMetrics().density);
        }
        currentSection.setLayoutParams(params);

        TextView title = new TextView(this);
        title.setText(titleRes);
        title.setTextColor(getColor(R.color.text_primary));
        title.setTextSize(17);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        currentSection.addView(title);

        profileDetailsContent.addView(currentSection);
    }

    private void addField(int labelRes, String value) {
        View row = LayoutInflater.from(this)
                .inflate(R.layout.item_profile_field, currentSection, false);

        TextView labelView = row.findViewById(R.id.profileFieldLabel);
        TextView valueView = row.findViewById(R.id.profileFieldValue);

        labelView.setText(labelRes);
        valueView.setText(displayValue(value));

        currentSection.addView(row);
    }

    private String displayValue(String value) {
        if (value == null
                || value.trim().isEmpty()
                || "null".equalsIgnoreCase(value.trim())) {
            return getString(R.string.profile_value_empty);
        }

        return value.trim();
    }

    private void openSetupForEdit() {
        Intent intent = new Intent(this, SetupActivity.class);
        intent.putExtra(SetupActivity.EXTRA_EDIT_PROFILE, true);
        startActivity(intent);
    }
}
