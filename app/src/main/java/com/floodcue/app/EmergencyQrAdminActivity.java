package com.floodcue.app;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class EmergencyQrAdminActivity extends FloodCueActivity {
    private static final String AUTHORIZE_URL = "https://floodcue.heliohost.us/server/api/authorize_emergency_scanner.php";
    private static final String REVOKE_URL = "https://floodcue.heliohost.us/server/api/revoke_emergency_scanner.php";
    private EditText uidInput;
    private TextView status;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_emergency_qr_admin);
        uidInput = findViewById(R.id.emergencyQrAdminUid);
        status = findViewById(R.id.emergencyQrAdminStatus);
        findViewById(R.id.buttonBackFromEmergencyQrAdmin).setOnClickListener(v -> finish());
        findViewById(R.id.buttonAuthorizeEmergencyScanner).setOnClickListener(v -> send(AUTHORIZE_URL, true));
        findViewById(R.id.buttonRevokeEmergencyScanner).setOnClickListener(v -> send(REVOKE_URL, false));
    }

    private void send(String endpoint, boolean authorize) {
        String uid = uidInput.getText().toString().trim();
        if (uid.isEmpty()) { status.setText(R.string.emergency_qr_admin_uid_required); return; }
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { status.setText(R.string.emergency_qr_login_required); return; }
        status.setText(R.string.emergency_qr_admin_working);
        user.getIdToken(false).addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) { status.setText(R.string.emergency_qr_failed); return; }
            new Thread(() -> {
                try {
                    HttpURLConnection c=(HttpURLConnection)new URL(endpoint).openConnection();
                    c.setRequestMethod("POST"); c.setDoOutput(true); c.setConnectTimeout(15000); c.setReadTimeout(15000);
                    c.setRequestProperty("Authorization", "Bearer " + task.getResult().getToken());
                    c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    try (OutputStream out=c.getOutputStream()) { out.write(new JSONObject().put("firebase_uid",uid).toString().getBytes(StandardCharsets.UTF_8)); }
                    int code=c.getResponseCode(); BufferedReader reader=new BufferedReader(new InputStreamReader(code<400?c.getInputStream():c.getErrorStream(),StandardCharsets.UTF_8));
                    StringBuilder body=new StringBuilder(); String line; while((line=reader.readLine())!=null) body.append(line);
                    JSONObject response=new JSONObject(body.toString()); if(!response.optBoolean("ok")) throw new Exception(response.optString("error","request_failed"));
                    runOnUiThread(() -> status.setText(authorize ? R.string.emergency_qr_admin_authorized : R.string.emergency_qr_admin_revoked)); c.disconnect();
                } catch(Exception e) { runOnUiThread(() -> status.setText(getString(R.string.emergency_qr_admin_failed,e.getMessage()))); }
            }).start();
        });
    }
}
