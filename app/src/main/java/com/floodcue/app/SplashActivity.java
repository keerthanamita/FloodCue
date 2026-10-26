package com.floodcue.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends FloodCueActivity {

    private TextView loadingStatus;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        loadingStatus = findViewById(R.id.loadingStatus);
        preferences = getSharedPreferences("FloodCuePrefs", MODE_PRIVATE);

        openNextScreen();
    }

    private void openNextScreen() {
        Intent intent;

        boolean languageSelected =
                preferences.getBoolean("language_selected", false);

        if (!languageSelected) {
            loadingStatus.setText("Choose your language…");
            intent = new Intent(this, LanguageActivity.class);

        } else {
            FirebaseUser user =
                    FirebaseAuth.getInstance().getCurrentUser();

            if (user == null) {
                preferences.edit()
                        .putBoolean("is_logged_in", false)
                        .apply();

                loadingStatus.setText("Opening sign in…");
                intent = new Intent(this, LoginActivity.class);

            } else {
                preferences.edit()
                        .putBoolean("is_logged_in", true)
                        .apply();

                String setupKey = "setup_completed_" + user.getUid();
                boolean setupCompleted =
                        preferences.getBoolean(setupKey, false);

                if (setupCompleted) {
                    loadingStatus.setText("Opening FloodCue…");
                    intent = new Intent(this, HomeActivity.class);
                } else {
                    loadingStatus.setText("Opening profile setup…");
                    intent = new Intent(this, SetupActivity.class);
                }
            }
        }

        startActivity(intent);
        finish();
    }
}
