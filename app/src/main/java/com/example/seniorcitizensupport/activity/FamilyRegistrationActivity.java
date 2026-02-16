package com.example.seniorcitizensupport.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
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
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.example.seniorcitizensupport.model.User;

import java.util.HashMap;
import java.util.Map;

public class FamilyRegistrationActivity extends BaseActivity {

    // Layouts
    private LinearLayout layoutStep1, layoutStep2;
    private TextView textStepIndicator;

    // Step 1 UI
    private EditText inputName, inputAge, inputPhone, inputEmail, inputPass, inputConfirmPass, inputAddress;
    private RadioGroup rgGender;
    private Button btnNext1;

    // Step 2 UI
    private Spinner spinnerRelationship;
    private RadioGroup rgLinkMethod;
    private TextInputEditText inputSearchQuery;
    private Button btnSearch, btnConfirmSenior, btnBack2, btnRegister;
    private MaterialCardView cardSeniorInfo;
    private TextView textSeniorName, textSeniorPhone;
    private CheckBox cbAddEmergency;

    private String linkedSeniorId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family_registration);

        initializeViews();
        setupSpinners();
        setupListeners();
    }

    private void initializeViews() {
        layoutStep1 = findViewById(R.id.layout_step_1);
        layoutStep2 = findViewById(R.id.layout_step_2);
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
        spinnerRelationship = findViewById(R.id.spinner_relationship);
        rgLinkMethod = findViewById(R.id.rg_link_method);
        inputSearchQuery = findViewById(R.id.input_search_query);
        btnSearch = findViewById(R.id.btn_search_senior);
        cardSeniorInfo = findViewById(R.id.card_senior_info);
        textSeniorName = findViewById(R.id.text_senior_name);
        textSeniorPhone = findViewById(R.id.text_senior_phone);
        btnConfirmSenior = findViewById(R.id.btn_confirm_senior);
        cbAddEmergency = findViewById(R.id.cb_add_emergency);
        btnBack2 = findViewById(R.id.btn_back_step2);
        btnRegister = findViewById(R.id.btn_register_final);
    }

    private void setupSpinners() {
        String[] relations = { "Son", "Daughter", "Spouse", "Grandchild", "Sibling", "Nephew/Niece", "Other" };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, relations);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRelationship.setAdapter(adapter);
    }

    private void setupListeners() {
        btnNext1.setOnClickListener(v -> {
            if (validateStep1())
                showStep(2);
        });

        btnBack2.setOnClickListener(v -> showStep(1));

        rgLinkMethod.setOnCheckedChangeListener((group, checkedId) -> {
            inputSearchQuery.setText("");
            if (checkedId == R.id.rb_search_phone) {
                inputSearchQuery.setHint("Enter Senior's Phone Number");
                inputSearchQuery.setInputType(InputType.TYPE_CLASS_PHONE);
            } else {
                inputSearchQuery.setHint("Enter Senior's Email");
                inputSearchQuery.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            }
        });

        btnSearch.setOnClickListener(v -> searchSenior());

        btnConfirmSenior.setOnClickListener(v -> {
            cardSeniorInfo.setBackgroundColor(getResources().getColor(R.color.teal_200));
            btnRegister.setEnabled(true);
            showToast("Senior Linked!");
        });

        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void showStep(int step) {
        layoutStep1.setVisibility(View.GONE);
        layoutStep2.setVisibility(View.GONE);

        if (step == 1) {
            layoutStep1.setVisibility(View.VISIBLE);
            textStepIndicator.setText("Step 1 of 2: Basic Information");
        } else if (step == 2) {
            layoutStep2.setVisibility(View.VISIBLE);
            textStepIndicator.setText("Step 2 of 2: Link Senior");
        }
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

    private void searchSenior() {
        String query = inputSearchQuery.getText().toString().trim();
        if (query.isEmpty()) {
            inputSearchQuery.setError("Enter search term");
            return;
        }

        showProgressDialog("Searching...");

        String searchField = (rgLinkMethod.getCheckedRadioButtonId() == R.id.rb_search_phone) ? "phone" : "email";

        firestore.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo("role", Constants.ROLE_SENIOR)
                .whereEqualTo(searchField, query)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    hideProgressDialog();
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Found
                        com.google.firebase.firestore.DocumentSnapshot doc = queryDocumentSnapshots.getDocuments()
                                .get(0);
                        linkedSeniorId = doc.getId();
                        String name = doc.getString("name");
                        String phone = doc.getString("phone");

                        textSeniorName.setText("Name: " + name);
                        textSeniorPhone.setText("Phone: " + phone);
                        cardSeniorInfo.setVisibility(View.VISIBLE);
                    } else {
                        showToast("No Senior Citizen found with this detail.");
                        cardSeniorInfo.setVisibility(View.GONE);
                        btnRegister.setEnabled(false);
                    }
                })
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    showToast("Error searching: " + e.getMessage());
                });
    }

    private void registerUser() {
        if (linkedSeniorId == null) {
            showToast("Please link a senior citizen first");
            return;
        }

        showProgressDialog("Creating Account...");

        String email = inputEmail.getText().toString().trim();
        String password = inputPass.getText().toString().trim();

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        saveFamilyData(auth.getCurrentUser().getUid());
                    } else {
                        hideProgressDialog();
                        showToast("Registration Failed: " + task.getException().getMessage());
                    }
                });
    }

    private void saveFamilyData(String uid) {
        Map<String, Object> user = new HashMap<>();
        user.put("id", uid);
        user.put("name", inputName.getText().toString().trim());
        user.put("email", inputEmail.getText().toString().trim());
        user.put("phone", inputPhone.getText().toString().trim());
        user.put("role", Constants.ROLE_FAMILY);
        user.put("userType", "family_member");
        user.put("address", inputAddress.getText().toString().trim());

        user.put("linkedSeniorId", linkedSeniorId);
        user.put("linkStatus", "pending_approval");
        user.put("relationship", spinnerRelationship.getSelectedItem().toString());
        user.put("isEmergencyContact", cbAddEmergency.isChecked());

        firestore.collection(Constants.KEY_COLLECTION_USERS)
                .document(uid)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    hideProgressDialog();
                    showToast("Request Sent! Waiting for approval.");
                    Intent intent = new Intent(FamilyRegistrationActivity.this, LoginActivity.class);
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
