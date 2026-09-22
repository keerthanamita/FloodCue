package com.floodcue.app;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
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

import android.widget.LinearLayout;

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

    /*
     * IMPORTANT:
     * Keep this value synchronized with the minimum password
     * length configured in Firebase Authentication.
     *
     * Firebase default minimum = 6 characters.
     */
    private static final int MIN_PASSWORD_LENGTH = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_create_account);

        // =========================================================
        // ROOT LAYOUT
        // =========================================================

        rootView = findViewById(R.id.createAccountRoot);

        // =========================================================
        // INPUT FIELDS
        // =========================================================

        emailEditText = findViewById(R.id.editTextEmail);

        passwordEditText = findViewById(R.id.editTextPassword);

        confirmPasswordEditText =
                findViewById(R.id.editTextConfirmPassword);

        // =========================================================
        // INPUT LAYOUTS
        // =========================================================

        emailInputLayout =
                findViewById(R.id.emailInputLayout);

        passwordInputLayout =
                findViewById(R.id.passwordInputLayout);

        confirmPasswordInputLayout =
                findViewById(R.id.confirmPasswordInputLayout);

        // =========================================================
        // CREATE ACCOUNT BUTTON
        // =========================================================

        createAccountButton =
                findViewById(R.id.buttonCreateAccount);

        // =========================================================
        // BACK TO LOGIN
        // =========================================================

        backToLoginText =
                findViewById(R.id.textBackToLogin);

        String loginLabel =
                "Already have an account? Login";

        SpannableStringBuilder spannable =
                new SpannableStringBuilder(loginLabel);

        int start = loginLabel.indexOf("Login");
        int end = start + "Login".length();

        if (start >= 0) {

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
        }

        backToLoginText.setText(spannable);

        // =========================================================
        // FIREBASE
        // =========================================================

        firebaseAuth = FirebaseAuth.getInstance();

        // =========================================================
        // PASSWORD POLICY UI
        // =========================================================

        setupPasswordPolicyFeedback();

        // =========================================================
        // CREATE ACCOUNT BUTTON
        // =========================================================

        createAccountButton.setOnClickListener(
                v -> createAccount()
        );

        // =========================================================
        // BACK TO LOGIN
        // =========================================================

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

        // =========================================================
        // CLEAR EMAIL ERROR WHEN USER RETURNS TO FIELD
        // =========================================================

        emailEditText.setOnFocusChangeListener(
                (v, hasFocus) -> {

                    if (hasFocus) {
                        emailInputLayout.setError(null);
                    }
                }
        );

        // =========================================================
        // PASSWORD FIELD FOCUS
        // =========================================================

        passwordEditText.setOnFocusChangeListener(
                (v, hasFocus) -> {

                    if (hasFocus) {
                        passwordInputLayout.setError(null);
                    }
                }
        );

        // =========================================================
        // CONFIRM PASSWORD FIELD FOCUS
        // =========================================================

        confirmPasswordEditText.setOnFocusChangeListener(
                (v, hasFocus) -> {

                    if (hasFocus) {
                        confirmPasswordInputLayout.setError(null);
                    }
                }
        );

        // =========================================================
        // ENTRANCE ANIMATION
        // =========================================================

        animateEntrance();

        // =========================================================
        // BUTTON PRESS ANIMATION
        // =========================================================

        addPressAnimation(createAccountButton);
    }

    // =========================================================
    // PASSWORD POLICY FEEDBACK
    // =========================================================

    private void setupPasswordPolicyFeedback() {

        passwordEditText.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count) {

                        updatePasswordRequirements(
                                s.toString()
                        );

                        // Clear Firebase password error
                        // once the user starts correcting it.
                        if (passwordInputLayout.getError() != null) {
                            passwordInputLayout.setError(null);
                        }
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s) {
                    }
                }
        );
    }

    // =========================================================
    // UPDATE PASSWORD REQUIREMENT UI
    // =========================================================

    private void updatePasswordRequirements(
            String password) {

        boolean hasUppercase =
                password.matches(".*[A-Z].*");

        boolean hasLowercase =
                password.matches(".*[a-z].*");

        boolean hasNumber =
                password.matches(".*[0-9].*");

        boolean hasSpecial =
                password.matches(
                        ".*[^a-zA-Z0-9].*"
                );

        boolean hasMinimumLength =
                password.length() >= MIN_PASSWORD_LENGTH;

        SpannableStringBuilder builder =
                new SpannableStringBuilder();

        appendRequirement(
                builder,
                hasMinimumLength,
                "Minimum " + MIN_PASSWORD_LENGTH + " characters"
        );

        appendRequirement(
                builder,
                hasUppercase,
                "Uppercase letter"
        );

        appendRequirement(
                builder,
                hasLowercase,
                "Lowercase letter"
        );

        appendRequirement(
                builder,
                hasNumber,
                "Number"
        );

        appendRequirement(
                builder,
                hasSpecial,
                "Special character"
        );

        passwordInputLayout.setHelperText(builder);

        /*
         * When the password field is empty, don't visually
         * mark everything as an error. Just show the guidance.
         */
    }

    // =========================================================
    // ADD ONE PASSWORD REQUIREMENT
    // =========================================================

    private void appendRequirement(
            SpannableStringBuilder builder,
            boolean satisfied,
            String text) {

        int start = builder.length();

        String symbol =
                satisfied ? "✓ " : "○ ";

        builder.append(symbol);
        builder.append(text);

        builder.append("\n");

        int end = builder.length();

        int color;

        if (satisfied) {
            color = getColor(
                    R.color.button_primary
            );
        } else {
            color = getColor(
                    android.R.color.darker_gray
            );
        }

        builder.setSpan(
                new ForegroundColorSpan(color),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        if (satisfied) {

            builder.setSpan(
                    new StyleSpan(Typeface.BOLD),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
    }

    // =========================================================
    // CHECK PASSWORD REQUIREMENTS
    // =========================================================

    private boolean isPasswordPolicySatisfied(
            String password) {

        if (password.length() < MIN_PASSWORD_LENGTH) {
            return false;
        }

        boolean hasUppercase =
                password.matches(".*[A-Z].*");

        boolean hasLowercase =
                password.matches(".*[a-z].*");

        boolean hasNumber =
                password.matches(".*[0-9].*");

        boolean hasSpecial =
                password.matches(
                        ".*[^a-zA-Z0-9].*"
                );

        return hasUppercase
                && hasLowercase
                && hasNumber
                && hasSpecial;
    }

    // =========================================================
    // GET PASSWORD REQUIREMENT ERROR
    // =========================================================

    private String getPasswordPolicyError(
            String password) {

        if (password.length() < MIN_PASSWORD_LENGTH) {

            return "Password needs at least "
                    + MIN_PASSWORD_LENGTH
                    + " characters.";
        }

        if (!password.matches(".*[A-Z].*")) {

            return "Add at least one uppercase letter.";
        }

        if (!password.matches(".*[a-z].*")) {

            return "Add at least one lowercase letter.";
        }

        if (!password.matches(".*[0-9].*")) {

            return "Add at least one number.";
        }

        if (!password.matches(
                ".*[^a-zA-Z0-9].*")) {

            return "Add at least one special character.";
        }

        return null;
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

        // =====================================================
        // CLEAR OLD ERRORS
        // =====================================================

        emailInputLayout.setError(null);
        passwordInputLayout.setError(null);
        confirmPasswordInputLayout.setError(null);

        // =====================================================
        // EMAIL VALIDATION
        // =====================================================

        if (TextUtils.isEmpty(email)) {

            showError(
                    emailInputLayout,
                    emailEditText,
                    "Enter your email."
            );

            return;
        }

        if (!isValidEmail(email)) {

            showError(
                    emailInputLayout,
                    emailEditText,
                    "Enter a valid email address."
            );

            return;
        }

        // =====================================================
        // PASSWORD EMPTY CHECK
        // =====================================================

        if (TextUtils.isEmpty(password)) {

            showError(
                    passwordInputLayout,
                    passwordEditText,
                    "Enter a password."
            );

            return;
        }

        // =====================================================
        // PASSWORD WHITESPACE CHECK
        // =====================================================

        if (password.trim().isEmpty()) {

            showError(
                    passwordInputLayout,
                    passwordEditText,
                    "Password cannot contain only spaces."
            );

            return;
        }

        // =====================================================
        // PASSWORD MAXIMUM LENGTH
        // Firebase supports up to 4096 characters.
        // =====================================================

        if (password.length() > 4096) {

            showError(
                    passwordInputLayout,
                    passwordEditText,
                    "Password is too long."
            );

            return;
        }

        // =====================================================
        // PASSWORD POLICY UI VALIDATION
        //
        // This is only for immediate user feedback.
        // Firebase remains the final authority.
        // =====================================================

        if (!isPasswordPolicySatisfied(password)) {

            String policyError =
                    getPasswordPolicyError(password);

            showError(
                    passwordInputLayout,
                    passwordEditText,
                    policyError
            );

            return;
        }

        // =====================================================
        // CONFIRM PASSWORD EMPTY CHECK
        // =====================================================

        if (TextUtils.isEmpty(confirmPassword)) {

            showError(
                    confirmPasswordInputLayout,
                    confirmPasswordEditText,
                    "Confirm your password."
            );

            return;
        }

        // =====================================================
        // CONFIRM PASSWORD MATCH
        // =====================================================

        if (!password.equals(confirmPassword)) {

            showError(
                    confirmPasswordInputLayout,
                    confirmPasswordEditText,
                    "Passwords do not match."
            );

            return;
        }

        // =====================================================
        // CREATE FIREBASE ACCOUNT
        // =====================================================

        setLoading(true);

        firebaseAuth
                .createUserWithEmailAndPassword(
                        email,
                        password
                )
                .addOnCompleteListener(
                        this,
                        task -> {

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
                        }
                );
    }

    // =========================================================
    // SEND VERIFICATION EMAIL
    // =========================================================

    private void sendVerificationEmail(
            FirebaseUser user) {

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

    private boolean isValidEmail(
            String email) {

        // Maximum practical email length.
        if (email.length() > 254) {
            return false;
        }

        // Spaces are not allowed.
        if (email.contains(" ")) {
            return false;
        }

        // Standard Android email pattern.
        if (!Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()) {

            return false;
        }

        // Avoid consecutive dots.
        if (email.contains("..")) {
            return false;
        }

        // Avoid dot at beginning/end.
        if (email.startsWith(".")
                || email.endsWith(".")) {

            return false;
        }

        // Check local part.
        int atIndex =
                email.indexOf("@");

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

    // =========================================================
    // SHOW FIELD ERROR
    // =========================================================

    private void showError(
            TextInputLayout inputLayout,
            EditText editText,
            String message) {

        if (inputLayout == passwordInputLayout) {
            inputLayout.setHelperText(null);
        }

        inputLayout.setError(message);

        editText.requestFocus();

        if (inputLayout == passwordInputLayout) {
            LinearLayout.LayoutParams params =
                    (LinearLayout.LayoutParams)
                            confirmPasswordInputLayout.getLayoutParams();

            params.topMargin = 0;

            confirmPasswordInputLayout.setLayoutParams(params);
        }
    }

    // =========================================================
    // FIREBASE ERROR HANDLING
    // =========================================================

    private void showFirebaseError(
            Exception exception) {

        String message =
                "Account creation failed. Please try again.";

        if (exception != null
                && exception.getMessage() != null) {

            String firebaseMessage =
                    exception.getMessage()
                            .toLowerCase();

            // =================================================
            // EMAIL ALREADY EXISTS
            // =================================================

            if (firebaseMessage.contains(
                    "already in use")
                    || firebaseMessage.contains(
                    "email-already-in-use")) {

                message =
                        "An account already exists with this email.";

            }

            // =================================================
            // PASSWORD POLICY / WEAK PASSWORD
            // =================================================

            else if (
                    firebaseMessage.contains(
                            "weak-password")
                            || firebaseMessage.contains(
                            "password is too weak")
                            || firebaseMessage.contains(
                            "password policy")
                            || firebaseMessage.contains(
                            "password does not meet")
                            || firebaseMessage.contains(
                            "password requirements")
            ) {

                String policyError =
                        getPasswordPolicyError(
                                passwordEditText
                                        .getText()
                                        .toString()
                        );

                if (policyError != null) {

                    showError(
                            passwordInputLayout,
                            passwordEditText,
                            policyError
                    );

                    return;

                } else {

                    message =
                            "Password does not meet the required Firebase password policy.";
                }
            }

            // =================================================
            // TOO MANY REQUESTS
            // =================================================

            else if (firebaseMessage.contains(
                    "too-many-requests")) {

                message =
                        "Too many attempts. Please try again later.";

            }

            // =================================================
            // NETWORK ERROR
            // =================================================

            else if (firebaseMessage.contains(
                    "network")) {

                message =
                        "Network error. Please check your internet connection.";

            }

            // =================================================
            // INVALID EMAIL
            // =================================================

            else if (firebaseMessage.contains(
                    "invalid-email")) {

                message =
                        "Please enter a valid email address.";

            }

            // =================================================
            // OPERATION NOT ALLOWED
            // =================================================

            else if (firebaseMessage.contains(
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

    private void setLoading(
            boolean loading) {

        createAccountButton.setEnabled(
                !loading
        );

        if (loading) {

            createAccountButton.setAlpha(
                    0.7f
            );

        } else {

            createAccountButton.setAlpha(
                    1.0f
            );
        }
    }

    // =========================================================
    // SUCCESS / ERROR MESSAGE
    // =========================================================

    private void showMessage(
            String message) {

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

    private void addClickEffect(
            View view) {

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

    private void addPressAnimation(
            View view) {

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

            createAccountButton.setOnTouchListener(
                    null
            );
        }

        super.onDestroy();
    }
}