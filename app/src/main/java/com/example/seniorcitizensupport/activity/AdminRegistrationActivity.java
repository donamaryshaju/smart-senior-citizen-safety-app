package com.example.seniorcitizensupport.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.seniorcitizensupport.BaseActivity;
import com.example.seniorcitizensupport.Constants;
import com.example.seniorcitizensupport.R;
import com.example.seniorcitizensupport.model.User;

import java.util.HashMap;
import java.util.Map;

public class AdminRegistrationActivity extends BaseActivity {

    // Step 1 UI (Now Only Step)
    private EditText inputName, inputPhone, inputEmail, inputPass, inputConfirmPass;
    private Button btnRegister;

    // private static final String VALID_ADMIN_CODE = "ADMIN2026"; // Removed

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_registration);

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        // Step 1
        inputName = findViewById(R.id.input_full_name);
        inputPhone = findViewById(R.id.input_phone);
        inputEmail = findViewById(R.id.input_email);
        inputPass = findViewById(R.id.input_password);
        inputConfirmPass = findViewById(R.id.input_confirm_password);
        // inputAdminCode = findViewById(R.id.input_admin_code); // Removed
        btnRegister = findViewById(R.id.btn_register_final);
    }

    private void setupListeners() {
        btnRegister.setOnClickListener(v -> {
            if (validateStep1())
                registerUser();
        });
    }

    private boolean validateStep1() {
        if (TextUtils.isEmpty(inputName.getText())) {
            inputName.setError("Required");
            return false;
        }
        if (TextUtils.isEmpty(inputEmail.getText())) {
            inputEmail.setError("Required");
            return false;
        }
        if (TextUtils.isEmpty(inputPass.getText()) || inputPass.getText().length() < 6) {
            inputPass.setError("Min 6 chars");
            return false;
        }
        if (!inputPass.getText().toString().equals(inputConfirmPass.getText().toString())) {
            inputConfirmPass.setError("Mismatch");
            return false;
        }

        return true;
    }

    private void registerUser() {
        showProgressDialog("Creating Admin Account...");

        String email = inputEmail.getText().toString().trim();
        String password = inputPass.getText().toString().trim();

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        saveAdminData(auth.getCurrentUser().getUid());
                    } else {
                        hideProgressDialog();
                        showToast("Registration Failed: " + task.getException().getMessage());
                    }
                });
    }

    private void saveAdminData(String uid) {
        Map<String, Object> user = new HashMap<>();
        user.put("id", uid);
        user.put("name", inputName.getText().toString().trim());
        user.put("email", inputEmail.getText().toString().trim());
        user.put("phone", inputPhone.getText().toString().trim());
        user.put("role", Constants.ROLE_ADMIN);
        user.put("userType", "admin");
        user.put("isAdminApproved", true); // Auto-approve since they have the code

        firestore.collection(Constants.KEY_COLLECTION_USERS)
                .document(uid)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    hideProgressDialog();
                    showToast("Admin access granted.");
                    Intent intent = new Intent(AdminRegistrationActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    showToast("Error saving data");
                });
    }
}
