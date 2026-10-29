package com.floodcue.app;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;

/**
 * "About FloodCue": what the app does, how the two languages work, what is
 * stored where, where the warnings come from, and the limits a user must know
 * in an emergency. Everything is bundled, so the screen works offline and
 * follows the app language like the rest of the settings screens.
 */
public class AboutFloodCueActivity extends FloodCueActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_floodcue);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(R.string.about_floodcue_heading);
        }

        findViewById(R.id.buttonBackToSettingsFromAbout).setOnClickListener(v -> finish());

        TextView versionView = findViewById(R.id.aboutFloodcueVersion);
        versionView.setText(getString(R.string.about_floodcue_version_label, versionName()));
    }

    private String versionName() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            if (info.versionName != null && !info.versionName.trim().isEmpty()) {
                return info.versionName;
            }
        } catch (PackageManager.NameNotFoundException ignored) {
            // Version simply is not readable on this device.
        }
        return "-";
    }
}
