package com.example.seniorcitizensupport.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.seniorcitizensupport.BaseActivity;
import com.example.seniorcitizensupport.Constants;
import com.example.seniorcitizensupport.R;
import com.example.seniorcitizensupport.model.RequestModel;
import com.example.seniorcitizensupport.utils.NotificationHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;

public class CreateRequestActivity extends BaseActivity {

    private String requestType;
    private View headerBackground;
    private TextView textTitle;
    private EditText inputDetails;
    private View layoutFileUpload;
    private Button btnSubmit;
    private ImageButton btnBack;
    private ImageView imgIllustration;

    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_request);

        requestType = getIntent().getStringExtra("EXTRA_REQUEST_TYPE");
        if (requestType == null)
            requestType = Constants.TYPE_MEDICAL;

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initializeViews();
        setupDynamicUI();
        setupListeners();
    }

    private void initializeViews() {
        headerBackground = findViewById(R.id.header_background);
        textTitle = findViewById(R.id.text_page_title);
        inputDetails = findViewById(R.id.input_details);
        layoutFileUpload = findViewById(R.id.layout_file_upload);
        btnSubmit = findViewById(R.id.btn_submit_request);
        btnBack = findViewById(R.id.btn_back);
        imgIllustration = findViewById(R.id.image_illustration);
    }

    private void setupDynamicUI() {
        textTitle.setText("Grocery Request");

        // Grocery Theme Colors (Green)
        int colorPrimary = Color.parseColor("#43A047");
        int colorLight = Color.parseColor("#E8F5E9");
        int illustrationRes = R.drawable.ic_grocery;
        String hintText = "Enter list of items (e.g. Bread, Milk, Rice...)";

        // Apply Colors
        headerBackground.setBackgroundColor(colorLight);
        textTitle.setTextColor(colorPrimary);
        btnSubmit.setBackgroundTintList(ColorStateList.valueOf(colorPrimary));
        inputDetails.setHint(hintText);
        layoutFileUpload.setVisibility(View.GONE); // No file upload for grocery for now

        // Apply Image
        try {
            imgIllustration.setImageResource(illustrationRes);
        } catch (Exception e) {
            imgIllustration.setImageResource(R.drawable.ic_grocery);
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSubmit.setOnClickListener(v -> {
            String details = inputDetails.getText().toString().trim();
            if (details.isEmpty()) {
                inputDetails.setError("Please enter details");
                return;
            }
            fetchLocationAndSubmit(details);
        });
    }

    private void fetchLocationAndSubmit(String description) {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, 100);
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Getting Location...");

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            double lat = 0.0, lng = 0.0;
            if (location != null) {
                lat = location.getLatitude();
                lng = location.getLongitude();
            }
            submitRequestToFirestore(description, lat, lng);
        }).addOnFailureListener(e -> {
            submitRequestToFirestore(description, 0.0, 0.0);
        });
    }

    private void submitRequestToFirestore(String description, double lat, double lng) {
        btnSubmit.setText("Submitting...");

        // Fetch Address first (reuse MainActivity logic effectively, or just fetch
        // directly here)
        firestore.collection(Constants.KEY_COLLECTION_USERS).document(auth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String address = "Address Not Provided";
                    if (documentSnapshot.exists() && documentSnapshot.getString("address") != null) {
                        address = documentSnapshot.getString("address");
                    }

                    Map<String, Object> request = new HashMap<>();
                    request.put("userId", auth.getCurrentUser().getUid());
                    request.put("type", requestType);
                    request.put("description", description);
                    request.put("status", Constants.STATUS_PENDING);
                    request.put("priority", "Normal");
                    request.put("timestamp", FieldValue.serverTimestamp());
                    request.put("location", address);
                    request.put("latitude", lat);
                    request.put("longitude", lng);

                    firestore.collection(Constants.KEY_COLLECTION_REQUESTS).add(request)
                            .addOnSuccessListener(ref -> {
                                Toast.makeText(this, "Request Submitted!", Toast.LENGTH_SHORT).show();
                                NotificationHelper.sendNotification(null, "New Request",
                                        requestType + " Request from Senior"); // Broadcast to volunteers potentially
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                btnSubmit.setEnabled(true);
                                btnSubmit.setText("Submit Request");
                            });
                })
                .addOnFailureListener(e -> {
                    // Proceed without address if fetch fails
                    // ... logic needed but for brevity assume success mostly or handle error
                    Toast.makeText(this, "Failed to fetch profile address", Toast.LENGTH_SHORT).show();
                    btnSubmit.setEnabled(true);
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            String details = inputDetails.getText().toString().trim();
            fetchLocationAndSubmit(details);
        } else {
            Toast.makeText(this, "Location permission needed", Toast.LENGTH_SHORT).show();
        }
    }
}
