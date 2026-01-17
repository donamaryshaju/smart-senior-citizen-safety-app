package com.example.seniorcitizensupport.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.seniorcitizensupport.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private EditText mFullName, mEmail, mPhone, mAddress;
    private Button mSaveBtn, mLogoutBtn;

    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Views
        mFullName = findViewById(R.id.profile_fullname);
        mEmail = findViewById(R.id.profile_email);
        mPhone = findViewById(R.id.profile_phone);
        mAddress = findViewById(R.id.profile_address);
        mSaveBtn = findViewById(R.id.profile_save_btn);
        mLogoutBtn = findViewById(R.id.profile_logout_btn);

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

        if (fAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userId = fAuth.getCurrentUser().getUid();

        // 1. Load existing data
        loadUserProfile();

        // 2. Save changes
        mSaveBtn.setOnClickListener(v -> saveProfileChanges());

        // 3. Logout logic
        mLogoutBtn.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadUserProfile() {
        DocumentReference docRef = fStore.collection("users").document(userId);
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {

                // --- ROBUST NAME LOADING ---
                String loadedName = documentSnapshot.getString("fName");
                if (loadedName == null) loadedName = documentSnapshot.getString("fullName");
                if (loadedName == null) loadedName = documentSnapshot.getString("Name");

                if (loadedName != null) {
                    mFullName.setText(loadedName);
                }

                // Set Email
                if (documentSnapshot.contains("email")) {
                    mEmail.setText(documentSnapshot.getString("email"));
                }

                // Set Phone
                if (documentSnapshot.contains("phone")) {
                    mPhone.setText(documentSnapshot.getString("phone"));
                }

                // Set Address
                if (documentSnapshot.contains("address")) {
                    mAddress.setText(documentSnapshot.getString("address"));
                } else if (documentSnapshot.contains("location")) {
                    mAddress.setText(documentSnapshot.getString("location"));
                }
            } else {
                Toast.makeText(ProfileActivity.this, "Profile not found", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Error fetching data", Toast.LENGTH_SHORT).show());
    }

    private void saveProfileChanges() {
        // Use .trim() to remove extra spaces
        String name = mFullName.getText().toString().trim();
        String phone = mPhone.getText().toString().trim();
        String address = mAddress.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Name, Phone and Address cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        mSaveBtn.setEnabled(false);
        mSaveBtn.setText("Saving...");

        DocumentReference docRef = fStore.collection("users").document(userId);

        Map<String, Object> userMap = new HashMap<>();

        // --- CRITICAL FIX: SAVE BOTH KEYS ---
        // This ensures compatibility with whatever key VolunteerDashboard is looking for
        userMap.put("fName", name);
        userMap.put("fullName", name);
        // ------------------------------------

        userMap.put("phone", phone);
        userMap.put("address", address);

        // Merge options ensures we don't delete email or other fields
        docRef.set(userMap, SetOptions.merge()).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(ProfileActivity.this, "Profile Saved Successfully", Toast.LENGTH_SHORT).show();
                mSaveBtn.setEnabled(true);
                mSaveBtn.setText("SAVE CHANGES");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(ProfileActivity.this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                mSaveBtn.setEnabled(true);
                mSaveBtn.setText("SAVE CHANGES");
            }
        });
    }
}
