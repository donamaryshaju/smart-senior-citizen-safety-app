package com.example.seniorcitizensupport.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.seniorcitizensupport.BaseActivity;
import com.example.seniorcitizensupport.Constants;
import com.example.seniorcitizensupport.R;
import com.example.seniorcitizensupport.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VolunteerRegistrationActivity extends BaseActivity {

    // Layouts
    private LinearLayout layoutStep1, layoutStep2, layoutStep3;
    private TextView textStepIndicator;

    // Step 1 UI
    private EditText inputName, inputAge, inputPhone, inputEmail, inputPass, inputConfirmPass, inputAddress;
    private RadioGroup rgGender;
    private Button btnNext1;

    // Step 2 UI
    private CheckBox cbMedical, cbGrocery, cbTransport, cbCleaning, cbNursing, cbMaintenance;
    private RadioGroup rgVehicle;
    private EditText inputCoverage;
    private Button btnNext2, btnBack2;

    // Step 3 UI
    private Button btnUploadID, btnBack3, btnRegister;
    private TextView textIdStatus;
    private CheckBox cbConsent;
    private boolean isIdUploaded = false; // Mock

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_volunteer_registration);

        initializeViews();
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
        cbMedical = findViewById(R.id.cb_medical);
        cbGrocery = findViewById(R.id.cb_grocery);
        cbTransport = findViewById(R.id.cb_transport);
        cbCleaning = findViewById(R.id.cb_cleaning);
        cbNursing = findViewById(R.id.cb_nursing);
        cbMaintenance = findViewById(R.id.cb_maintenance);
        rgVehicle = findViewById(R.id.rg_vehicle);
        inputCoverage = findViewById(R.id.input_coverage);
        btnNext2 = findViewById(R.id.btn_next_step2);
        btnBack2 = findViewById(R.id.btn_back_step2);

        // Step 3
        btnUploadID = findViewById(R.id.btn_upload_id);
        textIdStatus = findViewById(R.id.text_id_status);
        cbConsent = findViewById(R.id.cb_consent);
        btnBack3 = findViewById(R.id.btn_back_step3);
        btnRegister = findViewById(R.id.btn_register_final);
    }

    private void setupListeners() {
        btnNext1.setOnClickListener(v -> {
            if (validateStep1())
                showStep(2);
        });

        btnBack2.setOnClickListener(v -> showStep(1));
        btnNext2.setOnClickListener(v -> {
            if (validateStep2())
                showStep(3);
        });

        btnBack3.setOnClickListener(v -> showStep(2));

        btnUploadID.setOnClickListener(v -> showImagePickerOptions());

        btnRegister.setOnClickListener(v -> {
            if (validateStep3())
                registerUser();
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
            textStepIndicator.setText("Step 2 of 3: Professional Info");
        } else if (step == 3) {
            layoutStep3.setVisibility(View.VISIBLE);
            textStepIndicator.setText("Step 3 of 3: Verification");
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

    private boolean validateStep2() {
        if (rgVehicle.getCheckedRadioButtonId() == -1) {
            showToast("Select Vehicle Status");
            return false;
        }
        if (TextUtils.isEmpty(inputCoverage.getText())) {
            inputCoverage.setError("Required");
            return false;
        }
        return true;
    }

    private boolean validateStep3() {
        if (!isIdUploaded) {
            showToast("Please upload ID Proof");
            return false;
        }
        if (!cbConsent.isChecked()) {
            showToast("Consent required");
            return false;
        }
        return true;
    }

    // --- Image Upload Logic ---

    // Permission Request Code
    private static final int CAMERA_PERMISSION_CODE = 100;
    private android.net.Uri photoURI;
    private String base64IdProof;

    private void showImagePickerOptions() {
        String[] options = { "Take Photo", "Choose from Gallery" };
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Upload ID Proof");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                checkCameraPermissionAndOpen();
            } else {
                openGallery();
            }
        });
        builder.show();
    }

    private void checkCameraPermissionAndOpen() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(this,
                    new String[] { android.Manifest.permission.CAMERA },
                    CAMERA_PERMISSION_CODE);
        } else {
            openCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @androidx.annotation.NonNull String[] permissions,
            @androidx.annotation.NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                showToast("Camera permission is required to take photos.");
            }
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            java.io.File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (java.io.IOException ex) {
                showToast("Error creating image file");
            }
            if (photoFile != null) {
                photoURI = androidx.core.content.FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".fileprovider",
                        photoFile);
                takePictureIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, photoURI);
                cameraLauncher.launch(takePictureIntent);
            }
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        galleryLauncher.launch(Intent.createChooser(intent, "Select Picture"));
    }

    private java.io.File createImageFile() throws java.io.IOException {
        String timeStamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                .format(new java.util.Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        java.io.File storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        return java.io.File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    androidx.activity.result.ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    processImage(photoURI);
                }
            });

    androidx.activity.result.ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    photoURI = result.getData().getData();
                    processImage(photoURI);
                }
            });

    private void processImage(android.net.Uri imageUri) {
        textIdStatus.setText("Processing Image...");
        new Thread(() -> {
            String encoded = encodeImageToBase64(imageUri);
            runOnUiThread(() -> {
                if (encoded != null) {
                    base64IdProof = encoded;
                    isIdUploaded = true;
                    textIdStatus.setText("ID Proof Uploaded Successfully");
                    textIdStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                } else {
                    isIdUploaded = false;
                    textIdStatus.setText("Error processing image");
                    textIdStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                }
            });
        }).start();
    }

    private String encodeImageToBase64(android.net.Uri imageUri) {
        try {
            android.graphics.Bitmap bitmap = android.provider.MediaStore.Images.Media.getBitmap(getContentResolver(),
                    imageUri);

            // Resize if too big (max 800px width/height)
            int maxWidth = 800;
            int maxHeight = 800;
            float scale = Math.min(((float) maxWidth / bitmap.getWidth()), ((float) maxHeight / bitmap.getHeight()));

            android.graphics.Matrix matrix = new android.graphics.Matrix();
            matrix.postScale(scale, scale);

            android.graphics.Bitmap resizedBitmap = android.graphics.Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            java.io.ByteArrayOutputStream byteArrayOutputStream = new java.io.ByteArrayOutputStream();
            resizedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            return "data:image/jpeg;base64,"
                    + android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT);

        } catch (java.io.IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void registerUser() {
        showProgressDialog("Submitting Application...");

        String email = inputEmail.getText().toString().trim();
        String password = inputPass.getText().toString().trim();

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        saveVolunteerData(auth.getCurrentUser().getUid());
                    } else {
                        hideProgressDialog();
                        showToast("Registration Failed: " + task.getException().getMessage());
                    }
                });
    }

    private void saveVolunteerData(String uid) {
        showProgressDialog("Saving Data...");
        Map<String, Object> user = new HashMap<>();
        user.put("id", uid);
        user.put("name", inputName.getText().toString().trim());
        user.put("email", inputEmail.getText().toString().trim());
        user.put("phone", inputPhone.getText().toString().trim());
        user.put("role", Constants.ROLE_VOLUNTEER);
        user.put("userType", "volunteer");
        String addressStr = inputAddress.getText().toString().trim();
        user.put("address", addressStr);
        user.put("isVerified", false);
        user.put("verificationStatus", "pending");

        // Geocode Address
        android.location.Geocoder geocoder = new android.location.Geocoder(this, java.util.Locale.getDefault());
        try {
            List<android.location.Address> addresses = geocoder.getFromLocationName(addressStr, 1);
            if (addresses != null && !addresses.isEmpty()) {
                android.location.Address location = addresses.get(0);
                user.put("latitude", location.getLatitude());
                user.put("longitude", location.getLongitude());
            } else {
                // Default or 0.0 if not found, logic will handle 0.0 as "unknown"
                user.put("latitude", 0.0);
                user.put("longitude", 0.0);
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
            user.put("latitude", 0.0);
            user.put("longitude", 0.0);
        }

        if (base64IdProof != null) {
            user.put("idProofImage", base64IdProof);
        }

        // Services
        List<String> services = new ArrayList<>();
        if (cbMedical.isChecked())
            services.add("Medical");
        if (cbGrocery.isChecked())
            services.add("Grocery");
        if (cbTransport.isChecked())
            services.add("Transport");
        if (cbCleaning.isChecked())
            services.add("Cleaning");
        if (cbNursing.isChecked())
            services.add("Nursing");
        services.add(inputCoverage.getText().toString());
        user.put("services", services);

        firestore.collection(Constants.KEY_COLLECTION_USERS)
                .document(uid)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    hideProgressDialog();
                    showToast("Application Submitted! Pending Approval.");
                    Intent intent = new Intent(VolunteerRegistrationActivity.this, LoginActivity.class);
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
