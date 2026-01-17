package com.example.seniorcitizensupport.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.seniorcitizensupport.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    // UI Components
    private TextInputEditText editTextEmail;
    private TextInputEditText editTextPassword;
    private Button buttonLogin;
    private TextView textViewRegisterLink;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore fStore;

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // If user is already logged in, check their role first.
            checkUserRole(currentUser.getUid());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth & Firestore
        mAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

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
        Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show();

        // --- Firebase Sign-In ---
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success
                            Log.d(TAG, "signInWithEmail:success");
                            Toast.makeText(LoginActivity.this, "Login successful. Checking role...", Toast.LENGTH_SHORT).show();

                            // Check the role using the helper method
                            if (mAuth.getCurrentUser() != null) {
                                checkUserRole(mAuth.getCurrentUser().getUid());
                            }

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    /**
     * Helper method to fetch user role from Firestore and redirect
     */
    private void checkUserRole(String uid) {
        DocumentReference df = fStore.collection("users").document(uid);

        // Extract the data from the document
        df.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    // Try getting the role with both possible key names
                    String role = documentSnapshot.getString("role");
                    if (role == null) {
                        role = documentSnapshot.getString("userType");
                    }

                    Log.d(TAG, "Role found in DB: " + role); // Log for debugging

                    if (role != null) {
                        Intent intent;

                        // Check the specific string values
                        if (role.equalsIgnoreCase("Volunteer")) {
                            intent = new Intent(LoginActivity.this, VolunteerDashboardActivity.class);
                        } else if (role.equalsIgnoreCase("Admin")) {
                            intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                        } else if (role.equalsIgnoreCase("Family Member") || role.equalsIgnoreCase("Family")) {
                            intent = new Intent(LoginActivity.this, FamilyDashboardActivity.class);
                        } else {
                            // Default for "Senior Citizen" or unknown roles
                            intent = new Intent(LoginActivity.this, MainActivity.class);
                        }

                        // Perform the redirection
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        // Fallback if role field is totally missing
                        Toast.makeText(LoginActivity.this, "Role missing in database. Defaulting to Home.", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "User details not found in database.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
