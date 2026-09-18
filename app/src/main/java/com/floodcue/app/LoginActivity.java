package com.floodcue.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private SignInButton googleSignInButton;
    private TextView forgotPasswordText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView createAccountText;

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

        emailEditText = findViewById(R.id.editTextEmail);
        passwordEditText = findViewById(R.id.editTextPassword);
        loginButton = findViewById(R.id.buttonLogin);
        createAccountText = findViewById(R.id.textCreateAccount);
        forgotPasswordText = findViewById(R.id.textForgotPassword);
        googleSignInButton = findViewById(R.id.buttonGoogleSignIn);

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
    }

    private void loginUser() {

        String email =
                emailEditText.getText().toString().trim();

        String password =
                passwordEditText.getText().toString();

        if (email.isEmpty()) {
            emailEditText.setError("Enter your email");
            emailEditText.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Enter a valid email");
            emailEditText.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            passwordEditText.setError("Enter your password");
            passwordEditText.requestFocus();
            return;
        }

        loginButton.setEnabled(false);

        firebaseAuth
                .signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    loginButton.setEnabled(true);

                    if (task.isSuccessful()) {

                        FirebaseUser user =
                                firebaseAuth.getCurrentUser();

                        if (user == null) {
                            Toast.makeText(
                                    LoginActivity.this,
                                    "Login failed. Please try again.",
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }

                        user.reload().addOnCompleteListener(
                                reloadTask -> {

                                    FirebaseUser refreshedUser =
                                            firebaseAuth.getCurrentUser();

                                    if (refreshedUser != null
                                            && refreshedUser
                                            .isEmailVerified()) {

                                        sharedPreferences.edit()
                                                .putBoolean(
                                                        LOGIN_KEY,
                                                        true
                                                )
                                                .apply();

                                        openNextScreen();

                                    } else {
                                        Toast.makeText(
                                                LoginActivity.this,
                                                "Please verify your email before logging in.",
                                                Toast.LENGTH_LONG
                                        ).show();
                                    }
                                }
                        );

                    } else {

                        String message = "Login failed";

                        if (task.getException() != null) {
                            message =
                                    task.getException().getMessage();
                        }

                        Toast.makeText(
                                LoginActivity.this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void resetPassword() {

        String email =
                emailEditText.getText().toString().trim();

        if (email.isEmpty()) {
            emailEditText.setError(
                    "Enter your email first"
            );
            emailEditText.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError(
                    "Enter a valid email"
            );
            emailEditText.requestFocus();
            return;
        }

        firebaseAuth
                .sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        Toast.makeText(
                                LoginActivity.this,
                                "Password reset email sent.",
                                Toast.LENGTH_LONG
                        ).show();

                    } else {

                        String message =
                                "Failed to send reset email";

                        if (task.getException() != null) {
                            message =
                                    task.getException().getMessage();
                        }

                        Toast.makeText(
                                LoginActivity.this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void signInWithGoogle() {

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

                        runOnUiThread(() ->
                                handleCredential(
                                        result.getCredential()
                                )
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull GetCredentialException e) {

                        runOnUiThread(() ->
                                Toast.makeText(
                                        LoginActivity.this,
                                        "Google sign-in cancelled or failed.",
                                        Toast.LENGTH_SHORT
                                ).show()
                        );
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

                GoogleIdTokenCredential googleCredential =
                        GoogleIdTokenCredential.createFrom(
                                customCredential.getData()
                        );

                String idToken =
                        googleCredential.getIdToken();

                firebaseAuthWithGoogle(idToken);

            } else {

                Toast.makeText(
                        this,
                        "Unexpected credential type.",
                        Toast.LENGTH_SHORT
                ).show();
            }

        } else {

            Toast.makeText(
                    this,
                    "Google account credential not received.",
                    Toast.LENGTH_SHORT
            ).show();
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
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        FirebaseUser user =
                                firebaseAuth.getCurrentUser();

                        if (user != null) {

                            sharedPreferences.edit()
                                    .putBoolean(
                                            LOGIN_KEY,
                                            true
                                    )
                                    .apply();

                            openNextScreen();

                        } else {

                            Toast.makeText(
                                    LoginActivity.this,
                                    "Google login failed.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }

                    } else {

                        String message =
                                "Firebase Google authentication failed";

                        if (task.getException() != null) {
                            message =
                                    task.getException().getMessage();
                        }

                        Toast.makeText(
                                LoginActivity.this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void openNextScreen() {

        boolean setupCompleted =
                sharedPreferences.getBoolean(
                        "setup_completed",
                        false
                );

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
        finish();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        if (credentialExecutor
                instanceof java.util.concurrent.ExecutorService) {

            ((java.util.concurrent.ExecutorService)
                    credentialExecutor)
                    .shutdown();
        }
    }
}