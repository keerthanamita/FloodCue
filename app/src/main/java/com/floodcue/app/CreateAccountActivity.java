package com.floodcue.app;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class CreateAccountActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private Button createAccountButton;

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_create_account);

        // Connect XML views
        emailEditText = findViewById(R.id.editTextEmail);
        passwordEditText = findViewById(R.id.editTextPassword);
        confirmPasswordEditText = findViewById(R.id.editTextConfirmPassword);
        createAccountButton = findViewById(R.id.buttonCreateAccount);

        // Initialize Firebase Authentication
        firebaseAuth = FirebaseAuth.getInstance();

        // Create Account button
        createAccountButton.setOnClickListener(v -> createAccount());
    }

    private void createAccount() {

        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        String confirmPassword =
                confirmPasswordEditText.getText().toString();

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
            passwordEditText.setError("Enter a password");
            passwordEditText.requestFocus();
            return;
        }

        if (password.length() < 6) {
            passwordEditText.setError(
                    "Password must contain at least 6 characters"
            );
            passwordEditText.requestFocus();
            return;
        }

        // Check confirm password
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordEditText.setError(
                    "Confirm your password"
            );
            confirmPasswordEditText.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError(
                    "Passwords do not match"
            );
            confirmPasswordEditText.requestFocus();
            return;
        }

        // Disable button while creating account
        createAccountButton.setEnabled(false);

        // Create Firebase account
        firebaseAuth
                .createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        FirebaseUser user =
                                firebaseAuth.getCurrentUser();

                        if (user != null) {

                            // Send email verification
                            user.sendEmailVerification()
                                    .addOnCompleteListener(
                                            verificationTask -> {

                                                createAccountButton
                                                        .setEnabled(true);

                                                if (verificationTask
                                                        .isSuccessful()) {

                                                    Toast.makeText(
                                                            CreateAccountActivity.this,
                                                            "Account created. Please check your email and verify your account.",
                                                            Toast.LENGTH_LONG
                                                    ).show();

                                                } else {

                                                    Toast.makeText(
                                                            CreateAccountActivity.this,
                                                            "Account created, but verification email could not be sent.",
                                                            Toast.LENGTH_LONG
                                                    ).show();
                                                }
                                            }
                                    );

                        }

                    } else {

                        createAccountButton.setEnabled(true);

                        String errorMessage;

                        if (task.getException() != null) {
                            errorMessage =
                                    task.getException().getMessage();
                        } else {
                            errorMessage =
                                    "Account creation failed";
                        }

                        Toast.makeText(
                                CreateAccountActivity.this,
                                errorMessage,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }
}