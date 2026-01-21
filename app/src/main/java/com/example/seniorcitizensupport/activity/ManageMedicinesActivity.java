package com.example.seniorcitizensupport.activity;
import android.content.Intent;
import android.os.Bundle;


import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.seniorcitizensupport.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ManageMedicinesActivity extends AppCompatActivity {

    private static final String TAG = "ManageMedicinesActivity";
    private RecyclerView medicinesRecyclerView;
    private FloatingActionButton fabAddMedicine;
    private MedicineAdapter medicineAdapter;
    private List<MedicineModel> medicineList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_medicines);
        Log.d(TAG, "onCreate: Activity started.");

        // --- NEW: Crash-proof initialization ---
        try {
            // Initialize Firebase
            db = FirebaseFirestore.getInstance();
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

            // Initialize UI components from layout
            medicinesRecyclerView = findViewById(R.id.medicines_recycler_view);
            fabAddMedicine = findViewById(R.id.fab_add_medicine);

            // --- SAFETY CHECK ---
            if (medicinesRecyclerView == null) {
                // This is the most likely cause of the crash.
                Log.e(TAG, "FATAL ERROR: medicines_recycler_view not found in activity_manage_medicines.xml. Check the ID.");
                Toast.makeText(this, "Layout Error! Cannot find RecyclerView.", Toast.LENGTH_LONG).show();
                return; // Stop execution to prevent crash
            }

            // Setup the RecyclerView and its adapter
            setupRecyclerView();


            // Inside onCreate method in ManageMedicinesActivity.java

            fabAddMedicine.setOnClickListener(v -> {
                // --- THIS IS THE NEW CODE ---
                // Start the activity without passing any document ID
                Intent intent = new Intent(ManageMedicinesActivity.this, AddEditMedicineActivity.class);
                startActivity(intent);
            });


            // Check if user is logged in before trying to load data
            if (currentUser != null) {
                Log.d(TAG, "User is logged in. Fetching medicines...");
                loadMedicinesFromFirestore();
            } else {
                Log.w(TAG, "No user is logged in. Cannot fetch medicines.");
                Toast.makeText(this, "Please sign in to manage medicines.", Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            // This will catch any unexpected crash and report it.
            Log.e(TAG, "FATAL CRASH in onCreate!", e);
            Toast.makeText(this, "A critical error occurred.", Toast.LENGTH_LONG).show();
        }
    }

    private void setupRecyclerView() {
        medicineList = new ArrayList<>();
        medicineAdapter = new MedicineAdapter(medicineList, this);
        medicinesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        medicinesRecyclerView.setAdapter(medicineAdapter);
        Log.d(TAG, "setupRecyclerView: RecyclerView setup complete.");
    }

    private void loadMedicinesFromFirestore() {
        db.collection("medicines")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        medicineList.clear();
                        List<String> documentIds = new ArrayList<>();
                        List<String> imageNames = new ArrayList<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            MedicineModel medicine = document.toObject(MedicineModel.class);
                            medicineList.add(medicine);
                            documentIds.add(document.getId());
                            String imageName = document.getString("image");
                            imageNames.add(imageName != null ? imageName : "");
                        }

                        medicineAdapter.updateData(medicineList, documentIds, imageNames);
                        Log.d(TAG, "Successfully loaded " + medicineList.size() + " medicines.");

                        if (medicineList.isEmpty()) {
                            Toast.makeText(ManageMedicinesActivity.this, "No medicines found in the database.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                        Toast.makeText(ManageMedicinesActivity.this, "Error loading medicines.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
