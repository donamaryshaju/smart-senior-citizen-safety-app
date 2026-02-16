package com.example.seniorcitizensupport.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.seniorcitizensupport.BaseActivity;
import com.example.seniorcitizensupport.Constants;
import com.example.seniorcitizensupport.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;

public class HomeCareFormActivity extends BaseActivity {

    private String subType;
    private EditText inputDetails, inputTime;
    private Button btnSubmit;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homecare_form);

        subType = getIntent().getStringExtra("HOMECARE_SUBTYPE");
        if (subType == null)
            subType = "Home Care Assistance";

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initializeViews();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        btnSubmit.setOnClickListener(v -> fetchLocationAndSubmit());
    }

    private void initializeViews() {
        TextView title = findViewById(R.id.text_form_title);
        title.setText(subType);

        inputDetails = findViewById(R.id.input_details);
        inputTime = findViewById(R.id.input_time);
        btnSubmit = findViewById(R.id.btn_submit_homecare);
    }

    private void fetchLocationAndSubmit() {
        String details = inputDetails.getText().toString().trim();
        if (details.isEmpty()) {
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

        String time = inputTime.getText().toString().trim();
        String fullDetails = "Service: " + subType + "\nDetails: " + details;
        if (!time.isEmpty()) {
            fullDetails += "\nPreferred Time: " + time;
        }

        Map<String, Object> request = new HashMap<>();
        request.put("userId", auth.getCurrentUser().getUid());
        request.put("type", Constants.TYPE_HOMECARE);
        request.put("description", fullDetails);
        request.put("status", Constants.STATUS_PENDING);
        request.put("priority", "Normal");
        request.put("timestamp", FieldValue.serverTimestamp());
        request.put("location", "GPS Location Provided"); // Placeholder
        request.put("latitude", lat);
        request.put("longitude", lng);

        firestore.collection(Constants.KEY_COLLECTION_REQUESTS).add(request)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Home Care Request Sent!", Toast.LENGTH_SHORT).show();
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
