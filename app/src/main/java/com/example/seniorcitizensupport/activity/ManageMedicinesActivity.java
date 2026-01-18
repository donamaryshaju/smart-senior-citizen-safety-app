package com.example.seniorcitizensupport.activity;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.example.seniorcitizensupport.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class ManageMedicinesActivity extends AppCompatActivity {

    private RecyclerView medicinesRecyclerView;
    private FloatingActionButton fabAddMedicine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_medicines);

        // Initialize UI components
        medicinesRecyclerView = findViewById(R.id.medicines_recycler_view);
        fabAddMedicine = findViewById(R.id.fab_add_medicine);

        // Set a click listener for the "Add" button
        fabAddMedicine.setOnClickListener(v -> {
            // Placeholder for "Add Medicine" functionality
            Toast.makeText(ManageMedicinesActivity.this, "Add new medicine clicked!", Toast.LENGTH_SHORT).show();
        });

        // You will set up your RecyclerView and Adapter here to load data from Firestore
        // For now, this is a placeholder
        Toast.makeText(this, "Loaded medicine management page", Toast.LENGTH_SHORT).show();
    }
}
