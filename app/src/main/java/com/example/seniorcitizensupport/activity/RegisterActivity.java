package com.example.seniorcitizensupport.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.seniorcitizensupport.BaseActivity;
import com.example.seniorcitizensupport.Constants;
import com.example.seniorcitizensupport.R;
import com.example.seniorcitizensupport.model.User;
import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends BaseActivity {

    private static final String TAG = "RegisterActivity";

    // UI elements
    private EditText editTextFullName, editTextEmail, editTextPassword, editTextConfirmPassword;
    private Spinner spinnerUserType;
    private Button buttonRegister;
    private TextView textViewLogin;
    private ProgressBar progressBar; // Kept for layout compatibility, but BaseActivity dialog is better

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initializeViews();
        setupUserRoleSpinner();

        buttonRegister.setOnClickListener(v -> registerUser());
        textViewLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void initializeViews() {
        editTextFullName = findViewById(R.id.edit_text_full_name);
        editTextEmail = findViewById(R.id.edit_text_email);
        editTextPassword = findViewById(R.id.edit_text_password);
        editTextConfirmPassword = findViewById(R.id.edit_text_confirm_password);
        spinnerUserType = findViewById(R.id.spinner_user_type);
        buttonRegister = findViewById(R.id.button_register);
        textViewLogin = findViewById(R.id.text_view_login);
        progressBar = findViewById(R.id.progress_bar_register);
    }

    private void setupUserRoleSpinner() {
        String[] userRoles = new String[] {
                Constants.ROLE_SENIOR,
                Constants.ROLE_VOLUNTEER,
                Constants.ROLE_FAMILY,
                Constants.ROLE_ADMIN
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, userRoles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUserType.setAdapter(adapter);
    }

    private void registerUser() {
        String fullName = editTextFullName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(fullName)) {
            editTextFullName.setError("Full name is required.");
            editTextFullName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Email is required.");
            editTextEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            editTextPassword.setError("Password must be at least 6 characters.");
            editTextPassword.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)) {
            editTextConfirmPassword.setError("Passwords do not match.");
            editTextConfirmPassword.requestFocus();
            return;
        }

        String userType = spinnerUserType.getSelectedItem().toString();

        showProgressDialog("Registering...");

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        String userId = firebaseUser.getUid();

                        User user = new User();
                        user.setId(userId);
                        user.setName(fullName);
                        user.setEmail(email);
                        user.setRole(userType);

                        firestore.collection(Constants.KEY_COLLECTION_USERS)
                                .document(userId)
                                .set(user)
                                .addOnSuccessListener(aVoid -> {
                                    hideProgressDialog();
                                    showToast("Registration Successful!");

                                    Intent intent;
                                    if (userType.equals(Constants.ROLE_VOLUNTEER)) {
                                        intent = new Intent(RegisterActivity.this, VolunteerDashboardActivity.class);
                                    } else if (userType.equals(Constants.ROLE_ADMIN)) {
                                        intent = new Intent(RegisterActivity.this, AdminDashboardActivity.class);
                                    } else if (userType.equals(Constants.ROLE_FAMILY)) {
                                        intent = new Intent(RegisterActivity.this, FamilyDashboardActivity.class);
                                    } else {
                                        intent = new Intent(RegisterActivity.this, SeniorDashActivity.class);
                                    }

                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    hideProgressDialog();
                                    Log.e(TAG, "Error creating Firestore document", e);
                                    showToast("Error! Could not save user data: " + e.getMessage());
                                });
                    } else {
                        hideProgressDialog();
                        Log.e(TAG, "Firebase Auth registration failed", task.getException());
                        showToast("Registration Failed: "
                                + (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }
}
