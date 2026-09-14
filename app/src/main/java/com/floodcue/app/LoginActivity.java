package com.floodcue.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private TextView forgotPasswordText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView createAccountText;

    private FirebaseAuth firebaseAuth;
    private SharedPreferences sharedPreferences;

    private static final String PREFS_NAME = "FloodCuePrefs";
    private static final String LOGIN_KEY = "is_logged_in";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        // Connect XML views
        emailEditText = findViewById(R.id.editTextEmail);
        passwordEditText = findViewById(R.id.editTextPassword);
        loginButton = findViewById(R.id.buttonLogin);
        createAccountText = findViewById(R.id.textCreateAccount);
        forgotPasswordText = findViewById(R.id.textForgotPassword);

        // Initialize Firebase Authentication
        firebaseAuth = FirebaseAuth.getInstance();

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(
                PREFS_NAME,
                MODE_PRIVATE
        );

        // Login button
        loginButton.setOnClickListener(v -> loginUser());

        // Forgot Password
        forgotPasswordText.setOnClickListener(v -> resetPassword());

        // Create Account navigation
        createAccountText.setOnClickListener(v -> {

            Intent intent = new Intent(
                    LoginActivity.this,
                    CreateAccountActivity.class
            );

            startActivity(intent);
        });
    }

    private void loginUser() {

        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();

        // Check email
        if (TextUtils.isEmpty(email)) {

            emailEditText.setError("Enter your email");
            emailEditText.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {

            emailEditText.setError("Enter a valid email address");
            emailEditText.requestFocus();
            return;
        }

        // Check password
        if (TextUtils.isEmpty(password)) {

            passwordEditText.setError("Enter your password");
            passwordEditText.requestFocus();
            return;
        }

        // Disable button while logging in
        loginButton.setEnabled(false);

        // Firebase login
        firebaseAuth
                .signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        FirebaseUser user =
                                firebaseAuth.getCurrentUser();

                        if (user != null) {

                            // Refresh Firebase user information
                            user.reload().addOnCompleteListener(
                                    reloadTask -> {

                                        if (user.isEmailVerified()) {

                                            // Save login state
                                            sharedPreferences
                                                    .edit()
                                                    .putBoolean(
                                                            LOGIN_KEY,
                                                            true
                                                    )
                                                    .apply();

                                            Toast.makeText(
                                                    LoginActivity.this,
                                                    "Login successful",
                                                    Toast.LENGTH_SHORT
                                            ).show();

                                            // Check whether setup is completed
                                            boolean setupCompleted =
                                                    sharedPreferences.getBoolean(
                                                            "setup_completed",
                                                            false
                                                    );

                                            Intent intent;

                                            if (setupCompleted) {

                                                // Setup already completed
                                                intent = new Intent(
                                                        LoginActivity.this,
                                                        HomeActivity.class
                                                );

                                            } else {

                                                // Setup not completed
                                                intent = new Intent(
                                                        LoginActivity.this,
                                                        SetupActivity.class
                                                );
                                            }

                                            startActivity(intent);
                                            finish();

                                        } else {

                                            loginButton.setEnabled(true);

                                            Toast.makeText(
                                                    LoginActivity.this,
                                                    "Please verify your email before logging in.",
                                                    Toast.LENGTH_LONG
                                            ).show();
                                        }
                                    }
                            );

                        } else {

                            loginButton.setEnabled(true);

                            Toast.makeText(
                                    LoginActivity.this,
                                    "Unable to get user information.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }

                    } else {

                        loginButton.setEnabled(true);

                        String errorMessage;

                        if (task.getException() != null) {

                            errorMessage =
                                    task.getException().getMessage();

                        } else {

                            errorMessage = "Login failed";
                        }

                        Toast.makeText(
                                LoginActivity.this,
                                errorMessage,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void resetPassword() {

        String email =
                emailEditText.getText().toString().trim();

        // Check email
        if (TextUtils.isEmpty(email)) {

            emailEditText.setError(
                    "Enter your email first"
            );

            emailEditText.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {

            emailEditText.setError(
                    "Enter a valid email address"
            );

            emailEditText.requestFocus();
            return;
        }

        // Send password reset email
        firebaseAuth
                .sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        Toast.makeText(
                                LoginActivity.this,
                                "Password reset email sent. Check your inbox or spam folder.",
                                Toast.LENGTH_LONG
                        ).show();

                    } else {

                        Toast.makeText(
                                LoginActivity.this,
                                "Unable to send password reset email.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }
}