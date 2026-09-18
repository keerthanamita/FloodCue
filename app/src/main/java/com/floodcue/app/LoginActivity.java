package com.floodcue.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.gms.common.SignInButton;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private SignInButton googleSignInButton;

    private TextView forgotPasswordText;
    private TextView createAccountText;

    private EditText emailEditText;
    private EditText passwordEditText;

    private Button loginButton;

    private TextInputLayout emailInputLayout;
    private TextInputLayout passwordInputLayout;

    private View rootView;

    private FirebaseAuth firebaseAuth;

    private SharedPreferences sharedPreferences;

    private CredentialManager credentialManager;
    private Executor credentialExecutor;

    private static final String PREFS_NAME = "FloodCuePrefs";
    private static final String LOGIN_KEY = "is_logged_in";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        rootView = findViewById(R.id.loginRoot);

        emailEditText = findViewById(R.id.editTextEmail);
        passwordEditText = findViewById(R.id.editTextPassword);

        loginButton = findViewById(R.id.buttonLogin);

        createAccountText = findViewById(R.id.textCreateAccount);
        forgotPasswordText = findViewById(R.id.textForgotPassword);

        googleSignInButton = findViewById(R.id.buttonGoogleSignIn);

        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);

        firebaseAuth = FirebaseAuth.getInstance();

        sharedPreferences = getSharedPreferences(
                PREFS_NAME,
                MODE_PRIVATE
        );

        credentialManager = CredentialManager.create(this);
        credentialExecutor = Executors.newSingleThreadExecutor();

        loginButton.setOnClickListener(v -> loginUser());

        forgotPasswordText.setOnClickListener(v -> resetPassword());

        createAccountText.setOnClickListener(v -> {
            Intent intent = new Intent(
                    LoginActivity.this,
                    CreateAccountActivity.class
            );

            startActivity(intent);
        });

        googleSignInButton.setOnClickListener(v -> signInWithGoogle());

        emailEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                emailInputLayout.setError(null);
            }
        });

        passwordEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                passwordInputLayout.setError(null);
            }
        });

        configureGoogleButton();

        animateEntrance();

        addPressAnimation(loginButton);
        addPressAnimation(googleSignInButton);
    }

    private void loginUser() {

        String email = emailEditText
                .getText()
                .toString()
                .trim();

        String password = passwordEditText
                .getText()
                .toString();

        emailInputLayout.setError(null);
        passwordInputLayout.setError(null);

        if (TextUtils.isEmpty(email)) {

            showError(
                    emailInputLayout,
                    emailEditText,
                    "Enter your email"
            );

            return;
        }

        if (!isValidEmail(email)) {

            showError(
                    emailInputLayout,
                    emailEditText,
                    "Enter a valid email address"
            );

            return;
        }

        if (TextUtils.isEmpty(password)) {

            showError(
                    passwordInputLayout,
                    passwordEditText,
                    "Enter your password"
            );

            return;
        }

        setLoginLoading(true);

        firebaseAuth
                .signInWithEmailAndPassword(
                        email,
                        password
                )
                .addOnCompleteListener(
                        this,
                        task -> {

                            if (!task.isSuccessful()) {

                                setLoginLoading(false);

                                showFirebaseLoginError(
                                        task.getException()
                                );

                                return;
                            }

                            FirebaseUser user =
                                    firebaseAuth.getCurrentUser();

                            if (user == null) {

                                setLoginLoading(false);

                                showMessage(
                                        "Login failed. Please try again."
                                );

                                return;
                            }

                            user.reload()
                                    .addOnCompleteListener(
                                            reloadTask -> {

                                                setLoginLoading(false);

                                                FirebaseUser refreshedUser =
                                                        firebaseAuth
                                                                .getCurrentUser();

                                                if (refreshedUser != null
                                                        && refreshedUser
                                                        .isEmailVerified()) {

                                                    sharedPreferences
                                                            .edit()
                                                            .putBoolean(
                                                                    LOGIN_KEY,
                                                                    true
                                                            )
                                                            .apply();

                                                    openNextScreen();

                                                } else {

                                                    showMessage(
                                                            "Please verify your email before logging in."
                                                    );
                                                }
                                            }
                                    );
                        }
                );
    }

    private void resetPassword() {

        String email = emailEditText
                .getText()
                .toString()
                .trim();

        emailInputLayout.setError(null);

        if (TextUtils.isEmpty(email)) {

            showError(
                    emailInputLayout,
                    emailEditText,
                    "Enter your email first"
            );

            return;
        }

        if (!isValidEmail(email)) {

            showError(
                    emailInputLayout,
                    emailEditText,
                    "Enter a valid email address"
            );

            return;
        }

        forgotPasswordText.setEnabled(false);
        forgotPasswordText.setAlpha(0.6f);

        firebaseAuth
                .sendPasswordResetEmail(email)
                .addOnCompleteListener(
                        task -> {

                            forgotPasswordText.setEnabled(true);
                            forgotPasswordText.setAlpha(1.0f);

                            if (task.isSuccessful()) {

                                showMessage(
                                        "✓ Password reset email sent."
                                );

                            } else {

                                showFirebaseResetError(
                                        task.getException()
                                );
                            }
                        }
                );
    }

    private void signInWithGoogle() {

        googleSignInButton.setEnabled(false);
        googleSignInButton.setAlpha(0.7f);

        GetGoogleIdOption googleIdOption =
                new GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(
                                getString(
                                        R.string.default_web_client_id
                                )
                        )
                        .build();

        GetCredentialRequest request =
                new GetCredentialRequest.Builder()
                        .addCredentialOption(
                                googleIdOption
                        )
                        .build();

        credentialManager.getCredentialAsync(
                this,
                request,
                new CancellationSignal(),
                credentialExecutor,
                new CredentialManagerCallback<
                        GetCredentialResponse,
                        GetCredentialException>() {

                    @Override
                    public void onResult(
                            @NonNull GetCredentialResponse result) {

                        runOnUiThread(() -> {

                            googleSignInButton.setEnabled(true);
                            googleSignInButton.setAlpha(1.0f);

                            handleCredential(
                                    result.getCredential()
                            );
                        });
                    }

                    @Override
                    public void onError(
                            @NonNull GetCredentialException e) {

                        runOnUiThread(() -> {

                            googleSignInButton.setEnabled(true);
                            googleSignInButton.setAlpha(1.0f);

                            showMessage(
                                    "Google sign-in cancelled or failed."
                            );
                        });
                    }
                }
        );
    }

    private void handleCredential(Credential credential) {

        if (credential instanceof CustomCredential) {

            CustomCredential customCredential =
                    (CustomCredential) credential;

            if (GoogleIdTokenCredential
                    .TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                    .equals(
                            customCredential.getType()
                    )) {

                try {

                    GoogleIdTokenCredential googleCredential =
                            GoogleIdTokenCredential.createFrom(
                                    customCredential.getData()
                            );

                    String idToken =
                            googleCredential.getIdToken();

                    if (TextUtils.isEmpty(idToken)) {

                        showMessage(
                                "Google ID token was not received."
                        );

                        return;
                    }

                    firebaseAuthWithGoogle(idToken);

                } catch (Exception e) {

                    showMessage(
                            "Unable to process Google sign-in."
                    );
                }

            } else {

                showMessage(
                        "Unexpected credential type."
                );
            }

        } else {

            showMessage(
                    "Google account credential not received."
            );
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {

        AuthCredential credential =
                GoogleAuthProvider.getCredential(
                        idToken,
                        null
                );

        firebaseAuth
                .signInWithCredential(credential)
                .addOnCompleteListener(
                        this,
                        task -> {

                            if (task.isSuccessful()) {

                                FirebaseUser user =
                                        firebaseAuth.getCurrentUser();

                                if (user != null) {

                                    sharedPreferences
                                            .edit()
                                            .putBoolean(
                                                    LOGIN_KEY,
                                                    true
                                            )
                                            .apply();

                                    openNextScreen();

                                } else {

                                    showMessage(
                                            "Google login failed. Please try again."
                                    );
                                }

                            } else {

                                showFirebaseGoogleError(
                                        task.getException()
                                );
                            }
                        }
                );
    }

    private void showFirebaseLoginError(Exception exception) {

        String message =
                "Login failed. Please try again.";

        if (exception != null
                && exception.getMessage() != null) {

            String firebaseMessage =
                    exception.getMessage()
                            .toLowerCase();

            if (firebaseMessage.contains("invalid-credential")
                    || firebaseMessage.contains("invalid credential")
                    || firebaseMessage.contains("invalid-email")
                    || firebaseMessage.contains("wrong-password")
                    || firebaseMessage.contains("user-not-found")) {

                message =
                        "Incorrect email or password.";

            } else if (firebaseMessage.contains("network")) {

                message =
                        "Network error. Please check your internet connection.";

            } else if (firebaseMessage.contains("too-many-requests")
                    || firebaseMessage.contains("too many requests")) {

                message =
                        "Too many attempts. Please try again later.";

            } else if (firebaseMessage.contains("user-disabled")
                    || firebaseMessage.contains("disabled")) {

                message =
                        "This account has been disabled.";

            } else if (!TextUtils.isEmpty(
                    exception.getMessage()
            )) {

                message =
                        exception.getMessage();
            }
        }

        showMessage(message);
    }

    private void showFirebaseResetError(Exception exception) {

        String message =
                "Failed to send password reset email.";

        if (exception != null
                && exception.getMessage() != null) {

            String firebaseMessage =
                    exception.getMessage()
                            .toLowerCase();

            if (firebaseMessage.contains("network")) {

                message =
                        "Network error. Please check your internet connection.";

            } else if (firebaseMessage.contains("user-not-found")
                    || firebaseMessage.contains("user not found")) {

                message =
                        "No account was found with this email.";

            } else if (firebaseMessage.contains("invalid-email")) {

                message =
                        "Please enter a valid email address.";

            } else if (firebaseMessage.contains("too-many-requests")
                    || firebaseMessage.contains("too many requests")) {

                message =
                        "Too many attempts. Please try again later.";
            }
        }

        showMessage(message);
    }

    private void showFirebaseGoogleError(Exception exception) {

        String message =
                "Google authentication failed. Please try again.";

        if (exception != null
                && exception.getMessage() != null) {

            String firebaseMessage =
                    exception.getMessage()
                            .toLowerCase();

            if (firebaseMessage.contains("network")) {

                message =
                        "Network error. Please check your internet connection.";

            } else if (firebaseMessage.contains("too-many-requests")
                    || firebaseMessage.contains("too many requests")) {

                message =
                        "Too many attempts. Please try again later.";
            }
        }

        showMessage(message);
    }

    private boolean isValidEmail(String email) {

        if (email.length() > 254) {
            return false;
        }

        if (email.contains(" ")) {
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()) {

            return false;
        }

        if (email.contains("..")) {
            return false;
        }

        if (email.startsWith(".")
                || email.endsWith(".")) {

            return false;
        }

        int atIndex = email.indexOf("@");

        if (atIndex <= 0) {
            return false;
        }

        String localPart =
                email.substring(
                        0,
                        atIndex
                );

        if (localPart.startsWith(".")
                || localPart.endsWith(".")) {

            return false;
        }

        return true;
    }

    private void showError(
            TextInputLayout inputLayout,
            EditText editText,
            String message
    ) {

        inputLayout.setError(message);
        editText.requestFocus();
    }

    private void setLoginLoading(boolean loading) {

        loginButton.setEnabled(!loading);

        if (loading) {

            loginButton.setText(
                    "Signing in..."
            );

            loginButton.setAlpha(0.7f);

        } else {

            loginButton.setText(
                    "Sign in"
            );

            loginButton.setAlpha(1.0f);
        }
    }

    private void showMessage(String message) {

        if (rootView != null) {

            Snackbar.make(
                    rootView,
                    message,
                    Snackbar.LENGTH_LONG
            ).show();

        } else {

            android.widget.Toast.makeText(
                    this,
                    message,
                    android.widget.Toast.LENGTH_LONG
            ).show();
        }
    }

    private void configureGoogleButton() {

        if (googleSignInButton == null) {
            return;
        }

        googleSignInButton.setSize(
                SignInButton.SIZE_WIDE
        );

        int currentNightMode =
                getResources()
                        .getConfiguration()
                        .uiMode
                        & Configuration.UI_MODE_NIGHT_MASK;

        if (currentNightMode
                == Configuration.UI_MODE_NIGHT_YES) {

            googleSignInButton.setColorScheme(
                    SignInButton.COLOR_DARK
            );

        } else {

            googleSignInButton.setColorScheme(
                    SignInButton.COLOR_LIGHT
            );
        }
    }

    private void animateEntrance() {

        if (rootView == null) {
            return;
        }

        rootView.setAlpha(0f);
        rootView.setTranslationY(20f);

        rootView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(250)
                .start();
    }

    private void addPressAnimation(View view) {

        if (view == null) {
            return;
        }

        view.setOnTouchListener(
                (v, event) -> {

                    switch (event.getAction()) {

                        case MotionEvent.ACTION_DOWN:

                            v.animate()
                                    .scaleX(0.98f)
                                    .scaleY(0.98f)
                                    .setDuration(80)
                                    .start();

                            break;

                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:

                            v.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(80)
                                    .start();

                            break;
                    }

                    return false;
                }
        );
    }

    private void openNextScreen() {

        boolean setupCompleted =
                sharedPreferences.getBoolean(
                        "setup_completed",
                        false
                );

        Intent intent;

        if (setupCompleted) {

            intent =
                    new Intent(
                            LoginActivity.this,
                            HomeActivity.class
                    );

        } else {

            intent =
                    new Intent(
                            LoginActivity.this,
                            SetupActivity.class
                    );
        }

        startActivity(intent);

        finish();

        overridePendingTransition(
                android.R.anim.fade_in,
                android.R.anim.fade_out
        );
    }

    @Override
    protected void onDestroy() {

        if (credentialExecutor
                instanceof ExecutorService) {

            ((ExecutorService)
                    credentialExecutor)
                    .shutdown();
        }

        super.onDestroy();
    }
}