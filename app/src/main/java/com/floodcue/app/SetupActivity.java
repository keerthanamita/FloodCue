
package com.floodcue.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SetupActivity extends FloodCueActivity {

    @Override
    protected boolean showsBottomNavigation() {
        return false;
    }


    public static final String EXTRA_EDIT_PROFILE = "edit_profile";

    private static final String PREFS_NAME = "FloodCuePrefs";
    private static final String BRIDGEFY_URL =
            "https://play.google.com/store/apps/details?id=me.bridgefy.main";

    private ScrollView pageSetupOne;
    private ScrollView pageSetupTwo;
    private ScrollView pageSetupThree;

    private LinearLayout layoutSetupNav;
    private TextView textSetupStep;
    private TextView textSetupSubtitle;

    private Button buttonBack;
    private Button buttonSkip;
    private Button buttonNext;
    private Button buttonComplete;
    private Button buttonInstallBridgefy;

    private ProgressBar progressSetup;

    private EditText editFullName;
    private Spinner spinnerBloodGroup;

    private CheckBox allergyMedicines;
    private CheckBox allergyFood;
    private CheckBox allergyInsects;
    private CheckBox allergyLatex;
    private CheckBox allergyOther;
    private CheckBox allergyNone;
    private EditText editOtherAllergies;

    private EditText editMedications;
    private EditText editMedicalNotes;

    private EditText editPrimaryContactName;
    private Spinner spinnerPrimaryRelationship;
    private EditText editPrimaryPhone;
    private EditText editPrimaryAlternatePhone;

    private EditText editSecondContactName;
    private Spinner spinnerSecondRelationship;
    private EditText editSecondPhone;
    private EditText editSecondAlternatePhone;

    private EditText editThirdContactName;
    private Spinner spinnerThirdRelationship;
    private EditText editThirdPhone;
    private EditText editThirdAlternatePhone;

    private CheckBox assistanceNone;
    private CheckBox assistanceElderly;
    private CheckBox assistanceChild;
    private CheckBox assistancePregnant;
    private CheckBox assistanceWheelchair;
    private CheckBox assistanceWalking;
    private CheckBox assistanceHearing;
    private CheckBox assistanceVisual;
    private CheckBox assistanceOther;

    private Spinner spinnerCountry;
    private Spinner spinnerState;
    private Spinner spinnerDistrict;

    private SharedPreferences preferences;
    private String uid;

    private int currentPage = 1;
    private boolean isSaving = false;
    private boolean isLoadingProfile = false;
    private boolean serverProfileLoaded = false;

    private UserProfile existingProfile;

    private final String[] bloodGroups = {
            "Choose blood group",
            "A+", "A-", "B+", "B-",
            "AB+", "AB-", "O+", "O-"
    };

    private final String[] relationships = {
            "Choose relationship",
            "Parent", "Spouse", "Sibling", "Child",
            "Friend", "Neighbour", "Relative", "Other"
    };

    private final String[] states = {
            "Andhra Pradesh",
            "Telangana",
            "Tamil Nadu",
            "Karnataka",
            "Kerala"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    this,
                    getString(R.string.setup_sign_in_required),
                    Toast.LENGTH_LONG
            ).show();
            finish();
            return;
        }

        uid = user.getUid();

        bindViews();
        setupSpinners();
        setupAllergyOptions();
        setupAssistanceOptions();
        setupNavigation();

        loadSavedProfile();
        showPage(1);

        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (isSaving) {
                            Toast.makeText(
                                    SetupActivity.this,
                                    getString(R.string.setup_wait_saving),
                                    Toast.LENGTH_SHORT
                            ).show();
                        } else if (currentPage > 1) {
                            showPage(currentPage - 1);
                        } else {
                            returnToLogin();
                        }
                    }
                }
        );

        fetchServerProfile();
    }

    private void bindViews() {
        pageSetupOne = findViewById(R.id.pageSetupOne);
        pageSetupTwo = findViewById(R.id.pageSetupTwo);
        pageSetupThree = findViewById(R.id.pageSetupThree);

        layoutSetupNav = findViewById(R.id.layoutSetupNav);
        textSetupStep = findViewById(R.id.textSetupStep);
        textSetupSubtitle = findViewById(R.id.textSetupSubtitle);

        buttonBack = findViewById(R.id.buttonSetupBack);
        buttonSkip = findViewById(R.id.buttonSetupSkip);
        buttonNext = findViewById(R.id.buttonSetupNext);
        buttonComplete = findViewById(R.id.buttonCompleteSetup);
        buttonInstallBridgefy = findViewById(R.id.buttonInstallBridgefy);
        progressSetup = findViewById(R.id.progressSetup);

        editFullName = findViewById(R.id.editFullName);
        spinnerBloodGroup = findViewById(R.id.spinnerBloodGroup);

        allergyMedicines = findViewById(R.id.checkAllergyMedicines);
        allergyFood = findViewById(R.id.checkAllergyFood);
        allergyInsects = findViewById(R.id.checkAllergyInsects);
        allergyLatex = findViewById(R.id.checkAllergyLatex);
        allergyOther = findViewById(R.id.checkAllergyOther);
        allergyNone = findViewById(R.id.checkAllergyNone);
        editOtherAllergies = findViewById(R.id.editOtherAllergies);

        editMedications = findViewById(R.id.editMedications);
        editMedicalNotes = findViewById(R.id.editMedicalNotes);

        editPrimaryContactName = findViewById(R.id.editPrimaryContactName);
        spinnerPrimaryRelationship =
                findViewById(R.id.spinnerPrimaryRelationship);
        editPrimaryPhone = findViewById(R.id.editPrimaryPhone);
        editPrimaryAlternatePhone =
                findViewById(R.id.editPrimaryAlternatePhone);

        editSecondContactName = findViewById(R.id.editSecondContactName);
        spinnerSecondRelationship =
                findViewById(R.id.spinnerSecondRelationship);
        editSecondPhone = findViewById(R.id.editSecondPhone);
        editSecondAlternatePhone =
                findViewById(R.id.editSecondAlternatePhone);

        editThirdContactName = findViewById(R.id.editThirdContactName);
        spinnerThirdRelationship =
                findViewById(R.id.spinnerThirdRelationship);
        editThirdPhone = findViewById(R.id.editThirdPhone);
        editThirdAlternatePhone =
                findViewById(R.id.editThirdAlternatePhone);

        assistanceNone = findViewById(R.id.checkAssistanceNone);
        assistanceElderly = findViewById(R.id.checkAssistanceElderly);
        assistanceChild = findViewById(R.id.checkAssistanceChild);
        assistancePregnant = findViewById(R.id.checkAssistancePregnant);
        assistanceWheelchair = findViewById(R.id.checkAssistanceWheelchair);
        assistanceWalking = findViewById(R.id.checkAssistanceWalking);
        assistanceHearing = findViewById(R.id.checkAssistanceHearing);
        assistanceVisual = findViewById(R.id.checkAssistanceVisual);
        assistanceOther = findViewById(R.id.checkAssistanceOther);

        spinnerCountry = findViewById(R.id.spinnerCountry);
        spinnerState = findViewById(R.id.spinnerState);
        spinnerDistrict = findViewById(R.id.spinnerDistrict);
    }

    private void setupSpinners() {
        setSpinnerAdapter(spinnerBloodGroup, bloodGroups);
        setSpinnerAdapter(spinnerPrimaryRelationship, relationships);
        setSpinnerAdapter(spinnerSecondRelationship, relationships);
        setSpinnerAdapter(spinnerThirdRelationship, relationships);
        setSpinnerAdapter(spinnerCountry, new String[]{"India"});
        setSpinnerAdapter(spinnerState, states);

        spinnerState.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {
                        String state = states[
                                Math.max(0, Math.min(position, states.length - 1))
                                ];

                        String selectedDistrict = null;

                        if (existingProfile != null
                                && existingProfile.state != null
                                && existingProfile.state.equalsIgnoreCase(state)) {
                            selectedDistrict = existingProfile.district;
                        }

                        updateDistrictSpinner(state, selectedDistrict);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        updateDistrictSpinner("Andhra Pradesh", null);
                    }
                }
        );

        updateDistrictSpinner("Andhra Pradesh", null);
    }

    private void setSpinnerAdapter(Spinner spinner, String[] values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                values
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinner.setAdapter(adapter);
    }

    private void updateDistrictSpinner(
            String state,
            String selectedDistrict
    ) {
        String[] districts;

        switch (state) {
            case "Andhra Pradesh":
                districts = new String[]{
                        "Anakapalli", "Anantapur", "Annamayya", "Bapatla",
                        "Chittoor", "Dr. B. R. Ambedkar Konaseema",
                        "East Godavari", "Eluru", "Guntur", "Kakinada",
                        "Krishna", "Kurnool", "Nandyal", "NTR",
                        "Palnadu", "Parvathipuram Manyam", "Prakasam",
                        "Srikakulam", "Sri Potti Sriramulu Nellore",
                        "Sri Sathya Sai", "Tirupati", "Visakhapatnam",
                        "Vizianagaram", "West Godavari", "YSR Kadapa"
                };
                break;

            case "Telangana":
                districts = new String[]{
                        "Adilabad", "Bhadradri Kothagudem", "Hanumakonda",
                        "Hyderabad", "Jagtial", "Jangaon",
                        "Jayashankar Bhupalpally", "Jogulamba Gadwal",
                        "Kamareddy", "Karimnagar", "Khammam",
                        "Kumuram Bheem Asifabad", "Mahabubabad",
                        "Mahabubnagar", "Mancherial", "Medak",
                        "Medchal-Malkajgiri", "Mulugu", "Nagarkurnool",
                        "Nalgonda", "Narayanpet", "Nirmal", "Nizamabad",
                        "Peddapalli", "Rajanna Sircilla", "Rangareddy",
                        "Sangareddy", "Siddipet", "Suryapet", "Vikarabad",
                        "Wanaparthy", "Warangal", "Yadadri Bhuvanagiri"
                };
                break;

            case "Tamil Nadu":
                districts = new String[]{
                        "Chennai", "Coimbatore", "Madurai", "Salem",
                        "Tiruchirappalli", "Tirunelveli", "Vellore", "Erode"
                };
                break;

            case "Karnataka":
                districts = new String[]{
                        "Bengaluru Urban", "Mysuru", "Mangaluru",
                        "Udupi", "Belagavi", "Kalaburagi", "Shivamogga"
                };
                break;

            default:
                districts = new String[]{
                        "Thiruvananthapuram", "Kochi", "Thrissur",
                        "Kozhikode", "Kannur", "Kollam"
                };
                break;
        }

        setSpinnerAdapter(spinnerDistrict, districts);

        if (!TextUtils.isEmpty(selectedDistrict)) {
            setSpinnerSelection(spinnerDistrict, selectedDistrict, false);
        }
    }

    private void setupAllergyOptions() {
        allergyNone.setOnCheckedChangeListener((button, checked) -> {
            if (checked) {
                allergyMedicines.setChecked(false);
                allergyFood.setChecked(false);
                allergyInsects.setChecked(false);
                allergyLatex.setChecked(false);
                allergyOther.setChecked(false);
                editOtherAllergies.setText("");
                editOtherAllergies.setVisibility(View.GONE);
            }
        });

        CheckBox[] allergyChecks = {
                allergyMedicines,
                allergyFood,
                allergyInsects,
                allergyLatex,
                allergyOther
        };

        for (CheckBox checkBox : allergyChecks) {
            checkBox.setOnCheckedChangeListener((button, checked) -> {
                if (checked) {
                    allergyNone.setChecked(false);
                }

                editOtherAllergies.setVisibility(
                        allergyOther.isChecked() ? View.VISIBLE : View.GONE
                );
            });
        }
    }

    private void setupAssistanceOptions() {
        CheckBox[] options = {
                assistanceElderly,
                assistanceChild,
                assistancePregnant,
                assistanceWheelchair,
                assistanceWalking,
                assistanceHearing,
                assistanceVisual,
                assistanceOther
        };

        assistanceNone.setOnCheckedChangeListener((button, checked) -> {
            if (checked) {
                for (CheckBox option : options) {
                    option.setChecked(false);
                }
            }
        });

        for (CheckBox option : options) {
            option.setOnCheckedChangeListener((button, checked) -> {
                if (checked) {
                    assistanceNone.setChecked(false);
                }
            });
        }
    }

    private void setupNavigation() {
        buttonBack.setOnClickListener(v -> showPage(currentPage - 1));

        buttonSkip.setOnClickListener(v -> {
            if (currentPage == 1) {
                showPage(2);
            } else if (currentPage == 2) {
                showPage(3);
            }
        });

        buttonNext.setOnClickListener(v -> {
            if (currentPage == 1) {
                if (validatePersonalInformation()) {
                    showPage(2);
                }
            } else if (currentPage == 2) {
                showPage(3);
            }
        });

        buttonComplete.setOnClickListener(v -> saveProfile());

        buttonInstallBridgefy.setOnClickListener(v -> {
            try {
                startActivity(new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(BRIDGEFY_URL)
                ));
            } catch (Exception e) {
                Toast.makeText(
                        this,
                        getString(R.string.setup_play_store_failed),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void showPage(int page) {
        if (page < 1) {
            finish();
            return;
        }

        currentPage = Math.min(page, 3);

        pageSetupOne.setVisibility(
                currentPage == 1 ? View.VISIBLE : View.GONE
        );
        pageSetupTwo.setVisibility(
                currentPage == 2 ? View.VISIBLE : View.GONE
        );
        pageSetupThree.setVisibility(
                currentPage == 3 ? View.VISIBLE : View.GONE
        );

        textSetupStep.setText(getString(R.string.setup_step, currentPage));
        progressSetup.setProgress(currentPage * 33);

        textSetupSubtitle.setText(
                currentPage == 1
                        ? getString(R.string.setup_subtitle_personal)
                        : currentPage == 2
                        ? getString(R.string.setup_subtitle_contacts)
                        : getString(R.string.setup_subtitle_review)
        );

        layoutSetupNav.setVisibility(
                currentPage == 3 ? View.GONE : View.VISIBLE
        );
        buttonBack.setVisibility(
                currentPage == 1 ? View.GONE : View.VISIBLE
        );
        buttonSkip.setVisibility(
                currentPage == 3 ? View.GONE : View.VISIBLE
        );

        buttonNext.setText(
                currentPage == 2
                        ? getString(R.string.setup_review_button)
                        : getString(R.string.setup_continue_button)
        );

        if (currentPage == 1) {
            pageSetupOne.post(() -> pageSetupOne.scrollTo(0, 0));
        } else if (currentPage == 2) {
            pageSetupTwo.post(() -> pageSetupTwo.scrollTo(0, 0));
        } else {
            pageSetupThree.post(() -> pageSetupThree.scrollTo(0, 0));
        }
    }

    private boolean validatePersonalInformation() {
        String name = editFullName.getText().toString().trim();

        if (name.isEmpty()) {
            editFullName.setError(getString(R.string.setup_full_name_required));
            editFullName.requestFocus();
            return false;
        }

        if (spinnerBloodGroup.getSelectedItemPosition() == 0) {
            Toast.makeText(
                    this,
                    R.string.setup_blood_group_required,
                    Toast.LENGTH_SHORT
            ).show();
            return false;
        }

        return true;
    }

    private void loadSavedProfile() {
        existingProfile = ProfileStorage.getProfile(this, uid);

        if (existingProfile != null) {
            loadProfileIntoForm(existingProfile);
        }
    }

    private void fetchServerProfile() {
        ProfileApi.fetchProfile((success, exists, profile, error) -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }

            serverProfileLoaded = success;

            if (!success) {
                // Retain the local profile if the server is unavailable.
                return;
            }

            if (!exists || profile == null) {
                return;
            }

            existingProfile = profile;

            try {
                ProfileStorage.saveProfile(this, uid, profile);
            } catch (JSONException | RuntimeException ignored) {
                // Continue with the server profile even if caching fails.
            }

            if (profile.setup_completed && !isEditingProfile()) {
                // The profile is finished. Log the user in and send them to
                // the home screen, but only from the final page so this does
                // not interrupt the current setup step on pages one or two.
                preferences.edit()
                        .putBoolean("is_logged_in", true)
                        .apply();

                if (currentPage == 3) {
                    openHome();
                }
                return;
            }

            // Avoid replacing fields while the user is actively editing.
            if (!isSaving && currentPage == 1) {
                loadProfileIntoForm(profile);
            }
        });
    }

    private void loadProfileIntoForm(UserProfile profile) {
        if (profile == null) {
            return;
        }

        isLoadingProfile = true;

        setText(editFullName, profile.full_name);
        setSpinnerSelection(spinnerBloodGroup, profile.blood_group, true);

        String allergies = safe(profile.allergies).toLowerCase(Locale.ROOT);

        allergyMedicines.setChecked(allergies.contains("medicine"));
        allergyFood.setChecked(allergies.contains("food"));
        allergyInsects.setChecked(allergies.contains("insect"));
        allergyLatex.setChecked(allergies.contains("latex"));
        allergyOther.setChecked(allergies.contains("other"));
        allergyNone.setChecked(
                allergies.contains("none")
                        || allergies.contains("no known allergies")
        );

        setText(editOtherAllergies, profile.other_allergies);
        editOtherAllergies.setVisibility(
                allergyOther.isChecked() ? View.VISIBLE : View.GONE
        );

        setText(editMedications, profile.medications);
        setText(editMedicalNotes, profile.medical_notes);

        setText(editPrimaryContactName, profile.primary_contact_name);
        setSpinnerSelection(
                spinnerPrimaryRelationship,
                profile.primary_contact_relationship,
                true
        );
        setText(editPrimaryPhone, profile.primary_contact_phone);
        setText(editPrimaryAlternatePhone, profile.primary_contact_alt_phone);

        setText(editSecondContactName, profile.second_contact_name);
        setSpinnerSelection(
                spinnerSecondRelationship,
                profile.second_contact_relationship,
                true
        );
        setText(editSecondPhone, profile.second_contact_phone);
        setText(editSecondAlternatePhone, profile.second_contact_alt_phone);

        setText(editThirdContactName, profile.third_contact_name);
        setSpinnerSelection(
                spinnerThirdRelationship,
                profile.third_contact_relationship,
                true
        );
        setText(editThirdPhone, profile.third_contact_phone);
        setText(editThirdAlternatePhone, profile.third_contact_alt_phone);

        String assistance = safe(profile.special_assistance)
                .toLowerCase(Locale.ROOT);

        assistanceNone.setChecked(
                assistance.isEmpty() || assistance.equals("no")
        );
        assistanceElderly.setChecked(assistance.contains("elderly"));
        assistanceChild.setChecked(assistance.contains("child"));
        assistancePregnant.setChecked(assistance.contains("pregnant"));
        assistanceWheelchair.setChecked(assistance.contains("wheelchair"));
        assistanceWalking.setChecked(assistance.contains("walking"));
        assistanceHearing.setChecked(assistance.contains("hearing"));
        assistanceVisual.setChecked(assistance.contains("visual"));
        assistanceOther.setChecked(assistance.contains("other"));

        setSpinnerSelection(spinnerCountry, profile.country, false);
        setSpinnerSelection(spinnerState, profile.state, false);

        String state = selectedValue(spinnerState);
        if (state.isEmpty()) {
            state = "Andhra Pradesh";
        }

        updateDistrictSpinner(state, profile.district);

        isLoadingProfile = false;
    }

    private void setText(EditText view, String value) {
        view.setText(value == null ? "" : value);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void setSpinnerSelection(
            Spinner spinner,
            String value,
            boolean skipFirst
    ) {
        if (TextUtils.isEmpty(value) || spinner.getAdapter() == null) {
            return;
        }

        for (int i = 0; i < spinner.getCount(); i++) {
            Object item = spinner.getItemAtPosition(i);

            if (item != null
                    && item.toString().equalsIgnoreCase(value.trim())) {
                spinner.setSelection(i);
                return;
            }
        }

        if (skipFirst && spinner.getCount() > 0) {
            spinner.setSelection(0);
        }
    }

    private String selectedValue(Spinner spinner) {
        Object value = spinner.getSelectedItem();
        return value == null ? "" : value.toString().trim();
    }

    private String getAllergies() {
        List<String> selected = new ArrayList<>();

        if (allergyNone.isChecked()) {
            return "No known allergies";
        }

        if (allergyMedicines.isChecked()) selected.add("Medicines");
        if (allergyFood.isChecked()) selected.add("Food");
        if (allergyInsects.isChecked()) selected.add("Insect bites");
        if (allergyLatex.isChecked()) selected.add("Latex");
        if (allergyOther.isChecked()) selected.add("Other");

        return TextUtils.join(", ", selected);
    }

    private String getAssistance() {
        List<String> selected = new ArrayList<>();

        if (assistanceNone.isChecked()) {
            return "No";
        }

        if (assistanceElderly.isChecked()) selected.add("Elderly person");
        if (assistanceChild.isChecked()) selected.add("Child");
        if (assistancePregnant.isChecked()) selected.add("Pregnant");
        if (assistanceWheelchair.isChecked()) selected.add("Wheelchair user");
        if (assistanceWalking.isChecked()) selected.add("Difficulty walking");
        if (assistanceHearing.isChecked()) selected.add("Hearing impairment");
        if (assistanceVisual.isChecked()) selected.add("Visual impairment");
        if (assistanceOther.isChecked()) selected.add("Other");

        return selected.isEmpty() ? "No" : TextUtils.join(", ", selected);
    }

    private void saveProfile() {
        if (isSaving) {
            return;
        }

        if (!validatePersonalInformation()) {
            showPage(1);
            return;
        }

        hideKeyboard();

        isSaving = true;
        buttonComplete.setEnabled(false);
        buttonComplete.setText(R.string.setup_saving_profile);

        UserProfile profile = existingProfile == null
                ? new UserProfile()
                : existingProfile;

        /*
         * Language selection happens before login.
         * Read the saved preferences and include both values in the
         * profile JSON sent to save_profile.php.
         */
        profile.app_language = preferences.getString(
                "app_language",
                safeLanguage(profile.app_language, "en")
        );

        profile.survival_language = preferences.getString(
                "survival_language",
                safeLanguage(profile.survival_language, "en")
        );

        profile.full_name = editFullName.getText().toString().trim();

        String bloodGroup = selectedValue(spinnerBloodGroup);
        profile.blood_group = bloodGroups[0].equals(bloodGroup)
                ? ""
                : bloodGroup;

        profile.allergies = getAllergies();
        profile.other_allergies =
                editOtherAllergies.getText().toString().trim();
        profile.medications =
                editMedications.getText().toString().trim();
        profile.medical_notes =
                editMedicalNotes.getText().toString().trim();

        profile.primary_contact_name =
                editPrimaryContactName.getText().toString().trim();
        profile.primary_contact_relationship =
                normalizeRelationship(selectedValue(spinnerPrimaryRelationship));
        profile.primary_contact_phone =
                editPrimaryPhone.getText().toString().trim();
        profile.primary_contact_alt_phone =
                editPrimaryAlternatePhone.getText().toString().trim();

        profile.second_contact_name =
                editSecondContactName.getText().toString().trim();
        profile.second_contact_relationship =
                normalizeRelationship(selectedValue(spinnerSecondRelationship));
        profile.second_contact_phone =
                editSecondPhone.getText().toString().trim();
        profile.second_contact_alt_phone =
                editSecondAlternatePhone.getText().toString().trim();

        profile.third_contact_name =
                editThirdContactName.getText().toString().trim();
        profile.third_contact_relationship =
                normalizeRelationship(selectedValue(spinnerThirdRelationship));
        profile.third_contact_phone =
                editThirdPhone.getText().toString().trim();
        profile.third_contact_alt_phone =
                editThirdAlternatePhone.getText().toString().trim();

        profile.special_assistance = getAssistance();
        profile.country = selectedValue(spinnerCountry);
        profile.state = selectedValue(spinnerState);
        profile.district = selectedValue(spinnerDistrict);
        profile.setup_completed = true;

        ProfileApi.saveProfile(profile, (success, savedProfile, error) -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }

            isSaving = false;
            buttonComplete.setEnabled(true);
            buttonComplete.setText(R.string.setup_save_continue);

            if (!success || savedProfile == null) {
                Toast.makeText(
                        SetupActivity.this,
                        R.string.setup_save_failed,
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            existingProfile = savedProfile;

            // Preserve the saved languages locally as well.
            preferences.edit()
                    .putString("app_language", savedProfile.app_language)
                    .putString("survival_language", savedProfile.survival_language)
                    .putBoolean("is_logged_in", true)
                    .apply();

            try {
                ProfileStorage.saveProfile(this, uid, savedProfile);
            } catch (JSONException | RuntimeException e) {
                Toast.makeText(
                        this,
                        getString(R.string.setup_local_cache_failed),
                        Toast.LENGTH_LONG
                ).show();
            }

            openHome();
        });
    }

    private String normalizeRelationship(String relationship) {
        if (TextUtils.isEmpty(relationship)
                || relationship.startsWith("Choose ")) {
            return "";
        }

        return relationship;
    }

    private String safeLanguage(String language, String fallback) {
        if (TextUtils.isEmpty(language)
                || "null".equalsIgnoreCase(language)) {
            return fallback;
        }

        return language;
    }

    private void hideKeyboard() {
        View focus = getCurrentFocus();

        if (focus != null) {
            InputMethodManager manager = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);

            if (manager != null) {
                manager.hideSoftInputFromWindow(
                        focus.getWindowToken(),
                        0
                );
            }

            focus.clearFocus();
        }
    }

    private boolean isEditingProfile() {
        return getIntent().getBooleanExtra(EXTRA_EDIT_PROFILE, false);
    }

    private void returnToLogin() {
        if (isEditingProfile()) {
            finish();
            return;
        }

        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void openHome() {
        if (isEditingProfile()) {
            finish();
            return;
        }

        Intent intent = new Intent(this, HomeActivity.class);

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}
