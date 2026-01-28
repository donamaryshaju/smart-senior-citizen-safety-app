package com.example.seniorcitizensupport.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.example.seniorcitizensupport.BaseActivity;
import com.example.seniorcitizensupport.Constants;
import com.example.seniorcitizensupport.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends BaseActivity {

    private EditText mFullName, mEmail, mPhone, mAddress;
    private Button mSaveBtn, mLogoutBtn;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        if (auth.getCurrentUser() == null) {
            showToast("User not logged in");
            finish();
            return;
        }

        userId = auth.getCurrentUser().getUid();

        // Initialize Views
        mFullName = findViewById(R.id.profile_fullname);
        mEmail = findViewById(R.id.profile_email);
        mPhone = findViewById(R.id.profile_phone);
        mAddress = findViewById(R.id.profile_address);
        mSaveBtn = findViewById(R.id.profile_save_btn);
        mLogoutBtn = findViewById(R.id.profile_logout_btn);

        // 1. Load existing data
        loadUserProfile();

        // 2. Save changes
        mSaveBtn.setOnClickListener(v -> saveProfileChanges());

        // 3. Logout logic
        mLogoutBtn.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadUserProfile() {
        firestore.collection(Constants.KEY_COLLECTION_USERS).document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // --- ROBUST NAME LOADING ---
                        String loadedName = documentSnapshot.getString(Constants.KEY_NAME);
                        if (loadedName == null)
                            loadedName = documentSnapshot.getString("fullName");
                        if (loadedName == null)
                            loadedName = documentSnapshot.getString("fName");

                        if (loadedName != null) {
                            mFullName.setText(loadedName);
                        }

                        // Set Email
                        String email = documentSnapshot.getString(Constants.KEY_EMAIL);
                        if (email != null) {
                            mEmail.setText(email);
                        }

                        // Set Phone
                        String phone = documentSnapshot.getString(Constants.KEY_PHONE);
                        if (phone != null) {
                            mPhone.setText(phone);
                        }

                        // Set Address
                        String address = documentSnapshot.getString("address");
                        if (address == null)
                            address = documentSnapshot.getString("location");
                        if (address != null) {
                            mAddress.setText(address);
                        }
                    } else {
                        showToast("Profile not found");
                    }
                })
                .addOnFailureListener(e -> showToast("Error fetching data"));
    }

    private void saveProfileChanges() {
        String name = mFullName.getText().toString().trim();
        String phone = mPhone.getText().toString().trim();
        String address = mAddress.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {
            showToast("Name, Phone and Address cannot be empty");
            return;
        }

        mSaveBtn.setEnabled(false);
        mSaveBtn.setText("Saving...");

        Map<String, Object> userMap = new HashMap<>();
        userMap.put(Constants.KEY_NAME, name);
        userMap.put("fullName", name); // Compatibility
        userMap.put("fName", name); // Compatibility
        userMap.put(Constants.KEY_PHONE, phone);
        userMap.put("address", address);

        firestore.collection(Constants.KEY_COLLECTION_USERS).document(userId)
                .set(userMap, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    showToast("Profile Saved Successfully");
                    mSaveBtn.setEnabled(true);
                    mSaveBtn.setText("SAVE CHANGES");
                })
                .addOnFailureListener(e -> {
                    showToast("Failed to update: " + e.getMessage());
                    mSaveBtn.setEnabled(true);
                    mSaveBtn.setText("SAVE CHANGES");
                });
    }
}
