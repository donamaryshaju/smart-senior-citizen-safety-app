package com.example.seniorcitizensupport.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.seniorcitizensupport.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class FamilyDashboardActivity extends AppCompatActivity {

    private TextView welcomeText;
    private Button btnLogout, btnCheckStatus;
    private FirebaseAuth mAuth;
    private FirebaseFirestore fStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family_dashboard);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

        // Initialize Views
        welcomeText = findViewById(R.id.text_family_welcome);
        btnLogout = findViewById(R.id.button_logout_family);
        btnCheckStatus = findViewById(R.id.button_check_senior_status);

        // Fetch user name
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            fetchFamilyName(user.getUid());
        }

        // Logout Listener
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                Toast.makeText(FamilyDashboardActivity.this, "Logged out successfully.", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(FamilyDashboardActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });

        // Check Status Listener (Placeholder for future logic)
        btnCheckStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(FamilyDashboardActivity.this, "Checking connected senior status...", Toast.LENGTH_SHORT).show();
                // TODO: Add logic to view health status of connected senior citizen
            }
        });
    }

    private void fetchFamilyName(String uid) {
        DocumentReference docRef = fStore.collection("users").document(uid);
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String name = documentSnapshot.getString("fullName");
                if (name != null) {
                    welcomeText.setText("Welcome, " + name + "!");
                }
            }
        });
    }
}
