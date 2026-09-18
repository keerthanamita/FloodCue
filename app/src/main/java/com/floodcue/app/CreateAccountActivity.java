package com.floodcue.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

public class CreateAccountActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;

    private Button createAccountButton;

    private TextInputLayout emailInputLayout;
    private TextInputLayout passwordInputLayout;
    private TextInputLayout confirmPasswordInputLayout;

    private FirebaseAuth firebaseAuth;

    private View rootView;
    private TextView backToLoginText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_create_account);

        // Root layout
        rootView = findViewById(R.id.createAccountRoot);

        // Input fields
        emailEditText = findViewById(R.id.editTextEmail);
        passwordEditText = findViewById(R.id.editTextPassword);
        confirmPasswordEditText =
                findViewById(R.id.editTextConfirmPassword);

        // Input layouts
        emailInputLayout =
                findViewById(R.id.emailInputLayout);

        passwordInputLayout =
                findViewById(R.id.passwordInputLayout);

        confirmPasswordInputLayout =
                findViewById(R.id.confirmPasswordInputLayout);

        // Create account button
        createAccountButton =
                findViewById(R.id.buttonCreateAccount);

        // Back to Login
        backToLoginText =
                findViewById(R.id.textBackToLogin);
        String loginLabel = "Already have an account? Login";

        SpannableString spannable =
                new SpannableString(loginLabel);

        int start = loginLabel.indexOf("Login");
        int end = start + "Login".length();

        spannable.setSpan(
                new ForegroundColorSpan(
                        getColor(R.color.button_primary)
                ),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        spannable.setSpan(
                new StyleSpan(Typeface.BOLD),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        backToLoginText.setText(spannable);
        // Firebase
        firebaseAuth = FirebaseAuth.getInstance();

        // Create account button
        createAccountButton.setOnClickListener(
                v -> createAccount()
        );

        // Back to Login
        backToLoginText.setOnClickListener(v -> {

            addClickEffect(backToLoginText);

            Intent intent = new Intent(
                    CreateAccountActivity.this,
                    LoginActivity.class
            );

            startActivity(intent);

            overridePendingTransition(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
            );

            finish();
        });

        // Clear errors when user focuses on the fields
        emailEditText.setOnFocusChangeListener(
                (v, hasFocus) -> {

                    if (hasFocus) {
                        emailInputLayout.setError(null);
                    }
                }
        );

        passwordEditText.setOnFocusChangeListener(
                (v, hasFocus) -> {

                    if (hasFocus) {
                        passwordInputLayout.setError(null);
                    }
                }
        );

        confirmPasswordEditText.setOnFocusChangeListener(
                (v, hasFocus) -> {

                    if (hasFocus) {
                        confirmPasswordInputLayout.setError(null);
                    }
                }
        );

        // Entrance animation
        animateEntrance();

        // Button press animation
        addPressAnimation(createAccountButton);
    }

    // =========================================================
    // CREATE ACCOUNT
    // =========================================================

    private void createAccount() {

        String email =
                emailEditText.getText()
                        .toString()
                        .trim();

        String password =
                passwordEditText.getText()
                        .toString();

        String confirmPassword =
                confirmPasswordEditText.getText()
                        .toString();

        // Clear old errors
        emailInputLayout.setError(null);
        passwordInputLayout.setError(null);
        confirmPasswordInputLayout.setError(null);

        // ---------------------------------
        // Email validation
        // ---------------------------------

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

        // ---------------------------------
        // Password validation
        // ---------------------------------

        if (TextUtils.isEmpty(password)) {

            showError(
                    passwordInputLayout,
                    passwordEditText,
                    "Enter a password"
            );

            return;
        }

        if (password.length() < 6) {

            showError(
                    passwordInputLayout,
                    passwordEditText,
                    "Password must contain at least 6 characters"
            );

            return;
        }

        if (password.trim().isEmpty()) {

            showError(
                    passwordInputLayout,
                    passwordEditText,
                    "Password cannot contain only spaces"
            );

            return;
        }

        // ---------------------------------
        // Confirm password
        // ---------------------------------

        if (TextUtils.isEmpty(confirmPassword)) {

            showError(
                    confirmPasswordInputLayout,
                    confirmPasswordEditText,
                    "Confirm your password"
            );

            return;
        }

        if (!password.equals(confirmPassword)) {

            showError(
                    confirmPasswordInputLayout,
                    confirmPasswordEditText,
                    "Passwords do not match"
            );

            return;
        }

        // ---------------------------------
        // Create Firebase account
        // ---------------------------------

        setLoading(true);

        firebaseAuth
                .createUserWithEmailAndPassword(
                        email,
                        password
                )
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        FirebaseUser user =
                                firebaseAuth.getCurrentUser();

                        if (user != null) {

                            sendVerificationEmail(user);

                        } else {

                            setLoading(false);

                            showMessage(
                                    "Account creation failed. Please try again."
                            );
                        }

                    } else {

                        setLoading(false);

                        showFirebaseError(
                                task.getException()
                        );
                    }
                });
    }

    // =========================================================
    // SEND VERIFICATION EMAIL
    // =========================================================

    private void sendVerificationEmail(FirebaseUser user) {

        user.sendEmailVerification()
                .addOnCompleteListener(
                        verificationTask -> {

                            setLoading(false);

                            if (verificationTask.isSuccessful()) {

                                showMessage(
                                        "✓ Account created. Verification email sent."
                                );

                            } else {

                                showMessage(
                                        "Account created, but the verification email could not be sent."
                                );
                            }
                        }
                );
    }

    // =========================================================
    // EMAIL VALIDATION
    // =========================================================

    private boolean isValidEmail(String email) {

        // Maximum practical email length
        if (email.length() > 254) {
            return false;
        }

        // Spaces are not allowed
        if (email.contains(" ")) {
            return false;
        }

        // Standard Android email pattern
        if (!Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()) {

            return false;
        }

        // Avoid consecutive dots
        if (email.contains("..")) {
            return false;
        }

        // Avoid dot at beginning/end
        if (email.startsWith(".")
                || email.endsWith(".")) {

            return false;
        }

        // Check local part
        int atIndex = email.indexOf("@");

        if (atIndex <= 0) {
            return false;
        }

        String localPart =
                email.substring(0, atIndex);

        if (localPart.startsWith(".")
                || localPart.endsWith(".")) {

            return false;
        }

        return true;
    }

    // =========================================================
    // SHOW FIELD ERROR
    // =========================================================

    private void showError(
            TextInputLayout inputLayout,
            EditText editText,
            String message
    ) {

        inputLayout.setError(message);
        editText.requestFocus();
    }

    // =========================================================
    // FIREBASE ERROR HANDLING
    // =========================================================

    private void showFirebaseError(Exception exception) {

        String message =
                "Account creation failed. Please try again.";

        if (exception != null
                && exception.getMessage() != null) {

            String firebaseMessage =
                    exception.getMessage().toLowerCase();

            if (firebaseMessage.contains("already in use")
                    || firebaseMessage.contains(
                    "email-already-in-use")) {

                message =
                        "An account already exists with this email.";

            } else if (firebaseMessage.contains("weak-password")
                    || firebaseMessage.contains(
                    "password is too weak")) {

                message =
                        "Password is too weak. Please choose a stronger password.";

            } else if (firebaseMessage.contains(
                    "too-many-requests")) {

                message =
                        "Too many attempts. Please try again later.";

            } else if (firebaseMessage.contains("network")) {

                message =
                        "Network error. Please check your internet connection.";

            } else if (firebaseMessage.contains("invalid-email")) {

                message =
                        "Please enter a valid email address.";

            } else if (firebaseMessage.contains(
                    "operation-not-allowed")) {

                message =
                        "Email/password account creation is currently unavailable.";
            }
        }

        showMessage(message);
    }

    // =========================================================
    // LOADING STATE
    // =========================================================

    private void setLoading(boolean loading) {

        createAccountButton.setEnabled(!loading);

        if (loading) {

            createAccountButton.setText(
                    "Creating account..."
            );

            createAccountButton.setAlpha(0.7f);

        } else {

            createAccountButton.setText(
                    "Create account"
            );

            createAccountButton.setAlpha(1.0f);
        }
    }

    // =========================================================
    // SUCCESS / ERROR MESSAGE
    // =========================================================

    private void showMessage(String message) {

        if (rootView != null) {

            Snackbar.make(
                    rootView,
                    message,
                    Snackbar.LENGTH_LONG
            ).show();

        } else {

            Toast.makeText(
                    this,
                    message,
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // =========================================================
    // ENTRANCE ANIMATION
    // =========================================================

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

    // =========================================================
    // SUBTLE LINK CLICK EFFECT
    // =========================================================

    private void addClickEffect(View view) {

        if (view == null) {
            return;
        }

        view.animate()
                .alpha(0.90f)
                .setDuration(70)
                .withEndAction(() ->
                        view.animate()
                                .alpha(1f)
                                .setDuration(100)
                                .start()
                )
                .start();
    }

    // =========================================================
    // BUTTON PRESS ANIMATION
    // =========================================================

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

                            v.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(80)
                                    .start();

                            v.performClick();

                            break;

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

    // =========================================================
    // CLEANUP
    // =========================================================

    @Override
    protected void onDestroy() {

        if (createAccountButton != null) {
            createAccountButton.setOnTouchListener(null);
        }

        super.onDestroy();
    }
}