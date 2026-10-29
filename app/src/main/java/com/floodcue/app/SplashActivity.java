package com.floodcue.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Handler;
import android.os.Looper;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends FloodCueActivity {

    private TextView loadingStatus;

    @Override
    protected boolean showsBottomNavigation() {
        return false;
    }

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        loadingStatus = findViewById(R.id.loadingStatus);
        preferences = getSharedPreferences("FloodCuePrefs", MODE_PRIVATE);

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                openNextScreen();
            }
        }, 1500);
    }

    private void openNextScreen() {
        Intent intent;

        boolean languageSelected =
                preferences.getBoolean("language_selected", false);

        if (!languageSelected) {
            loadingStatus.setText(R.string.splash_choose_language);
            intent = new Intent(this, LanguageActivity.class);

        } else {
            FirebaseUser user =
                    FirebaseAuth.getInstance().getCurrentUser();

            if (user == null) {
                preferences.edit()
                        .putBoolean("is_logged_in", false)
                        .apply();

                loadingStatus.setText(R.string.splash_opening_sign_in);
                intent = new Intent(this, LoginActivity.class);

            } else {
                preferences.edit()
                        .putBoolean("is_logged_in", true)
                        .apply();

                String setupKey = "setup_completed_" + user.getUid();
                boolean setupCompleted =
                        preferences.getBoolean(setupKey, false);

                if (setupCompleted) {
                    loadingStatus.setText(R.string.splash_opening_app);
                    intent = new Intent(this, HomeActivity.class);
                } else {
                    loadingStatus.setText(R.string.splash_opening_setup);
                    intent = new Intent(this, SetupActivity.class);
                }
            }
        }

        startActivity(intent);
        finish();
    }
}
