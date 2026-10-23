package com.floodcue.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

public class SetupActivity extends AppCompatActivity {

    // Main screen containers
    private View screen1;
    private View screen2;
    private View screen3;

    // Navigation buttons
    private MaterialButton buttonNext1;
    private MaterialButton buttonBack2;
    private MaterialButton buttonNext2;
    private MaterialButton buttonBack3;
    private MaterialButton buttonFinish;

    // Screen 1
    private MaterialAutoCompleteTextView survivalLanguageInput;
    private MaterialAutoCompleteTextView appLanguageInput;

    // Screen 2
    private TextInputEditText editTextName;
    private TextInputEditText editTextBloodGroup;
    private TextInputEditText editTextEmergencyContact;

    // Current screen
    private int currentScreen = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_setup);

        initializeViews();
        setupNavigation();
        showScreen(1);

        setupSystemBackButton();
    }

    private void initializeViews() {

        // Screen containers
        screen1 = findViewById(R.id.setupScreen1);
        screen2 = findViewById(R.id.setupScreen2);
        screen3 = findViewById(R.id.setupScreen3);

        // Navigation
        buttonNext1 = findViewById(R.id.buttonNext1);

        buttonBack2 = findViewById(R.id.buttonBack2);
        buttonNext2 = findViewById(R.id.buttonNext2);

        buttonBack3 = findViewById(R.id.buttonBack3);
        buttonFinish = findViewById(R.id.buttonFinish);

        // Screen 1
        survivalLanguageInput =
                findViewById(R.id.survivalLanguageInput);

        appLanguageInput =
                findViewById(R.id.appLanguageInput);

        // Screen 2
        editTextName =
                findViewById(R.id.editTextName);

        editTextBloodGroup =
                findViewById(R.id.editTextBloodGroup);

        editTextEmergencyContact =
                findViewById(R.id.editTextEmergencyContact);
    }

    private void setupNavigation() {

        // SCREEN 1 → SCREEN 2
        buttonNext1.setOnClickListener(v -> {
            showScreen(2);
        });

        // SCREEN 2 → SCREEN 1
        buttonBack2.setOnClickListener(v -> {
            showScreen(1);
        });

        // SCREEN 2 → SCREEN 3
        buttonNext2.setOnClickListener(v -> {
            showScreen(3);
        });

        // SCREEN 3 → SCREEN 2
        buttonBack3.setOnClickListener(v -> {
            showScreen(2);
        });

        // FINISH
        buttonFinish.setOnClickListener(v -> {

            /*
             * PLACEHOLDER ONLY
             *
             * Later this button will:
             *
             * 1. Validate setup information
             * 2. Request required permissions
             * 3. Get current location
             * 4. Detect region
             * 5. Save setup information
             * 6. Download offline resources
             * 7. Mark setup as completed
             * 8. Open HomeActivity
             */

            Intent intent = new Intent(
                    SetupActivity.this,
                    HomeActivity.class
            );

            startActivity(intent);
            finish();
        });
    }

    private void showScreen(int screenNumber) {

        currentScreen = screenNumber;

        screen1.setVisibility(View.GONE);
        screen2.setVisibility(View.GONE);
        screen3.setVisibility(View.GONE);

        if (screenNumber == 1) {

            screen1.setVisibility(View.VISIBLE);

        } else if (screenNumber == 2) {

            screen2.setVisibility(View.VISIBLE);

        } else if (screenNumber == 3) {

            screen3.setVisibility(View.VISIBLE);
        }
    }

    private void setupSystemBackButton() {

        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {

                    @Override
                    public void handleOnBackPressed() {

                        if (currentScreen == 3) {

                            showScreen(2);

                        } else if (currentScreen == 2) {

                            showScreen(1);

                        } else {

                            /*
                             * On Screen 1, allow the Activity
                             * to close normally.
                             */
                            finish();
                        }
                    }
                }
        );
    }
}