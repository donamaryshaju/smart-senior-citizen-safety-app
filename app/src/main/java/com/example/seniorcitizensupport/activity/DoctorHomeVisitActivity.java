package com.example.seniorcitizensupport.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.seniorcitizensupport.BaseActivity;
import com.example.seniorcitizensupport.Constants;
import com.example.seniorcitizensupport.R;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;

public class DoctorHomeVisitActivity extends BaseActivity {

    private Spinner spinnerType, spinnerTime;
    private EditText inputAddress;
    private Button btnBook;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_home_visit);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        spinnerType = findViewById(R.id.spinner_doctor_type);
        spinnerTime = findViewById(R.id.spinner_time_slot);
        inputAddress = findViewById(R.id.input_address);
        btnBook = findViewById(R.id.btn_book_home_visit);

        // Populate Spinners
        String[] types = { "General Physician", "Cardiologist", "Diabetologist", "Physiotherapist", "Nurse" };
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);

        String[] times = { "Morning (9AM - 12PM)", "Afternoon (12PM - 4PM)", "Evening (4PM - 8PM)" };
        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, times);
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTime.setAdapter(timeAdapter);

        // Pre-fill address if available in User model
        fetchUserAddress();

        btnBook.setOnClickListener(v -> bookHomeVisit());
    }

    private void fetchUserAddress() {
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            firestore.collection(Constants.KEY_COLLECTION_USERS)
                    .document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String address = documentSnapshot.getString("address");
                            if (address != null && !address.isEmpty()) {
                                inputAddress.setText(address);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Ignore failure, just don't pre-fill
                    });
        }
    }

    private void bookHomeVisit() {
        String address = inputAddress.getText().toString().trim();
        if (TextUtils.isEmpty(address)) {
            inputAddress.setError("Address Required");
            return;
        }

        showProgressDialog("Booking Home Visit...");

        Map<String, Object> request = new HashMap<>();
        request.put("userId", auth.getCurrentUser().getUid());
        request.put("type", "Doctor Home Visit");
        request.put("doctorType", spinnerType.getSelectedItem().toString());
        request.put("timeSlot", spinnerTime.getSelectedItem().toString());
        request.put("address", address);

        String description = String.format("Request: Doctor Home Visit\nSpecialist: %s\nTime Slot: %s\nAddress: %s",
                spinnerType.getSelectedItem().toString(), spinnerTime.getSelectedItem().toString(), address);

        request.put("description", description);
        request.put("status", Constants.STATUS_PENDING);
        request.put("priority", "High"); // Home visits are usually high priority
        request.put("timestamp", FieldValue.serverTimestamp());
        request.put("location", address);
        // Can add lat/lng if we geocode the address

        firestore.collection(Constants.KEY_COLLECTION_REQUESTS)
                .add(request)
                .addOnSuccessListener(ref -> {
                    hideProgressDialog();
                    showToast("Home Visit Booked! A doctor will confirm soon.");
                    finish();
                })
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    showToast("Failed: " + e.getMessage());
                });
    }
}
