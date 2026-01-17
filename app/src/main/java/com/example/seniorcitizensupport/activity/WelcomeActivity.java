package com.example.seniorcitizensupport.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.seniorcitizensupport.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class WelcomeActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore fStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        // ✅ If already logged in → redirect by role
        if (currentUser != null) {
            redirectUserByRole(currentUser.getUid());
            return;
        }

        // Show welcome screen
        setContentView(R.layout.activity_welcome);

        Button loginButton = findViewById(R.id.button_login);
        Button registerButton = findViewById(R.id.button_register);
        Button guestButton = findViewById(R.id.button_continue_guest);

        loginButton.setOnClickListener(v ->
                startActivity(new Intent(WelcomeActivity.this, LoginActivity.class)));

        registerButton.setOnClickListener(v ->
                startActivity(new Intent(WelcomeActivity.this, RegisterActivity.class)));

        guestButton.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    // ---------------- ROLE REDIRECTION ----------------
    private void redirectUserByRole(String uid) {

        fStore.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {

                    String role = documentSnapshot.getString("role");

                    if (role == null) {
                        role = documentSnapshot.getString("userType");
                    }

                    Intent intent;

                    if ("Volunteer".equalsIgnoreCase(role)) {
                        intent = new Intent(this, VolunteerDashboardActivity.class);
                    } else if ("Admin".equalsIgnoreCase(role)) {
                        intent = new Intent(this, AdminDashboardActivity.class);
                    } else if ("Family".equalsIgnoreCase(role) ||
                            "Family Member".equalsIgnoreCase(role)) {
                        intent = new Intent(this, FamilyDashboardActivity.class);
                    } else {
                        intent = new Intent(this, com.example.seniorcitizensupport.activity.MainActivity.class);
                    }

                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Unable to load profile. Opening home.", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, com.example.seniorcitizensupport.activity.MainActivity.class));
                    finish();
                });
    }
}
