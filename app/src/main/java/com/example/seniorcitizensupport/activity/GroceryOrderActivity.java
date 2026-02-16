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

import com.bumptech.glide.Glide;
import com.example.seniorcitizensupport.Constants;
import com.example.seniorcitizensupport.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class GroceryOrderActivity extends AppCompatActivity {

    // Views
    private LinearLayout layoutStep1, layoutStep2;
    private CardView cardUpload, cardText, cardRepeat;
    private Button btnContinue, btnSubmit;
    private ImageView btnBack;
    private TextView tvHeaderTitle;

    // Step 2 Views
    private TextView tvSelectedMethodText, btnChangeMethod, tvDeliveryAddress, tvRepeatInfo;
    private LinearLayout layoutInputCamera, layoutInputText, layoutInputRepeat;
    private ImageView imgPreview;
    private Button btnRetakePhoto;
    private EditText etMedicineList; // Used for Grocery list too
    private RadioGroup radioGroupTime;

    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int LOCATION_PERMISSION_CODE = 101;
    private Uri photoURI;
    private boolean isListUploaded = false;
    private String selectedMethod = "";
    private int currentStep = 1;
    private String grocerySubType = "Grocery"; // Default

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // Location
    private com.google.android.gms.location.FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grocery_order);

        if (getIntent().hasExtra("GROCERY_SUBTYPE")) {
            grocerySubType = getIntent().getStringExtra("GROCERY_SUBTYPE");
        }

        // Init Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Init Location Client
        fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this);

        // Initialize Views
        initViews();

        tvHeaderTitle.setText("Order " + grocerySubType);

        // Listeners for Step 1
        cardUpload.setOnClickListener(v -> selectMethod("Upload"));
        cardText.setOnClickListener(v -> selectMethod("Text"));
        cardRepeat.setOnClickListener(v -> fetchLastOrderAndPopulate());
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
        cardText = findViewById(R.id.cardText);
        cardRepeat = findViewById(R.id.cardRepeat);

        btnContinue = findViewById(R.id.btnContinue);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnBack = findViewById(R.id.btnBack);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);

        tvSelectedMethodText = findViewById(R.id.tvSelectedMethodText);
        btnChangeMethod = findViewById(R.id.btnChangeMethod);

        layoutInputCamera = findViewById(R.id.layoutInputCamera);
        layoutInputText = findViewById(R.id.layoutInputText);
        layoutInputRepeat = findViewById(R.id.layoutInputRepeat);

        imgPreview = findViewById(R.id.imgPreview);
        btnRetakePhoto = findViewById(R.id.btnRetakePhoto);
        etMedicineList = findViewById(R.id.etMedicineList);
        radioGroupTime = findViewById(R.id.radioGroupTime);
        tvDeliveryAddress = findViewById(R.id.tvDeliveryAddress);
        tvRepeatInfo = findViewById(R.id.tvRepeatInfo);

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

        if (selectedMethod.equals("Upload")) {
            layoutInputCamera.setVisibility(View.VISIBLE);
            tvSelectedMethodText.setText("Upload List Selected");
            if (!isListUploaded) {
                showImagePickerOptions();
            }
        } else if (selectedMethod.equals("Text")) {
            layoutInputText.setVisibility(View.VISIBLE);
            tvSelectedMethodText.setText("Type Grocery List");
        } else if (selectedMethod.equals("Repeat")) {
            layoutInputText.setVisibility(View.VISIBLE); // Show text area pre-filled
            tvSelectedMethodText.setText("Repeat Last Order");
        }

        checkSubmitValidity();
    }

    private void goToStep1() {
        currentStep = 1;
        layoutStep2.setVisibility(View.GONE);
        layoutStep1.setVisibility(View.VISIBLE);
        btnContinue.setVisibility(View.GONE);
        selectedMethod = "";
        isListUploaded = false;
        imgPreview.setImageURI(null);
        imgPreview.setImageResource(R.drawable.ic_camera_alt);
        etMedicineList.setText("");
        radioGroupTime.clearCheck();
        resetCardStyle(cardUpload);
        resetCardStyle(cardText);
        resetCardStyle(cardRepeat);
    }

    // --- STEP 2 LOGIC ---

    private void checkSubmitValidity() {
        boolean isTimeSelected = radioGroupTime.getCheckedRadioButtonId() != -1;
        boolean isMethodValid = true;

        if (selectedMethod.equals("Upload") && !isListUploaded) {
            isMethodValid = false;
        }

        if (isTimeSelected && isMethodValid) {
            btnSubmit.setEnabled(true);
            btnSubmit.setAlpha(1.0f);
        } else {
            btnSubmit.setEnabled(false);
            btnSubmit.setAlpha(0.5f);
        }
    }

    // --- REPEAT ORDER LOGIC ---
    private void fetchLastOrderAndPopulate() {
        if (auth.getCurrentUser() == null)
            return;

        Toast.makeText(this, "Fetching last order...", Toast.LENGTH_SHORT).show();

        db.collection(Constants.KEY_COLLECTION_REQUESTS)
                .whereEqualTo("userId", auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        java.util.List<com.google.firebase.firestore.DocumentSnapshot> docs = queryDocumentSnapshots
                                .getDocuments();
                        com.google.firebase.firestore.DocumentSnapshot lastOrder = null;
                        long latestTime = 0;

                        for (com.google.firebase.firestore.DocumentSnapshot doc : docs) {
                            // Filter logic for Grocery type
                            String type = doc.getString("type");
                            if (Constants.TYPE_GROCERY.equals(type)) {
                                java.util.Date date = null;
                                Object ts = doc.get("timestamp");
                                if (ts instanceof com.google.firebase.Timestamp) {
                                    date = ((com.google.firebase.Timestamp) ts).toDate();
                                }
                                if (date != null && date.getTime() > latestTime) {
                                    latestTime = date.getTime();
                                    lastOrder = doc;
                                }
                            }
                        }

                        if (lastOrder != null) {
                            String description = lastOrder.getString("description");
                            if (description != null) {
                                // Clean up
                                String cleanDesc = description.replace("Type: Grocery Order\n", "")
                                        .replace("Details: ", "")
                                        .replaceAll("Delivery: .*$", "")
                                        .trim();
                                etMedicineList.setText(cleanDesc);
                                selectMethod("Repeat"); // Uses text view
                                showToast("Last order loaded.");
                            }
                        } else {
                            showToast("No previous grocery orders found.");
                            selectMethod("Text");
                        }
                    } else {
                        showToast("No previous orders found.");
                        selectMethod("Text");
                    }
                });
    }

    // --- Camera & Gallery Logic ---

    private void showImagePickerOptions() {
        String[] options = { "Take Photo", "Choose from Gallery" };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Upload List");
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
            }
        } else if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationAndSubmit();
            } else {
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
                photoURI = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                cameraLauncher.launch(takePictureIntent);
            }
        }
    }

    private void openGallery() {
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
                    isListUploaded = true;
                    imgPreview.setImageURI(photoURI);
                    checkSubmitValidity();
                    showToast("List Captured!");
                }
            });

    ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImage = result.getData().getData();
                    photoURI = selectedImage;
                    isListUploaded = true;
                    imgPreview.setImageURI(selectedImage);
                    checkSubmitValidity();
                    showToast("List Selected!");
                }
            });

    // --- SUBMIT LOGIC ---

    private void submitRequestToFirestore(Map<String, Object> request) {
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Submitting...");

        db.collection(Constants.KEY_COLLECTION_REQUESTS).add(request)
                .addOnSuccessListener(documentReference -> {
                    showToast("Grocery Order Sent!");
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
        if (("Text".equals(selectedMethod) || "Repeat".equals(selectedMethod))
                && etMedicineList.getText().toString().trim().isEmpty()) {
            showToast("Please list your items");
            return;
        }

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
                        // Default to 0,0 if location unavailable
                        uploadImageAndSubmit(0.0, 0.0);
                    }
                })
                .addOnFailureListener(e -> uploadImageAndSubmit(0.0, 0.0));
    }

    private void uploadImageAndSubmit(double latitude, double longitude) {
        if ("Upload".equals(selectedMethod) && photoURI != null) {
            btnSubmit.setText("Processing Image...");
            new Thread(() -> {
                String base64Image = encodeImageToBase64(photoURI);
                runOnUiThread(() -> {
                    submitOrder(latitude, longitude, base64Image);
                });
            }).start();
        } else {
            submitOrder(latitude, longitude, null);
        }
    }

    private String encodeImageToBase64(Uri imageUri) {
        try {
            android.graphics.Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            int maxWidth = 800;
            int maxHeight = 800;
            float scale = Math.min(((float) maxWidth / bitmap.getWidth()), ((float) maxHeight / bitmap.getHeight()));
            android.graphics.Matrix matrix = new android.graphics.Matrix();
            matrix.postScale(scale, scale);
            android.graphics.Bitmap resizedBitmap = android.graphics.Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);
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
        if ("Text".equals(selectedMethod) || "Repeat".equals(selectedMethod)) {
            details = etMedicineList.getText().toString().trim();
        } else if ("Upload".equals(selectedMethod)) {
            details = "List Photo Uploaded";
        }

        Map<String, Object> request = new HashMap<>();
        request.put("userId", auth.getCurrentUser().getUid());
        request.put("type", Constants.TYPE_GROCERY);
        request.put("description", "Type: Grocery Order\nSubtype: " + grocerySubType + "\nDetails: " + details
                + "\nDelivery: " + deliveryTime);
        request.put("status", Constants.STATUS_PENDING);
        request.put("priority", "Normal");
        request.put("timestamp", FieldValue.serverTimestamp());

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
