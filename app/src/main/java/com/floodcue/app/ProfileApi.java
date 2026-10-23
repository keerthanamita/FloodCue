
package com.floodcue.app;

import android.os.Handler;
import android.os.Looper;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ProfileApi {

    private static final String BASE_URL =
            "https://floodcue.heliohost.us/server/api/";

    private static final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private static final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    private ProfileApi() {
    }

    public interface FetchCallback {
        void onResult(
                boolean success,
                boolean exists,
                UserProfile profile,
                String error
        );
    }

    public interface SaveCallback {
        void onResult(
                boolean success,
                UserProfile savedProfile,
                String error
        );
    }

    public static void fetchProfile(FetchCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            callback.onResult(false, false, null, "not_logged_in");
            return;
        }

        user.getIdToken(false).addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                callback.onResult(false, false, null, "token_unavailable");
                return;
            }

            String token = task.getResult().getToken();

            if (token == null || token.isEmpty()) {
                callback.onResult(false, false, null, "token_unavailable");
                return;
            }

            executor.execute(() -> {
                try {
                    JSONObject response = request(
                            "GET",
                            "get_profile.php",
                            token,
                            null
                    );

                    if (!response.optBoolean("ok", false)) {
                        String error = response.optString(
                                "error", "profile_fetch_failed"
                        );

                        mainHandler.post(() ->
                                callback.onResult(false, false, null, error)
                        );
                        return;
                    }

                    boolean exists = response.optBoolean("exists", false);

                    if (!exists) {
                        mainHandler.post(() ->
                                callback.onResult(true, false, null, null)
                        );
                        return;
                    }

                    JSONObject profileJson = response.optJSONObject("profile");

                    if (profileJson == null) {
                        mainHandler.post(() ->
                                callback.onResult(
                                        false, false, null, "invalid_profile_response"
                                )
                        );
                        return;
                    }

                    UserProfile profile = UserProfile.fromJson(profileJson);

                    mainHandler.post(() ->
                            callback.onResult(true, true, profile, null)
                    );

                } catch (Exception e) {
                    mainHandler.post(() ->
                            callback.onResult(
                                    false, false, null, safeError(e)
                            )
                    );
                }
            });
        });
    }

    public static void saveProfile(
            UserProfile profile,
            SaveCallback callback
    ) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            callback.onResult(false, null, "not_logged_in");
            return;
        }

        if (profile == null) {
            callback.onResult(false, null, "profile_missing");
            return;
        }

        user.getIdToken(false).addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                callback.onResult(false, null, "token_unavailable");
                return;
            }

            String token = task.getResult().getToken();

            if (token == null || token.isEmpty()) {
                callback.onResult(false, null, "token_unavailable");
                return;
            }

            executor.execute(() -> {
                try {
                    JSONObject body = new JSONObject();
                    body.put("profile", profile.toJson());

                    JSONObject response = request(
                            "POST",
                            "save_profile.php",
                            token,
                            body
                    );

                    if (!response.optBoolean("ok", false)) {
                        String error = response.optString(
                                "error", "profile_save_failed"
                        );

                        mainHandler.post(() ->
                                callback.onResult(false, null, error)
                        );
                        return;
                    }

                    JSONObject savedJson = response.optJSONObject("profile");

                    if (savedJson == null) {
                        mainHandler.post(() ->
                                callback.onResult(
                                        false, null, "invalid_save_response"
                                )
                        );
                        return;
                    }

                    UserProfile savedProfile =
                            UserProfile.fromJson(savedJson);

                    savedProfile.setup_completed =
                            response.optBoolean("setup_completed", false);

                    if (!savedProfile.setup_completed) {
                        mainHandler.post(() ->
                                callback.onResult(
                                        false, null, "setup_not_completed"
                                )
                        );
                        return;
                    }

                    mainHandler.post(() ->
                            callback.onResult(true, savedProfile, null)
                    );

                } catch (Exception e) {
                    mainHandler.post(() ->
                            callback.onResult(false, null, safeError(e))
                    );
                }
            });
        });
    }

    private static JSONObject request(
            String method,
            String endpoint,
            String token,
            JSONObject body
    ) throws Exception {

        HttpURLConnection connection = null;

        try {
            URL url = new URL(BASE_URL + endpoint);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod(method);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setUseCaches(false);
            connection.setRequestProperty(
                    "Authorization", "Bearer " + token
            );
            connection.setRequestProperty(
                    "Accept", "application/json"
            );

            if ("POST".equals(method)) {
                connection.setDoOutput(true);
                connection.setRequestProperty(
                        "Content-Type", "application/json; charset=UTF-8"
                );

                try (OutputStream output = connection.getOutputStream();
                     BufferedWriter writer = new BufferedWriter(
                             new OutputStreamWriter(
                                     output, StandardCharsets.UTF_8
                             )
                     )) {
                    writer.write(body.toString());
                    writer.flush();
                }
            }

            int status = connection.getResponseCode();

            InputStream input = status >= 200 && status < 400
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            if (input == null) {
                throw new Exception("HTTP " + status);
            }

            StringBuilder responseText = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(input, StandardCharsets.UTF_8)
            )) {
                String line;

                while ((line = reader.readLine()) != null) {
                    responseText.append(line);
                }
            }

            JSONObject response;

            try {
                response = new JSONObject(responseText.toString());
            } catch (JSONException e) {
                throw new Exception("Invalid server response (HTTP " + status + ")");
            }

            if (status < 200 || status >= 300) {
                String serverError = response.optString(
                        "error", "http_" + status
                );
                throw new Exception(serverError);
            }

            return response;

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String safeError(Exception e) {
        String message = e.getMessage();

        if (message == null || message.trim().isEmpty()) {
            return "network_error";
        }

        return message;
    }
}
