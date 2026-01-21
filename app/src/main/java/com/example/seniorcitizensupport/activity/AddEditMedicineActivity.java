package com.example.seniorcitizensupport.activity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.seniorcitizensupport.R;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class AddEditMedicineActivity extends AppCompatActivity {

    private static final String TAG = "AddEditMedicineActivity";
    public static final String KEY_DOC_ID = "key_document_id";

    private TextInputEditText etName, etDescription, etPrice, etStock, etImage;
    private SwitchMaterial switchAvailable;
    private Button btnSave;
    private TextView tvTitle;

    private FirebaseFirestore db;
    private String currentDocId; // To store the ID of the medicine being edited
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_medicine);

        db = FirebaseFirestore.getInstance();
        initializeViews();

        // Check if an ID was passed to this activity
        if (getIntent().hasExtra(KEY_DOC_ID)) {
            currentDocId = getIntent().getStringExtra(KEY_DOC_ID);
            isEditMode = true;
            tvTitle.setText("Edit Medicine");
            loadMedicineData();
        } else {
            isEditMode = false;
            tvTitle.setText("Add Medicine");
        }

        btnSave.setOnClickListener(v -> saveMedicine());
    }

    private void initializeViews() {
        tvTitle = findViewById(R.id.text_view_title);
        etName = findViewById(R.id.edit_text_name);
        etDescription = findViewById(R.id.edit_text_description);
        etPrice = findViewById(R.id.edit_text_price);
        etStock = findViewById(R.id.edit_text_stock);
        etImage = findViewById(R.id.edit_text_image);
        switchAvailable = findViewById(R.id.switch_available);
        btnSave = findViewById(R.id.btn_save);
    }

    private void loadMedicineData() {
        db.collection("medicines").document(currentDocId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Use the MedicineModel to easily access fields
                        MedicineModel medicine = documentSnapshot.toObject(MedicineModel.class);
                        if (medicine != null) {
                            etName.setText(medicine.getName());
                            etDescription.setText(medicine.getDescription());
                            etPrice.setText(String.valueOf(medicine.getPrice()));
                            etStock.setText(String.valueOf(medicine.getStock()));
                            etImage.setText(documentSnapshot.getString("image")); // Get image from document
                            switchAvailable.setChecked(medicine.isAvailable());
                        }
                    } else {
                        Toast.makeText(this, "Medicine not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load data.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error loading medicine data", e);
                });
    }

    private void saveMedicine() {
        // --- Input Validation ---
        String name = etName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String stockStr = etStock.getText().toString().trim();
        String image = etImage.getText().toString().trim();

        if (name.isEmpty() || description.isEmpty() || priceStr.isEmpty() || stockStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        int stock;
        try {
            price = Double.parseDouble(priceStr);
            stock = Integer.parseInt(stockStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers for price and stock", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- Prepare data for Firestore ---
        Map<String, Object> medicineData = new HashMap<>();
        medicineData.put("name", name);
        medicineData.put("description", description);
        medicineData.put("price", price);
        medicineData.put("stock", stock);
        medicineData.put("image", image);
        medicineData.put("available", switchAvailable.isChecked());

        // --- Save to Firestore ---
        DocumentReference docRef;
        if (isEditMode) {
            // Update an existing document
            docRef = db.collection("medicines").document(currentDocId);
        } else {
            // Create a new document
            docRef = db.collection("medicines").document();
        }

        docRef.set(medicineData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Medicine saved successfully!", Toast.LENGTH_SHORT).show();
                    finish(); // Go back to the ManageMedicinesActivity
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error saving medicine", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error saving medicine", e);
                });
    }
}
