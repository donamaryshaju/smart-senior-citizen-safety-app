package com.example.seniorcitizensupport.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.example.seniorcitizensupport.BaseActivity;
import com.example.seniorcitizensupport.Constants;
import com.example.seniorcitizensupport.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.firestore.DocumentSnapshot;

public class LoginActivity extends BaseActivity {

    private static final String TAG = "LoginActivity";
    private static final String PREFS_NAME = "LoginPrefs";
    private static final String PREF_EMAIL = "email";
    private static final String PREF_PASSWORD = "password";
    private static final String PREF_REMEMBER = "remember";

    // UI Components
    private TextInputEditText editTextEmail;
    private TextInputEditText editTextPassword;
    private Button buttonLogin;
    private TextView textViewRegisterLink;
    private CheckBox checkboxRememberMe;
    private TextView textViewForgotPassword;
    private Button buttonEmergency;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // DEBUG: Catch crashes and show toast
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            e.printStackTrace();
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> Toast
                    .makeText(getApplicationContext(), "LOGIN CRASH: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // FIX: Load UI first to avoid blank screen

        // Check if user is already signed in (Firebase persistence)
        if (auth.getCurrentUser() != null) {
            showProgressDialog("Resuming session..."); // Show feedback
            checkUserRole(auth.getCurrentUser().getUid());
            return;
        }

        try {
            sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

            // Initialize UI components
            initializeViews();

            // Check Remember Me
            checkRememberMe();

            // Set Listeners
            setupListeners();
        } catch (Exception e) {
            Toast.makeText(this, "Login Init Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void initializeViews() {
        editTextEmail = findViewById(R.id.edit_text_email_login);
        editTextPassword = findViewById(R.id.edit_text_password_login);
        buttonLogin = findViewById(R.id.button_login);
        textViewRegisterLink = findViewById(R.id.text_register);
        checkboxRememberMe = findViewById(R.id.checkbox_remember_me);
        textViewForgotPassword = findViewById(R.id.text_forgot_password);
        buttonEmergency = findViewById(R.id.button_emergency);
    }

    private void checkRememberMe() {
        boolean remember = sharedPreferences.getBoolean(PREF_REMEMBER, false);
        if (remember) {
            String email = sharedPreferences.getString(PREF_EMAIL, "");
            String password = sharedPreferences.getString(PREF_PASSWORD, "");
            editTextEmail.setText(email);
            editTextPassword.setText(password);
            checkboxRememberMe.setChecked(true);
        }
    }

    private void setupListeners() {
        // Login Button
        buttonLogin.setOnClickListener(v -> loginUser());

        // Register Link
        textViewRegisterLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegistrationSelectionActivity.class);
            startActivity(intent);
        });

        // Forgot Password
        textViewForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());

        // Emergency Access
        buttonEmergency.setOnClickListener(v -> {
            // Navigate to MainActivity as Guest
            Intent intent = new Intent(LoginActivity.this, SeniorDashActivity.class);
            intent.putExtra("GUEST_MODE", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loginUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // --- Input Validation ---
        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Email is required.");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Password is required.");
            return;
        }

        // Show a loading message
        showProgressDialog("Logging in...");

        // --- Firebase Sign-In ---
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success
                            Log.d(TAG, "signInWithEmail:success");

                            // Handle Remember Me
                            handleRememberMe(email, password);

                            // Check the role using the helper method
                            if (auth.getCurrentUser() != null) {
                                checkUserRole(auth.getCurrentUser().getUid());
                            } else {
                                hideProgressDialog();
                            }

                        } else {
                            // If sign in fails, display a message to the user.
                            hideProgressDialog();
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            showToast("Authentication failed: "
                                    + (task.getException() != null ? task.getException().getMessage()
                                            : "Unknown error"));
                        }
                    }
                });
    }

    private void handleRememberMe(String email, String password) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (checkboxRememberMe.isChecked()) {
            editor.putBoolean(PREF_REMEMBER, true);
            editor.putString(PREF_EMAIL, email);
            editor.putString(PREF_PASSWORD, password); // Note: Storing password in plain text is not best practice but
                                                       // requested
        } else {
            editor.clear();
        }
        editor.apply();
    }

    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password");
        builder.setMessage("Enter your email address to receive a reset link.");

        final EditText input = new EditText(this);
        input.setHint("Email Address");
        input.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        builder.setView(input);

        builder.setPositiveButton("Send", (dialog, which) -> {
            String email = input.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                showToast("Please enter your email.");
            } else {
                sendPasswordResetEmail(email);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void sendPasswordResetEmail(String email) {
        showProgressDialog("Sending reset email...");
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    hideProgressDialog();
                    if (task.isSuccessful()) {
                        showToast("Reset link sent to your email.");
                    } else {
                        showToast("Failed to send reset email: " +
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }

    /**
     * Helper method to fetch user role from Firestore and redirect
     */
    private void checkUserRole(String uid) {
        firestore.collection(Constants.KEY_COLLECTION_USERS).document(uid)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        hideProgressDialog();
                        if (documentSnapshot.exists()) {
                            String role = documentSnapshot.getString(Constants.KEY_ROLE);
                            if (role == null) {
                                role = documentSnapshot.getString("userType"); // Fallback
                            }

                            Log.d(TAG, "Role found in DB: " + role);

                            if (role != null) {
                                Intent intent = null;

                                if (role.equalsIgnoreCase(Constants.ROLE_VOLUNTEER)) {
                                    intent = new Intent(LoginActivity.this, VolunteerDashboardActivity.class);
                                } else if (role.equalsIgnoreCase(Constants.ROLE_ADMIN)) {
                                    intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                                } else if (role.equalsIgnoreCase(Constants.ROLE_FAMILY)
                                        || role.equalsIgnoreCase("Family")) {
                                    intent = new Intent(LoginActivity.this, FamilyDashboardActivity.class);
                                } else {
                                    // Default for "Senior Citizen"
                                    intent = new Intent(LoginActivity.this, SeniorDashActivity.class);
                                }

                                if (intent != null) {
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                }
                            } else {
                                showToast("Role missing in database. Defaulting to Home.");
                                startActivity(new Intent(LoginActivity.this, SeniorDashActivity.class));
                                finish();
                            }
                        } else {
                            showToast("User details not found in database.");
                            auth.signOut();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    showToast("Failed to fetch user data: " + e.getMessage());
                });
    }
}
