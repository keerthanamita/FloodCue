package com.floodcue.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Short, practical guide to offline communication with Bridgefy.
 *
 * Bridgefy is a separate app; FloodCue only explains it and opens it.
 * Everything on this screen is bundled, so it works fully offline.
 */
public class OfflineCommunicationActivity extends FloodCueActivity {

    private static final String BRIDGEFY_PACKAGE = "me.bridgefy.main";
    private static final String BRIDGEFY_PLAY_URL =
            "https://play.google.com/store/apps/details?id=me.bridgefy.main";

    private static final String[] STEPS = {
            "1. Install Bridgefy from Google Play before the flood season. Installing needs internet once; using it afterwards does not.",
            "2. Open Bridgefy once and finish its activation step while you still have internet — activation also requires a one-time connection.",
            "3. Turn on Bluetooth and keep it on. On newer Android versions the app may also ask for location permission, which the system uses for nearby-device discovery.",
            "4. Everyone you want to reach must also have Bridgefy installed with Bluetooth switched on.",
            "5. Messages work directly over roughly 100 metres; where supported, they can hop from phone to phone through other nearby users (mesh network).",
            "6. Walls, terrain, distance and low battery reduce range — delivery is never guaranteed.",
            "7. Once activated, messaging needs no internet, no mobile data and no Wi-Fi, but it does need working Bluetooth on both phones.",
    };

    private TextView statusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline_communication);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(R.string.offline_comm_heading);
        }

        findViewById(R.id.buttonBackFromOfflineComm).setOnClickListener(v -> finish());
        statusView = findViewById(R.id.offlineCommStatus);

        Button openButton = findViewById(R.id.buttonOpenBridgefy);
        openButton.setOnClickListener(v -> openBridgefy());

        LinearLayout stepsContainer = findViewById(R.id.offlineCommSteps);
        int topMargin = Math.round(10 * getResources().getDisplayMetrics().density);
        for (String step : STEPS) {
            TextView row = new TextView(this);
            row.setText(step);
            row.setTextColor(getColor(R.color.text_primary));
            row.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            row.setLineSpacing(Math.round(2 * getResources().getDisplayMetrics().density), 1f);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.topMargin = topMargin;
            row.setLayoutParams(params);
            stepsContainer.addView(row);
        }
    }

    /** Opens Bridgefy when it is installed; otherwise explains and offers Google Play. */
    private void openBridgefy() {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(BRIDGEFY_PACKAGE);
        if (launchIntent != null) {
            try {
                startActivity(launchIntent);
                statusView.setVisibility(View.GONE);
                return;
            } catch (RuntimeException failedToLaunch) {
                // Fall through to the message below.
            }
        }

        statusView.setText(R.string.offline_comm_not_installed);
        statusView.setVisibility(View.VISIBLE);

        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(BRIDGEFY_PLAY_URL)));
        } catch (RuntimeException cannotOpenStore) {
            Toast.makeText(this, R.string.offline_comm_open_failed, Toast.LENGTH_SHORT).show();
        }
    }
}
