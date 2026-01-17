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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.seniorcitizensupport.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    // UI elements
    private EditText editTextFullName, editTextEmail, editTextPassword, editTextConfirmPassword;
    private Spinner spinnerUserType;
    private Button buttonRegister;
    private TextView textViewLogin;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore mStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initializeViews();

        mAuth = FirebaseAuth.getInstance();
        mStore = FirebaseFirestore.getInstance();

        // This method now sets up the spinner without using arrays.xml
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

    /**
     * Sets up the Spinner by creating the list of roles directly in the Java code,
     * removing the need for an arrays.xml file.
     */
    private void setupUserRoleSpinner() {
        // Create the list of roles directly here
        String[] userRoles = new String[]{"Senior Citizen", "Volunteer", "Family Member", "Admin"};

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, userRoles);

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        spinnerUserType.setAdapter(adapter);
    }

    private void registerUser() {
        String fullName = editTextFullName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        // --- Input Validation ---
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

        progressBar.setVisibility(View.VISIBLE);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        String userId = firebaseUser.getUid();

                        DocumentReference docRef = mStore.collection("users").document(userId);
                        Map<String, Object> user = new HashMap<>();
                        user.put("fullName", fullName);
                        user.put("email", email);
                        user.put("userType", userType);

                        docRef.set(user).addOnSuccessListener(aVoid -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(RegisterActivity.this, "Registration Successful!", Toast.LENGTH_SHORT).show();

                            Intent intent;
                            if (userType.equals("Volunteer")) {
                                intent = new Intent(RegisterActivity.this, VolunteerDashboardActivity.class);
                            } else if (userType.equals("Admin")) {
                                intent = new Intent(RegisterActivity.this, AdminDashboardActivity.class);
                            } else if (userType.equals("Family Member")) {
                                intent = new Intent(RegisterActivity.this, FamilyDashboardActivity.class);
                            } else {
                                // Default to Senior Citizen / Main Activity
                                intent = new Intent(RegisterActivity.this, MainActivity.class);
                            }

                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        }).addOnFailureListener(e -> {
                            progressBar.setVisibility(View.GONE);
                            Log.e(TAG, "Error creating Firestore document", e);
                            Toast.makeText(RegisterActivity.this, "Error! Could not save user data.", Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "Firebase Auth registration failed", task.getException());
                        Toast.makeText(RegisterActivity.this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
