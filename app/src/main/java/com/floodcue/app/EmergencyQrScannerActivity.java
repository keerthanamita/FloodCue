package com.floodcue.app;

import android.os.Bundle;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import androidx.activity.result.ActivityResultLauncher;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class EmergencyQrScannerActivity extends FloodCueActivity {
    private static final String SCAN_URL =
            "https://floodcue.heliohost.us/server/api/scan_emergency_qr.php";
    private TextView resultText;
    private final ActivityResultLauncher<ScanOptions> scanner =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() == null) {
                    resultText.setText(R.string.emergency_qr_scan_cancelled);
                } else {
                    verifyToken(result.getContents());
                }
            });

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_emergency_qr_scanner);
        resultText = findViewById(R.id.emergencyQrScanResult);
        findViewById(R.id.buttonBackFromEmergencyQrScanner).setOnClickListener(v -> finish());
        findViewById(R.id.buttonStartEmergencyQrScan).setOnClickListener(v -> startScan());
    }

    private void startScan() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt(getString(R.string.emergency_qr_scan_prompt));
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);
        scanner.launch(options);
    }

    private void verifyToken(String token) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { resultText.setText(R.string.emergency_qr_login_required); return; }
        resultText.setText(R.string.emergency_qr_verifying);
        user.getIdToken(false).addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) { resultText.setText(R.string.emergency_qr_failed); return; }
            new Thread(() -> {
                try {
                    HttpURLConnection c = (HttpURLConnection) new URL(SCAN_URL).openConnection();
                    c.setRequestMethod("POST"); c.setDoOutput(true); c.setConnectTimeout(15000); c.setReadTimeout(15000);
                    c.setRequestProperty("Authorization", "Bearer " + task.getResult().getToken());
                    c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    try (OutputStream out = c.getOutputStream()) { out.write(new JSONObject().put("token", token).toString().getBytes(StandardCharsets.UTF_8)); }
                    int code = c.getResponseCode();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(code < 400 ? c.getInputStream() : c.getErrorStream(), StandardCharsets.UTF_8));
                    StringBuilder body = new StringBuilder(); String line; while ((line = reader.readLine()) != null) body.append(line);
                    JSONObject response = new JSONObject(body.toString());
                    if (!response.optBoolean("ok")) throw new Exception(response.optString("error", "unauthorized"));
                    JSONObject profile = response.getJSONObject("profile");
                    runOnUiThread(() -> resultText.setText(formatProfile(profile)));
                    c.disconnect();
                } catch (Exception e) { runOnUiThread(() -> resultText.setText(getString(R.string.emergency_qr_scan_failed, e.getMessage()))); }
            }).start();
        });
    }

    private String formatProfile(JSONObject p) {
        return getString(R.string.emergency_qr_scan_success,
                p.optString("full_name", ""), p.optString("blood_group", ""),
                p.optString("allergies", ""), p.optString("medications", ""),
                p.optString("primary_contact_name", ""), p.optString("primary_contact_phone", ""));
    }
}
