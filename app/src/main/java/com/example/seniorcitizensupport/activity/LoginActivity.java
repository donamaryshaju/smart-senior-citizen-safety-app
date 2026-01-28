package com.example.seniorcitizensupport.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

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

    // UI Components
    private TextInputEditText editTextEmail;
    private TextInputEditText editTextPassword;
    private Button buttonLogin;
    private TextView textViewRegisterLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user is already signed in
        if (auth.getCurrentUser() != null) {
            checkUserRole(auth.getCurrentUser().getUid());
            return; // Exit onCreate to prevent UI flicker
        }

        setContentView(R.layout.activity_login);

        // Initialize UI components
        initializeViews();

        // Set listener for the Login button
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        // Set listener for the "Register" link
        textViewRegisterLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open the RegisterActivity
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    private void initializeViews() {
        editTextEmail = findViewById(R.id.edit_text_email_login);
        editTextPassword = findViewById(R.id.edit_text_password_login);
        buttonLogin = findViewById(R.id.button_login);
        textViewRegisterLink = findViewById(R.id.text_view_register_link);
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
                            // Try getting the role with both possible key names (handling legacy data if
                            // any)
                            String role = documentSnapshot.getString(Constants.KEY_ROLE);
                            if (role == null) {
                                role = documentSnapshot.getString("userType"); // Fallback for old data
                            }

                            Log.d(TAG, "Role found in DB: " + role);

                            if (role != null) {
                                Intent intent;

                                if (role.equalsIgnoreCase(Constants.ROLE_VOLUNTEER)) {
                                    intent = new Intent(LoginActivity.this, VolunteerDashboardActivity.class);
                                } else if (role.equalsIgnoreCase(Constants.ROLE_ADMIN)) {
                                    intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                                } else if (role.equalsIgnoreCase(Constants.ROLE_FAMILY)
                                        || role.equalsIgnoreCase("Family")) {
                                    intent = new Intent(LoginActivity.this, FamilyDashboardActivity.class);
                                } else {
                                    // Default for "Senior Citizen"
                                    intent = new Intent(LoginActivity.this, MainActivity.class);
                                }

                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            } else {
                                showToast("Role missing in database. Defaulting to Home.");
                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                finish();
                            }
                        } else {
                            showToast("User details not found in database.");
                            // Optional: Logout if user data is missing
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
