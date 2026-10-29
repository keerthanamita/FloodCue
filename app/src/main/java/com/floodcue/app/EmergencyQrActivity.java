package com.floodcue.app;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class EmergencyQrActivity extends FloodCueActivity {
    private static final String CREATE_URL =
            "https://floodcue.heliohost.us/server/api/create_emergency_qr.php";
    private static final String REVOKE_URL =
            "https://floodcue.heliohost.us/server/api/revoke_emergency_qr.php";
    private ImageView qrImage;
    private TextView status;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_emergency_qr);
        qrImage = findViewById(R.id.emergencyQrImage);
        status = findViewById(R.id.emergencyQrStatus);
        findViewById(R.id.buttonBackFromEmergencyQr).setOnClickListener(v -> finish());
        findViewById(R.id.buttonRevokeEmergencyQr).setOnClickListener(v -> revokeTokens());
        findViewById(R.id.buttonOpenEmergencyQrScanner).setOnClickListener(v ->
                startActivity(new android.content.Intent(this, EmergencyQrScannerActivity.class)));
        findViewById(R.id.buttonOpenEmergencyQrAdmin).setOnClickListener(v ->
                startActivity(new android.content.Intent(this, EmergencyQrAdminActivity.class)));
        createToken();
    }

    private void revokeTokens() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        status.setText(R.string.emergency_qr_generating);
        user.getIdToken(false).addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) return;
            new Thread(() -> {
                try {
                    HttpURLConnection c = (HttpURLConnection) new URL(REVOKE_URL).openConnection();
                    c.setRequestMethod("POST"); c.setDoOutput(true); c.setConnectTimeout(15000); c.setReadTimeout(15000);
                    c.setRequestProperty("Authorization", "Bearer " + task.getResult().getToken());
                    c.getOutputStream().close();
                    c.getResponseCode(); c.disconnect();
                    runOnUiThread(() -> { qrImage.setImageDrawable(null); status.setText(R.string.emergency_qr_revoked); });
                } catch (Exception ignored) { runOnUiThread(() -> status.setText(R.string.emergency_qr_failed)); }
            }).start();
        });
    }

    private void createToken() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { status.setText(R.string.emergency_qr_login_required); return; }
        status.setText(R.string.emergency_qr_generating);
        user.getIdToken(false).addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                status.setText(R.string.emergency_qr_failed); return;
            }
            new Thread(() -> {
                try {
                    HttpURLConnection c = (HttpURLConnection) new URL(CREATE_URL).openConnection();
                    c.setRequestMethod("POST"); c.setDoOutput(true); c.setConnectTimeout(15000); c.setReadTimeout(15000);
                    c.setRequestProperty("Authorization", "Bearer " + task.getResult().getToken());
                    c.setRequestProperty("Accept", "application/json");
                    c.getOutputStream().close();
                    StringBuilder body = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
                        String line; while ((line = reader.readLine()) != null) body.append(line);
                    }
                    JSONObject json = new JSONObject(body.toString());
                    if (!json.optBoolean("ok")) throw new Exception("server_rejected");
                    String token = json.getString("token");
                    Bitmap bitmap = makeQr(token);
                    runOnUiThread(() -> { qrImage.setImageBitmap(bitmap); status.setText(getString(R.string.emergency_qr_expires, json.optString("expires_at"))); });
                    c.disconnect();
                } catch (Exception e) { runOnUiThread(() -> status.setText(R.string.emergency_qr_failed)); }
            }).start();
        });
    }

    private Bitmap makeQr(String value) throws Exception {
        BitMatrix matrix = new MultiFormatWriter().encode(value, BarcodeFormat.QR_CODE, 800, 800);
        Bitmap bitmap = Bitmap.createBitmap(800, 800, Bitmap.Config.ARGB_8888);
        for (int x=0; x<800; x++) for (int y=0; y<800; y++) bitmap.setPixel(x,y,matrix.get(x,y) ? 0xFF111111 : 0xFFFFFFFF);
        return bitmap;
    }
}
