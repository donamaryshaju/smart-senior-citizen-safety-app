package com.example.seniorcitizensupport.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.seniorcitizensupport.BaseActivity;
import com.example.seniorcitizensupport.Constants;
import com.example.seniorcitizensupport.R;
import com.example.seniorcitizensupport.utils.NotificationHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;

public class MedicalFormActivity extends BaseActivity {

    private String subType;
    private EditText inputDetails;
    private CardView bannerEmergency;
    private LinearLayout groupUpload;
    private TextView textUploadStatus;
    private Button btnSubmit;
    private FusedLocationProviderClient fusedLocationClient;
    private boolean isEmergency = false;

    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri prescriptionUri = null; // Mock URI for now

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medical_form);

        subType = getIntent().getStringExtra("MEDICAL_SUBTYPE");
        if (subType == null)
            subType = "Medical Assistance";

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initializeViews();
        setupDynamicUI();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_upload).setOnClickListener(v -> openImagePicker());

        btnSubmit.setOnClickListener(v -> fetchLocationAndSubmit());
    }

    private void initializeViews() {
        TextView title = findViewById(R.id.text_form_title);
        title.setText(subType);

        inputDetails = findViewById(R.id.input_details);
        bannerEmergency = findViewById(R.id.banner_emergency);
        groupUpload = findViewById(R.id.group_upload);
        textUploadStatus = findViewById(R.id.text_upload_status);
        btnSubmit = findViewById(R.id.btn_submit_medical);
    }

    private void setupDynamicUI() {
        if (subType.contains("Emergency")) {
            isEmergency = true;
            bannerEmergency.setVisibility(View.VISIBLE);
            btnSubmit.setBackgroundColor(android.graphics.Color.RED);
            btnSubmit.setText("SEND EMERGENCY ALERT");
            inputDetails.setHint("Briefly describe the emergency (Optional)...");
        } else if (subType.contains("Prescription")) {
            groupUpload.setVisibility(View.VISIBLE);
            inputDetails.setHint("List medicines manually if not uploading image...");
        } else {
            // Doctor Visit
            inputDetails.setHint("Doctor Name, Hospital, Appointment Time...");
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Prescription"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            prescriptionUri = data.getData();
            textUploadStatus.setText("Image Selected");
            textUploadStatus.setTextColor(android.graphics.Color.parseColor("#43A047")); // Green
        }
    }

    private void fetchLocationAndSubmit() {
        String details = inputDetails.getText().toString().trim();
        if (details.isEmpty() && !isEmergency && prescriptionUri == null) {
            inputDetails.setError("Please provide details");
            return;
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, 100);
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Locating...");

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            double lat = 0.0, lng = 0.0;
            if (location != null) {
                lat = location.getLatitude();
                lng = location.getLongitude();
            }
            submitRequest(details, lat, lng);
        }).addOnFailureListener(e -> submitRequest(details, 0.0, 0.0));
    }

    private void submitRequest(String details, double lat, double lng) {
        btnSubmit.setText("Submitting...");

        // Fetch Address (Simplified: Just use lat/lng or "Current Location")
        // In real app, we'd reverse geocode here or use the address from profile

        String priority = isEmergency ? "High" : "Normal";
        String fullDetails = "Type: " + subType + "\nDetails: " + details;
        if (prescriptionUri != null) {
            fullDetails += "\n[Prescription Image Attached]"; // Placeholder for actual upload logic
        }

        Map<String, Object> request = new HashMap<>();
        request.put("userId", auth.getCurrentUser().getUid());
        request.put("type", Constants.TYPE_MEDICAL);
        request.put("description", fullDetails);
        request.put("status", Constants.STATUS_PENDING);
        request.put("priority", priority);
        request.put("timestamp", FieldValue.serverTimestamp());
        request.put("location", "GPS Location Provided"); // Placeholder
        request.put("latitude", lat);
        request.put("longitude", lng);

        firestore.collection(Constants.KEY_COLLECTION_REQUESTS).add(request)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Medical Request Sent!", Toast.LENGTH_SHORT).show();
                    if (isEmergency) {
                        NotificationHelper.sendNotification(null, "EMERGENCY ALERT", "New Medical Emergency Reported!");
                    }
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("TRY AGAIN");
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchLocationAndSubmit();
        }
    }
}
