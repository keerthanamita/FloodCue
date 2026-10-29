package com.floodcue.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AboutBridgefyActivity extends FloodCueActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_bridgefy);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(R.string.about_bridgefy_heading);
        }

        TextView buttonBackToSettings = findViewById(R.id.buttonBackToSettings);
        Button openWebsiteButton = findViewById(R.id.buttonOpenBridgefyWebsite);
        Button getAppButton = findViewById(R.id.buttonGetBridgefyApp);

        buttonBackToSettings.setOnClickListener(v -> finish());
        openWebsiteButton.setOnClickListener(v ->
                openUrl(getString(R.string.about_bridgefy_website_url)));
        getAppButton.setOnClickListener(v -> openBridgefyApp());
    }

    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        try {
            startActivity(intent);
        } catch (Exception exception) {
            Toast.makeText(
                    this,
                    R.string.about_bridgefy_open_failed,
                    Toast.LENGTH_SHORT
            ).show();
        }
    }
    private void openBridgefyApp() {
        String packageName = "me.bridgefy.main";

        Intent launchIntent = getPackageManager()
                .getLaunchIntentForPackage(packageName);

        if (launchIntent != null) {
            try {
                startActivity(launchIntent);
            } catch (Exception e) {
                openBridgefyPlayStore();
            }
        } else {
            openBridgefyPlayStore();
        }
    }

    private void openBridgefyPlayStore() {
        String packageName = "me.bridgefy.main";

        try {
            Intent intent = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + packageName)
            );
            startActivity(intent);
        } catch (Exception e) {
            openUrl(
                    "https://play.google.com/store/apps/details?id="
                            + packageName
            );
        }
    }
}
