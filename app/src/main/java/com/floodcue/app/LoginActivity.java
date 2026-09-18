package com.floodcue.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;

import com.google.android.gms.common.SignInButton;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout emailInputLayout;
    private TextInputLayout passwordInputLayout;

    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;

    private MaterialButton loginButton;
    private SignInButton googleSignInButton;

    private TextView forgotPasswordText;
    private TextView createAccountText;

    private FirebaseAuth firebaseAuth;

    private android.content.SharedPreferences prefs;

    private CredentialManager credentialManager;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        View loginRoot = findViewById(R.id.loginRoot);

        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);

        emailEditText = findViewById(R.id.editTextEmail);
        passwordEditText = findViewById(R.id.editTextPassword);

        loginButton = findViewById(R.id.buttonLogin);
        googleSignInButton = findViewById(R.id.buttonGoogleSignIn);

        forgotPasswordText = findViewById(R.id.textForgotPassword);
        createAccountText = findViewById(R.id.textCreateAccount);

        firebaseAuth = FirebaseAuth.getInstance();

        prefs = getSharedPreferences("FloodCuePrefs", MODE_PRIVATE);

        credentialManager = CredentialManager.create(this);
        executorService = Executors.newSingleThreadExecutor();

        /*
         * Google's original Sign-In button.
         *
         * SIZE_WIDE gives the normal full-width Google button.
         * COLOR_AUTO allows Google Play services to select
         * the appropriate light/dark appearance.
         */
        googleSignInButton.setStyle(
                SignInButton.SIZE_WIDE,
                SignInButton.COLOR_AUTO
        );

        // Login button
        loginButton.setOnClickListener(v -> {
            addClickEffect(loginButton);
            loginUser();
        });

        // Forgot password
        forgotPasswordText.setOnClickListener(v -> {
            addClickEffect(forgotPasswordText);
            resetPassword();
        });

        // Create account
        createAccountText.setOnClickListener(v -> {
            addClickEffect(createAccountText);

            Intent intent = new Intent(
                    LoginActivity.this,
                    CreateAccountActivity.class
            );

            startActivity(intent);

            overridePendingTransition(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
            );
        });

        // Google Sign-In
        googleSignInButton.setOnClickListener(v -> {
            addClickEffect(googleSignInButton);
            signInWithGoogle();
        });

        // Focus effects for email
        emailEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                emailInputLayout.setBoxStrokeWidth(2);
            } else {
                emailInputLayout.setBoxStrokeWidth(1);
            }
        });

        // Focus effects for password
        passwordEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                passwordInputLayout.setBoxStrokeWidth(2);
            } else {
                passwordInputLayout.setBoxStrokeWidth(1);
            }
        });

        // Entrance animation
        animateEntrance(loginRoot);

        // Press animation
        addPressAnimation(loginButton);
        addPressAnimation(googleSignInButton);
    }

    // ---------------------------------------------------------
    // EMAIL / PASSWORD LOGIN
    // ---------------------------------------------------------

    private void loginUser() {

        String email = emailEditText.getText() == null
                ? ""
                : emailEditText.getText().toString().trim();

        String password = passwordEditText.getText() == null
                ? ""
                : passwordEditText.getText().toString();

        clearErrors();

        boolean valid = true;

        if (TextUtils.isEmpty(email)) {
            emailInputLayout.setError("Enter your email");
            valid = false;
        } else if (!isValidEmail(email)) {
            emailInputLayout.setError("Enter a valid email");
            valid = false;
        }

        if (TextUtils.isEmpty(password)) {
            passwordInputLayout.setError("Enter your password");
            valid = false;
        }

        if (!valid) {
            return;
        }

        setLoginLoading(true);

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (!task.isSuccessful()) {

                        setLoginLoading(false);

                        String message = "Login failed";

                        if (task.getException() != null) {
                            String error = task.getException()
                                    .getMessage();

                            if (error != null) {
                                if (error.contains("password is invalid")
                                        || error.contains("INVALID_LOGIN_CREDENTIALS")
                                        || error.contains("no user record")) {

                                    message = "Incorrect email or password";

                                } else if (error.contains("network")) {

                                    message = "Check your internet connection";

                                } else {
                                    message = error;
                                }
                            }
                        }

                        showError(message);
                        return;
                    }

                    FirebaseUser user = firebaseAuth.getCurrentUser();

                    if (user == null) {
                        setLoginLoading(false);
                        showError("Unable to get account information");
                        return;
                    }

                    /*
                     * Reload the user so Firebase gives us
                     * the latest email verification state.
                     */
                    user.reload()
                            .addOnCompleteListener(reloadTask -> {

                                setLoginLoading(false);

                                FirebaseUser refreshedUser =
                                        firebaseAuth.getCurrentUser();

                                if (refreshedUser == null) {
                                    showError(
                                            "Unable to get account information"
                                    );
                                    return;
                                }

                                if (!refreshedUser.isEmailVerified()) {

                                    showMessage(
                                            "Please verify your email before logging in."
                                    );

                                    return;
                                }

                                prefs.edit()
                                        .putBoolean("is_logged_in", true)
                                        .apply();

                                openNextScreen();
                            });
                });
    }

    // ---------------------------------------------------------
    // FORGOT PASSWORD
    // ---------------------------------------------------------

    private void resetPassword() {

        String email = emailEditText.getText() == null
                ? ""
                : emailEditText.getText().toString().trim();

        emailInputLayout.setError(null);

        if (TextUtils.isEmpty(email)) {
            emailInputLayout.setError("Enter your email first");
            return;
        }

        if (!isValidEmail(email)) {
            emailInputLayout.setError("Enter a valid email");
            return;
        }

        forgotPasswordText.setEnabled(false);
        forgotPasswordText.setAlpha(0.6f);

        firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {

                    forgotPasswordText.setEnabled(true);
                    forgotPasswordText.setAlpha(1f);

                    if (task.isSuccessful()) {

                        showMessage(
                                "Password reset email sent. Check your inbox."
                        );

                    } else {

                        String message = "Unable to send reset email";

                        if (task.getException() != null
                                && task.getException().getMessage() != null) {

                            message = task.getException().getMessage();
                        }

                        showError(message);
                    }
                });
    }

    // ---------------------------------------------------------
    // GOOGLE SIGN-IN
    // ---------------------------------------------------------

    private void signInWithGoogle() {

        googleSignInButton.setEnabled(false);
        googleSignInButton.setAlpha(0.65f);

        GetGoogleIdOption googleIdOption =
                new GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(
                                getString(R.string.default_web_client_id)
                        )
                        .build();

        GetCredentialRequest request =
                new GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build();

        credentialManager.getCredentialAsync(
                this,
                request,
                null,
                executorService,
                new androidx.credentials.CredentialManagerCallback<
                        GetCredentialResponse,
                        androidx.credentials.exceptions.GetCredentialException>() {

                    @Override
                    public void onResult(
                            GetCredentialResponse result) {

                        runOnUiThread(() -> {
                            googleSignInButton.setEnabled(true);
                            googleSignInButton.setAlpha(1f);
                        });

                        handleCredential(result.getCredential());
                    }

                    @Override
                    public void onError(
                            androidx.credentials.exceptions.GetCredentialException e) {

                        runOnUiThread(() -> {
                            googleSignInButton.setEnabled(true);
                            googleSignInButton.setAlpha(1f);

                            String message = e.getMessage();

                            if (message == null
                                    || message.trim().isEmpty()) {
                                message = "Google sign-in was cancelled";
                            }

                            showMessage(message);
                        });
                    }
                }
        );
    }

    private void handleCredential(Credential credential) {

        if (credential instanceof CustomCredential) {

            CustomCredential customCredential =
                    (CustomCredential) credential;

            if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                    .equals(customCredential.getType())) {

                try {

                    GoogleIdTokenCredential googleCredential =
                            GoogleIdTokenCredential
                                    .createFrom(customCredential.getData());

                    String idToken = googleCredential.getIdToken();

                    firebaseAuthWithGoogle(idToken);

                } catch (Exception e) {

                    showError(
                            "Unable to process Google sign-in"
                    );
                }

            } else {

                showError("Unexpected Google credential");
            }

        } else {

            showError("Unexpected credential type");
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {

        AuthCredential credential =
                GoogleAuthProvider.getCredential(idToken, null);

        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        prefs.edit()
                                .putBoolean("is_logged_in", true)
                                .apply();

                        openNextScreen();

                    } else {

                        String message =
                                "Google authentication failed";

                        if (task.getException() != null
                                && task.getException().getMessage() != null) {

                            message =
                                    task.getException().getMessage();
                        }

                        showError(message);
                    }
                });
    }

    // ---------------------------------------------------------
    // VALIDATION
    // ---------------------------------------------------------

    private boolean isValidEmail(String email) {

        return !TextUtils.isEmpty(email)
                && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void clearErrors() {

        emailInputLayout.setError(null);
        passwordInputLayout.setError(null);
    }

    // ---------------------------------------------------------
    // LOADING
    // ---------------------------------------------------------

    private void setLoginLoading(boolean loading) {

        loginButton.setEnabled(!loading);
        googleSignInButton.setEnabled(!loading);

        if (loading) {

            loginButton.setText("Logging in...");
            loginButton.setAlpha(0.7f);
            googleSignInButton.setAlpha(0.6f);

        } else {

            loginButton.setText("Login");
            loginButton.setAlpha(1f);
            googleSignInButton.setAlpha(1f);
        }
    }

    // ---------------------------------------------------------
    // MESSAGES
    // ---------------------------------------------------------

    private void showError(String message) {

        View root = findViewById(R.id.loginRoot);

        Snackbar snackbar =
                Snackbar.make(
                        root,
                        message,
                        Snackbar.LENGTH_LONG
                );

        snackbar.setBackgroundTint(
                getColor(R.color.error_color)
        );

        snackbar.show();
    }

    private void showMessage(String message) {

        View root = findViewById(R.id.loginRoot);

        Snackbar.make(
                root,
                message,
                Snackbar.LENGTH_LONG
        ).show();
    }

    // ---------------------------------------------------------
    // ANIMATIONS
    // ---------------------------------------------------------

    private void animateEntrance(View view) {

        if (view == null) {
            return;
        }

        view.setAlpha(0f);
        view.setTranslationY(18f);

        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(350)
                .start();
    }

    private void addClickEffect(View view) {

        if (view == null) {
            return;
        }

        view.animate()
                .scaleX(0.96f)
                .scaleY(0.96f)
                .alpha(0.65f)
                .setDuration(70)
                .withEndAction(() ->
                        view.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .alpha(1f)
                                .setDuration(120)
                                .start()
                )
                .start();
    }

    private void addPressAnimation(View view) {

        if (view == null) {
            return;
        }

        view.setOnTouchListener((v, event) -> {

            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:

                    v.animate()
                            .scaleX(0.98f)
                            .scaleY(0.98f)
                            .setDuration(70)
                            .start();

                    break;

                case MotionEvent.ACTION_UP:

                    v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(90)
                            .start();

                    v.performClick();

                    break;

                case MotionEvent.ACTION_CANCEL:

                    v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(90)
                            .start();

                    break;
            }

            return false;
        });
    }

    // ---------------------------------------------------------
    // NEXT SCREEN
    // ---------------------------------------------------------

    private void openNextScreen() {

        boolean setupCompleted =
                prefs.getBoolean("setup_completed", false);

        Intent intent;

        if (setupCompleted) {

            intent = new Intent(
                    LoginActivity.this,
                    HomeActivity.class
            );

        } else {

            intent = new Intent(
                    LoginActivity.this,
                    SetupActivity.class
            );
        }

        startActivity(intent);

        overridePendingTransition(
                android.R.anim.fade_in,
                android.R.anim.fade_out
        );

        finish();
    }

    // ---------------------------------------------------------
    // CLEANUP
    // ---------------------------------------------------------

    @Override
    protected void onDestroy() {

        super.onDestroy();

        if (executorService != null
                && !executorService.isShutdown()) {

            executorService.shutdown();
        }
    }
}