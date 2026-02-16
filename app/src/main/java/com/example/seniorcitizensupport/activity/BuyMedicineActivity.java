package com.example.seniorcitizensupport.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.seniorcitizensupport.Constants;
import com.example.seniorcitizensupport.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BuyMedicineActivity extends AppCompatActivity {

    // Views
    private LinearLayout layoutStep1, layoutStep2;
    private CardView cardUpload, cardText, cardRepeat; // Renamed cardVoice to cardText
    private Button btnContinue, btnSubmit;
    private ImageView btnBack;

    // Step 2 Views
    private TextView tvSelectedMethodText, btnChangeMethod, tvDeliveryAddress;
    private LinearLayout layoutInputCamera, layoutInputText, layoutInputRepeat; // Renamed layoutInputVoice to
                                                                                // layoutInputText
    private ImageView imgPreview;
    private Button btnRetakePhoto;
    private EditText etMedicineList; // New EditText
    private RadioGroup radioGroupTime;

    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int LOCATION_PERMISSION_CODE = 101; // Added Location Permission Code
    private Uri photoURI;
    private boolean isPrescriptionUploaded = false;
    private String selectedMethod = "";
    private int currentStep = 1;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // Location
    private com.google.android.gms.location.FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buy_medicine);

        // Init Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Init Location Client
        fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this);

        // Initialize Views
        initViews();

        // Listeners for Step 1
        cardUpload.setOnClickListener(v -> selectMethod("Upload"));
        cardText.setOnClickListener(v -> selectMethod("Text")); // Changed from Volce to Text
        cardRepeat.setOnClickListener(v -> fetchLastOrderAndPopulate()); // Changed action
        btnContinue.setOnClickListener(v -> goToStep2());

        // Listeners for Step 2
        btnChangeMethod.setOnClickListener(v -> goToStep1());
        btnRetakePhoto.setOnClickListener(v -> showImagePickerOptions());

        btnBack.setOnClickListener(v -> {
            if (currentStep == 2) {
                goToStep1();
            } else {
                finish();
            }
        });

        // Enable Submit Button only when a time is selected
        radioGroupTime.setOnCheckedChangeListener((group, checkedId) -> checkSubmitValidity());

        btnSubmit.setOnClickListener(v -> checkLocationAndSubmit());
    }

    private void initViews() {
        layoutStep1 = findViewById(R.id.layoutStep1);
        layoutStep2 = findViewById(R.id.layoutStep2);

        cardUpload = findViewById(R.id.cardUpload);
        cardText = findViewById(R.id.cardText); // was cardVoice
        cardRepeat = findViewById(R.id.cardRepeat);

        btnContinue = findViewById(R.id.btnContinue);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnBack = findViewById(R.id.btnBack);

        tvSelectedMethodText = findViewById(R.id.tvSelectedMethodText);
        btnChangeMethod = findViewById(R.id.btnChangeMethod);

        layoutInputCamera = findViewById(R.id.layoutInputCamera);
        layoutInputText = findViewById(R.id.layoutInputText); // was layoutInputVoice
        layoutInputRepeat = findViewById(R.id.layoutInputRepeat);

        imgPreview = findViewById(R.id.imgPreview);
        btnRetakePhoto = findViewById(R.id.btnRetakePhoto);
        etMedicineList = findViewById(R.id.etMedicineList); // Init EditText
        etMedicineList = findViewById(R.id.etMedicineList); // Init EditText
        radioGroupTime = findViewById(R.id.radioGroupTime);
        tvDeliveryAddress = findViewById(R.id.tvDeliveryAddress);

        fetchUserAddress();
    }

    private void fetchUserAddress() {
        if (auth.getCurrentUser() != null) {
            db.collection(Constants.KEY_COLLECTION_USERS)
                    .document(auth.getCurrentUser().getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String address = documentSnapshot.getString("address");
                            if (address != null && !address.isEmpty()) {
                                tvDeliveryAddress.setText(address);
                            } else {
                                tvDeliveryAddress.setText("No address found. Please update profile.");
                            }
                        } else {
                            tvDeliveryAddress.setText("User details not found.");
                        }
                    })
                    .addOnFailureListener(e -> tvDeliveryAddress.setText("Error loading address"));
        }
    }

    @Override
    public void onBackPressed() {
        if (currentStep == 2) {
            goToStep1();
        } else {
            super.onBackPressed();
        }
    }

    // --- STEP 1 LOGIC ---

    private void selectMethod(String method) {
        selectedMethod = method;

        // Reset styles
        resetCardStyle(cardUpload);
        resetCardStyle(cardText);
        resetCardStyle(cardRepeat);

        // Highlight selected
        if (method.equals("Upload"))
            highlightCard(cardUpload);
        else if (method.equals("Text"))
            highlightCard(cardText);
        else if (method.equals("Repeat"))
            highlightCard(cardRepeat);

        // Show Continue button
        btnContinue.setVisibility(View.VISIBLE);
    }

    private void resetCardStyle(CardView card) {
        card.setCardBackgroundColor(getResources().getColor(R.color.card_bg));
    }

    private void highlightCard(CardView card) {
        card.setCardBackgroundColor(getResources().getColor(R.color.light_blue_bg));
    }

    private void goToStep2() {
        currentStep = 2;
        layoutStep1.setVisibility(View.GONE);
        layoutStep2.setVisibility(View.VISIBLE);

        // Setup Step 2 UI
        layoutInputCamera.setVisibility(View.GONE);
        layoutInputText.setVisibility(View.GONE);
        layoutInputRepeat.setVisibility(View.GONE);

        // Reset inputs when entering fresh (optional, but good for cleanliness)
        // etMedicineList.setText("");
        // We keep it if populated by Repeat Order

        if (selectedMethod.equals("Upload")) {
            layoutInputCamera.setVisibility(View.VISIBLE);
            tvSelectedMethodText.setText("Upload Prescription Selected");
            if (!isPrescriptionUploaded) {
                showImagePickerOptions();
            }
        } else if (selectedMethod.equals("Text")) {
            layoutInputText.setVisibility(View.VISIBLE);
            tvSelectedMethodText.setText("Type Medicine List");
        }
        /* Repeat logic is handled by pre-filling Text and selecting Text method */

        checkSubmitValidity();
    }

    private void goToStep1() {
        currentStep = 1;
        layoutStep2.setVisibility(View.GONE);
        layoutStep1.setVisibility(View.VISIBLE);
        btnContinue.setVisibility(View.GONE); // Hide continue button when going back to step 1
        selectedMethod = ""; // Reset selected method
        isPrescriptionUploaded = false; // Reset prescription status
        imgPreview.setImageURI(null); // Clear image preview
        imgPreview.setImageResource(R.drawable.ic_camera_alt); // Set default camera icon
        etMedicineList.setText(""); // Clear text
        radioGroupTime.clearCheck(); // Clear selected radio button
        resetCardStyle(cardUpload);
        resetCardStyle(cardText);
        resetCardStyle(cardRepeat);
    }

    // --- STEP 2 LOGIC ---

    private void checkSubmitValidity() {
        boolean isTimeSelected = radioGroupTime.getCheckedRadioButtonId() != -1;
        boolean isMethodValid = true;

        if (selectedMethod.equals("Upload") && !isPrescriptionUploaded) {
            isMethodValid = false;
        }
        // Text method is valid if they selected it (we can add check for empty text if
        // stricter validation needed)
        // For now, let's trust the user or check if empty on submit

        if (isTimeSelected && isMethodValid) {
            btnSubmit.setEnabled(true);
            btnSubmit.setAlpha(1.0f);
        } else {
            btnSubmit.setEnabled(false);
            btnSubmit.setAlpha(0.5f);
        }
    }

    // --- REPEAT ORDER LOGIC (FETCH FROM FIREBASE) ---
    private void fetchLastOrderAndPopulate() {
        if (auth.getCurrentUser() == null) {
            showToast("User not logged in");
            return;
        }

        Toast.makeText(this, "Fetching last order...", Toast.LENGTH_SHORT).show();

        db.collection(Constants.KEY_COLLECTION_REQUESTS)
                .whereEqualTo("userId", auth.getCurrentUser().getUid())
                // .whereEqualTo("type", Constants.TYPE_MEDICAL) // Removed to avoid composite
                // index requirement
                // .orderBy("timestamp", Query.Direction.DESCENDING) // Removed to avoid
                // composite index requirement
                // .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Client-side filtering and sorting
                        java.util.List<com.google.firebase.firestore.DocumentSnapshot> docs = queryDocumentSnapshots
                                .getDocuments();
                        com.google.firebase.firestore.DocumentSnapshot lastOrder = null;
                        long latestTime = 0;

                        for (com.google.firebase.firestore.DocumentSnapshot doc : docs) {
                            String type = doc.getString("type");
                            if (Constants.TYPE_MEDICAL.equals(type)) {
                                java.util.Date date = null;
                                // Handle Timestamp or Date objects safely
                                Object ts = doc.get("timestamp");
                                if (ts instanceof com.google.firebase.Timestamp) {
                                    date = ((com.google.firebase.Timestamp) ts).toDate();
                                } else if (ts instanceof java.util.Date) {
                                    date = (java.util.Date) ts;
                                }

                                if (date != null && date.getTime() > latestTime) {
                                    latestTime = date.getTime();
                                    lastOrder = doc;
                                }
                            }
                        }

                        if (lastOrder != null) {
                            String description = lastOrder.getString("description");
                            String imageUrl = lastOrder.getString("imageUrl");

                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                // Image Order
                                reusedImageString = imageUrl;
                                isPrescriptionUploaded = true;
                                selectMethod("Upload");

                                // Load image into preview
                                com.bumptech.glide.Glide.with(this)
                                        .load(imageUrl)
                                        .placeholder(R.drawable.ic_camera_alt)
                                        .into(imgPreview);

                                showToast("Last order image loaded.");
                            } else if (description != null) {
                                // Text Order
                                reusedImageString = null;
                                isPrescriptionUploaded = false;

                                // Clean up description if it has "Details: " prefix
                                String cleanDesc = description.replace("Type: Medical Assistance\n", "")
                                        .replace("Type: Prescription Purchase\n", "")
                                        .replace("Type: Medicine Order\n", "")
                                        .replaceAll("Details: ", "")
                                        .replaceAll("Delivery: .*$", "")
                                        .trim();

                                etMedicineList.setText(cleanDesc);
                                selectMethod("Text");
                                showToast("Last order details loaded.");
                            } else {
                                showToast("Last order has no details.");
                                selectMethod("Text");
                            }
                        } else {
                            showToast("No previous medical orders found.");
                            selectMethod("Text");
                        }
                    } else {
                        showToast("No previous orders found.");
                        selectMethod("Text");
                    }
                })
                .addOnFailureListener(e -> {
                    showToast("Failed to fetch order: " + e.getMessage());
                    selectMethod("Text");
                });
    }

    // --- Camera & Gallery Logic ---

    private void showImagePickerOptions() {
        String[] options = { "Take Photo", "Choose from Gallery" };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Upload Prescription");
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA },
                    CAMERA_PERMISSION_CODE);
        } else {
            openCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                showToast("Camera permission is required to take photos.");
            }
        } else if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationAndSubmit(); // Retry submit
            } else {
                showToast("Location permission is needed to find nearby volunteers.");
                // Optionally submit with 0.0, but better to enforce or warn.
                // For now, let's try to submit with 0.0 if they deny, seamlessly.
                uploadImageAndSubmit(0.0, 0.0);
            }
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                showToast("Error creating image file");
            }
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                cameraLauncher.launch(takePictureIntent);
            }
        }
    }

    private void openGallery() {
        // CHANGED: Use ACTION_GET_CONTENT instead of ACTION_PICK for better
        // compatibility
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        galleryLauncher.launch(Intent.createChooser(intent, "Select Picture"));
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    isPrescriptionUploaded = true;
                    imgPreview.setImageURI(photoURI);
                    checkSubmitValidity();
                    showToast("Prescription Captured!");
                }
            });

    ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImage = result.getData().getData();
                    // Keep referencing this URI. Note: For permanent access, might need
                    // takePersistableUriPermission
                    // but for immediate upload/viewing in this session, it's usually fine.
                    photoURI = selectedImage; // Update photoURI to this new one
                    isPrescriptionUploaded = true;
                    imgPreview.setImageURI(selectedImage);
                    checkSubmitValidity();
                    showToast("Prescription Selected!");
                }
            });

    // --- SUBMIT LOGIC ---

    private void submitRequestToFirestore(Map<String, Object> request) {
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Submitting...");

        db.collection(Constants.KEY_COLLECTION_REQUESTS).add(request)
                .addOnSuccessListener(documentReference -> {
                    showToast("Order Sent to Volunteers!");
                    finish();
                })
                .addOnFailureListener(e -> {
                    showToast("Error: " + e.getMessage());
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("SUBMIT REQUEST");
                });
    }

    private void checkLocationAndSubmit() {
        if (radioGroupTime.getCheckedRadioButtonId() == -1) {
            showToast("Please select a delivery time");
            return;
        }
        if (auth.getCurrentUser() == null) {
            showToast("User not logged in");
            return;
        }
        if ("Text".equals(selectedMethod) && etMedicineList.getText().toString().trim().isEmpty()) {
            showToast("Please list your medicines");
            return;
        }

        // Check Location Permissions
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                    LOCATION_PERMISSION_CODE);
            return;
        }

        btnSubmit.setText("Locating...");
        btnSubmit.setEnabled(false);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        uploadImageAndSubmit(location.getLatitude(), location.getLongitude());
                    } else {
                        // Location null, try submitting with 0.0 or request updates (keeping it simple:
                        // 0.0)
                        showToast("Unable to get current location. Submitting anyway.");
                        uploadImageAndSubmit(0.0, 0.0);
                    }
                })
                .addOnFailureListener(e -> {
                    showToast("Location error: " + e.getMessage() + ". Submitting anyway.");
                    uploadImageAndSubmit(0.0, 0.0);
                });
    }

    private String reusedImageString = null;

    private void uploadImageAndSubmit(double latitude, double longitude) {
        if ("Upload".equals(selectedMethod)) {
            if (photoURI != null) {
                btnSubmit.setText("Processing Image...");

                // Run encoding in background to avoid blocking UI
                new Thread(() -> {
                    String base64Image = encodeImageToBase64(photoURI);
                    runOnUiThread(() -> {
                        if (base64Image != null) {
                            submitOrder(latitude, longitude, base64Image);
                        } else {
                            showToast("Image processing failed. Submitting without image.");
                            submitOrder(latitude, longitude, null);
                        }
                    });
                }).start();
            } else if (reusedImageString != null) {
                // Reuse the existing image string (Base64 or URL)
                btnSubmit.setText("Submitting...");
                submitOrder(latitude, longitude, reusedImageString);
            } else {
                // Fallback (shouldn't happen if validation works)
                submitOrder(latitude, longitude, null);
            }
        } else {
            submitOrder(latitude, longitude, null);
        }
    }

    private String encodeImageToBase64(Uri imageUri) {
        try {
            android.graphics.Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

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

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void submitOrder(double latitude, double longitude, String imageUrl) {
        String deliveryTime = "";
        int selectedTimeId = radioGroupTime.getCheckedRadioButtonId();
        if (selectedTimeId == R.id.radioUrgent)
            deliveryTime = "Urgent (within 2 hours)";
        else if (selectedTimeId == R.id.radioToday)
            deliveryTime = "Today Evening";
        else if (selectedTimeId == R.id.radioTomorrow)
            deliveryTime = "Tomorrow Morning";

        String details = "";
        if ("Text".equals(selectedMethod)) {
            details = etMedicineList.getText().toString().trim();
        } else if ("Upload".equals(selectedMethod)) {
            details = "Prescription Uploaded";
            if (imageUrl == null) {
                details += "\n[Image Upload Failed]";
            }
        }

        // Construct Request
        Map<String, Object> request = new HashMap<>();
        request.put("userId", auth.getCurrentUser().getUid());
        request.put("type", Constants.TYPE_MEDICAL);
        request.put("description", "Type: Medicine Order\nDetails: " + details + "\nDelivery: " + deliveryTime);
        request.put("status", Constants.STATUS_PENDING);
        request.put("priority", "Normal");
        request.put("timestamp", FieldValue.serverTimestamp());

        // Use fetched address as location string
        String address = tvDeliveryAddress.getText().toString();
        if (address.isEmpty() || address.equals("Loading address...")) {
            address = "Location Provided (GPS)";
        }
        request.put("location", address);

        request.put("latitude", latitude);
        request.put("longitude", longitude);

        if (imageUrl != null) {
            request.put("imageUrl", imageUrl);
        }

        submitRequestToFirestore(request);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
