package com.example.seniorcitizensupport.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.seniorcitizensupport.BaseActivity;
import com.example.seniorcitizensupport.Constants;
import com.example.seniorcitizensupport.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SeniorRegistrationActivity extends BaseActivity {

    private static final String TAG = "SeniorReg";

    // Layouts
    private LinearLayout layoutStep1, layoutStep2, layoutStep3;
    private TextView textStepIndicator;

    // Step 1 UI
    private EditText inputName, inputAge, inputPhone, inputEmail, inputPass, inputConfirmPass, inputAddress;
    private RadioGroup rgGender;

    // Step 2 UI
    private Spinner spinnerBlood;
    private CheckBox cbDiabetes, cbBP, cbHeart, cbArthritis, cbAsthma;
    private EditText inputOtherConditions, inputMeds, inputAllergies, inputDocName, inputDocPhone;

    // Step 3 UI
    private EditText ec1Name, ec1Phone, ec2Name, ec2Phone;
    private Spinner spinnerEc1Rel, spinnerEc2Rel;

    // Buttons
    private Button btnNext1, btnNext2, btnBack2, btnBack3, btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_senior_registration);

        initializeViews();
        setupSpinners();
        setupListeners();
    }

    private void initializeViews() {
        layoutStep1 = findViewById(R.id.layout_step_1);
        layoutStep2 = findViewById(R.id.layout_step_2);
        layoutStep3 = findViewById(R.id.layout_step_3);
        textStepIndicator = findViewById(R.id.text_step_indicator);

        // Step 1
        inputName = findViewById(R.id.input_full_name);
        inputAge = findViewById(R.id.input_age);
        inputPhone = findViewById(R.id.input_phone);
        inputEmail = findViewById(R.id.input_email);
        inputPass = findViewById(R.id.input_password);
        inputConfirmPass = findViewById(R.id.input_confirm_password);
        inputAddress = findViewById(R.id.input_address);
        rgGender = findViewById(R.id.radio_group_gender);
        btnNext1 = findViewById(R.id.btn_next_step1);

        // Step 2
        spinnerBlood = findViewById(R.id.spinner_blood_group);
        cbDiabetes = findViewById(R.id.cb_diabetes);
        cbBP = findViewById(R.id.cb_hypertension);
        cbHeart = findViewById(R.id.cb_heart);
        cbArthritis = findViewById(R.id.cb_arthritis);
        cbAsthma = findViewById(R.id.cb_asthma);
        inputOtherConditions = findViewById(R.id.input_other_conditions);
        inputMeds = findViewById(R.id.input_medications);
        inputAllergies = findViewById(R.id.input_allergies);
        inputDocName = findViewById(R.id.input_doctor_name);
        inputDocPhone = findViewById(R.id.input_doctor_phone);
        btnNext2 = findViewById(R.id.btn_next_step2);
        btnBack2 = findViewById(R.id.btn_back_step2);

        // Step 3
        ec1Name = findViewById(R.id.input_ec1_name);
        ec1Phone = findViewById(R.id.input_ec1_phone);
        spinnerEc1Rel = findViewById(R.id.spinner_ec1_relation);
        ec2Name = findViewById(R.id.input_ec2_name);
        ec2Phone = findViewById(R.id.input_ec2_phone);
        spinnerEc2Rel = findViewById(R.id.spinner_ec2_relation);
        btnBack3 = findViewById(R.id.btn_back_step3);
        btnRegister = findViewById(R.id.btn_register_final);
    }

    private void setupSpinners() {
        // Blood Group
        String[] bloodGroups = { "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-" };
        ArrayAdapter<String> bloodAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, bloodGroups);
        bloodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBlood.setAdapter(bloodAdapter);

        // Relationships
        String[] relations = { "Son", "Daughter", "Spouse", "Sibling", "Friend", "Neighbor", "Other" };
        ArrayAdapter<String> relAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, relations);
        relAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEc1Rel.setAdapter(relAdapter);
        spinnerEc2Rel.setAdapter(relAdapter);
    }

    private void setupListeners() {
        btnNext1.setOnClickListener(v -> {
            if (validateStep1()) {
                showStep(2);
            }
        });

        btnNext2.setOnClickListener(v -> showStep(3));
        btnBack2.setOnClickListener(v -> showStep(1));

        btnBack3.setOnClickListener(v -> showStep(2));
        btnRegister.setOnClickListener(v -> {
            if (validateStep3()) {
                registerUser();
            }
        });
    }

    private void showStep(int step) {
        layoutStep1.setVisibility(View.GONE);
        layoutStep2.setVisibility(View.GONE);
        layoutStep3.setVisibility(View.GONE);

        if (step == 1) {
            layoutStep1.setVisibility(View.VISIBLE);
            textStepIndicator.setText("Step 1 of 3: Basic Information");
        } else if (step == 2) {
            layoutStep2.setVisibility(View.VISIBLE);
            textStepIndicator.setText("Step 2 of 3: Medical Information");
        } else if (step == 3) {
            layoutStep3.setVisibility(View.VISIBLE);
            textStepIndicator.setText("Step 3 of 3: Emergency Contacts");
        }
    }

    private boolean validateStep1() {
        if (TextUtils.isEmpty(inputName.getText())) {
            inputName.setError("Required");
            return false;
        }
        if (TextUtils.isEmpty(inputAge.getText())) {
            inputAge.setError("Required");
            return false;
        }
        if (Integer.parseInt(inputAge.getText().toString()) < 60) {
            // inputAge.setError("Must be 60+"); // Relaxed for now or strictly 60? User
            // said 18 for Senior/Family, but "Senior" usually implies 60. Let's stick to 18
            // as generic check.
            if (Integer.parseInt(inputAge.getText().toString()) < 18) {
                inputAge.setError("Must be 18+");
                return false;
            }
        }
        if (rgGender.getCheckedRadioButtonId() == -1) {
            showToast("Select Gender");
            return false;
        }
        if (TextUtils.isEmpty(inputPhone.getText()) || inputPhone.getText().length() != 10) {
            inputPhone.setError("Invalid Phone");
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
        if (TextUtils.isEmpty(inputAddress.getText())) {
            inputAddress.setError("Required");
            return false;
        }

        return true;
    }

    private boolean validateStep3() {
        if (TextUtils.isEmpty(ec1Name.getText())) {
            ec1Name.setError("Required");
            return false;
        }
        if (TextUtils.isEmpty(ec1Phone.getText())) {
            ec1Phone.setError("Required");
            return false;
        }
        if (TextUtils.isEmpty(ec2Name.getText())) {
            ec2Name.setError("Required");
            return false;
        }
        if (TextUtils.isEmpty(ec2Phone.getText())) {
            ec2Phone.setError("Required");
            return false;
        }
        return true;
    }

    private void registerUser() {
        showProgressDialog("Creating Account...");

        String email = inputEmail.getText().toString().trim();
        String password = inputPass.getText().toString().trim();

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        saveUserData(auth.getCurrentUser().getUid());
                    } else {
                        hideProgressDialog();
                        showToast("Registration Failed: " + task.getException().getMessage());
                    }
                });
    }

    private void saveUserData(String uid) {
        Map<String, Object> user = new HashMap<>();

        // Basic
        user.put("id", uid);
        user.put("name", inputName.getText().toString().trim());
        user.put("email", inputEmail.getText().toString().trim());
        user.put("phone", inputPhone.getText().toString().trim());
        user.put("age", inputAge.getText().toString().trim());
        user.put("role", Constants.ROLE_SENIOR);
        user.put("userType", "senior_citizen"); // For compatibility
        user.put("address", inputAddress.getText().toString().trim());

        // Gender
        int genderId = rgGender.getCheckedRadioButtonId();
        RadioButton rb = findViewById(genderId);
        user.put("gender", rb.getText().toString());

        // Medical
        user.put("bloodGroup", spinnerBlood.getSelectedItem().toString());
        List<String> conditions = new ArrayList<>();
        if (cbDiabetes.isChecked())
            conditions.add("Diabetes");
        if (cbBP.isChecked())
            conditions.add("Hypertension");
        if (cbHeart.isChecked())
            conditions.add("Heart Disease");
        if (cbArthritis.isChecked())
            conditions.add("Arthritis");
        if (cbAsthma.isChecked())
            conditions.add("Asthma");
        String otherCond = inputOtherConditions.getText().toString().trim();
        if (!otherCond.isEmpty())
            conditions.add(otherCond);
        user.put("medicalConditions", conditions);

        user.put("medications", inputMeds.getText().toString().trim());
        user.put("allergies", inputAllergies.getText().toString().trim());
        user.put("doctorName", inputDocName.getText().toString().trim());
        user.put("doctorPhone", inputDocPhone.getText().toString().trim());

        // Emergency Contacts
        List<Map<String, String>> emergencyContacts = new ArrayList<>();

        Map<String, String> ec1 = new HashMap<>();
        ec1.put("name", ec1Name.getText().toString().trim());
        ec1.put("phone", ec1Phone.getText().toString().trim());
        ec1.put("relation", spinnerEc1Rel.getSelectedItem().toString());
        emergencyContacts.add(ec1);

        Map<String, String> ec2 = new HashMap<>();
        ec2.put("name", ec2Name.getText().toString().trim());
        ec2.put("phone", ec2Phone.getText().toString().trim());
        ec2.put("relation", spinnerEc2Rel.getSelectedItem().toString());
        emergencyContacts.add(ec2);

        user.put("emergencyContacts", emergencyContacts);

        firestore.collection(Constants.KEY_COLLECTION_USERS)
                .document(uid)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    hideProgressDialog();
                    showToast("Welcome! Registration Successful.");

                    Intent intent = new Intent(SeniorRegistrationActivity.this, SeniorDashActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    showToast("Error saving profile: " + e.getMessage());
                });
    }
}
