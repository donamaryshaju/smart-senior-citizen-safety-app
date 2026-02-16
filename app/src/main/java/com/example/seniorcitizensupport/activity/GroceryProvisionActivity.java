package com.example.seniorcitizensupport.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.seniorcitizensupport.BaseActivity;
import com.example.seniorcitizensupport.Constants;
import com.example.seniorcitizensupport.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GroceryProvisionActivity extends BaseActivity {

    // UI Components
    private TextView headerCooking;
    private LinearLayout layoutCookingItems;
    private boolean isCookingExpanded = false;

    private EditText inputAdditionalItems;
    private RadioGroup radioGroupDelivery;
    private Button btnSubmit;
    private CardView btnRepeatOrder, btnSavedList;

    private FusedLocationProviderClient fusedLocationClient;

    // Item Counters
    private Map<String, Integer> itemQuantities = new HashMap<>();
    private Map<String, View> itemViews = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grocery_provision);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initializeViews();
        setupListeners();
        setupIncludedItems();
    }

    private void initializeViews() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        headerCooking = findViewById(R.id.header_cooking);
        layoutCookingItems = findViewById(R.id.layout_cooking_items);

        inputAdditionalItems = findViewById(R.id.input_additional_items);
        radioGroupDelivery = findViewById(R.id.radio_group_delivery);
        btnSubmit = findViewById(R.id.btn_submit_request);
        btnRepeatOrder = findViewById(R.id.btn_repeat_order);
        btnSavedList = findViewById(R.id.btn_saved_list);

        // Setup Item Views Map
        itemViews.put("Rice", findViewById(R.id.layout_rice));
        itemViews.put("Wheat", findViewById(R.id.layout_wheat));
        itemViews.put("Dal", findViewById(R.id.layout_dal));
        itemViews.put("Oil", findViewById(R.id.layout_oil));
        itemViews.put("Salt", findViewById(R.id.layout_salt));

        // Note: You must ensure IDs in the layout match these keys if using dynamic
        // lookup,
        // or just pass the View directly.
        // Since we used <include>, the IDs are set on the include tag.
    }

    private void setupIncludedItems() {
        setupItem("Rice", findViewById(R.id.layout_rice), "Rice");
        setupItem("Wheat", findViewById(R.id.layout_wheat), "Wheat Flour");
        setupItem("Dal", findViewById(R.id.layout_dal), "Dal/Pulses");
        setupItem("Oil", findViewById(R.id.layout_oil), "Cooking Oil (L)");
        setupItem("Salt", findViewById(R.id.layout_salt), "Salt (pkt)");
    }

    private void setupItem(String key, View view, String displayName) {
        if (view == null)
            return;

        TextView name = view.findViewById(R.id.text_item_name);
        TextView quantity = view.findViewById(R.id.text_quantity);
        ImageButton btnPlus = view.findViewById(R.id.btn_increase);
        ImageButton btnMinus = view.findViewById(R.id.btn_decrease);

        name.setText(displayName);
        itemQuantities.put(key, 0);

        btnPlus.setOnClickListener(v -> {
            int current = itemQuantities.get(key);
            current++;
            itemQuantities.put(key, current);
            quantity.setText(String.valueOf(current));
        });

        btnMinus.setOnClickListener(v -> {
            int current = itemQuantities.get(key);
            if (current > 0) {
                current--;
                itemQuantities.put(key, current);
                quantity.setText(String.valueOf(current));
            }
        });
    }

    private void setupListeners() {
        // Expandable Category
        headerCooking.setOnClickListener(v -> {
            isCookingExpanded = !isCookingExpanded;
            layoutCookingItems.setVisibility(isCookingExpanded ? View.VISIBLE : View.GONE);
            headerCooking.setText(isCookingExpanded ? "Cooking Essentials ▲" : "Cooking Essentials ▼");
        });

        // Quick Actions
        btnRepeatOrder.setOnClickListener(v -> {
            // Mock: Fill some data
            updateItemQuantity("Rice", 5);
            updateItemQuantity("Oil", 2);
            Toast.makeText(this, "Loaded last month's order", Toast.LENGTH_SHORT).show();
        });

        btnSavedList.setOnClickListener(v -> {
            // Mock: Fill different data
            updateItemQuantity("Wheat", 10);
            updateItemQuantity("Salt", 1);
            Toast.makeText(this, "Standard List Loaded", Toast.LENGTH_SHORT).show();
        });

        // Submit
        btnSubmit.setOnClickListener(v -> fetchLocationAndSubmit());
    }

    private void updateItemQuantity(String key, int qty) {
        if (itemQuantities.containsKey(key)) {
            itemQuantities.put(key, qty);
            View view = itemViews.get(key);
            if (view != null) {
                TextView qtyText = view.findViewById(R.id.text_quantity);
                qtyText.setText(String.valueOf(qty));
            }
        }
    }

    private void fetchLocationAndSubmit() {
        // Build the list string
        StringBuilder orderBuilder = new StringBuilder();
        boolean hasItems = false;

        for (Map.Entry<String, Integer> entry : itemQuantities.entrySet()) {
            if (entry.getValue() > 0) {
                orderBuilder.append("• ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                hasItems = true;
            }
        }

        String additional = inputAdditionalItems.getText().toString().trim();
        if (!additional.isEmpty()) {
            orderBuilder.append("\n[Additional Items]\n").append(additional);
            hasItems = true;
        }

        if (!hasItems) {
            Toast.makeText(this, "Please select at least one item", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get Delivery Preference
        int selectedId = radioGroupDelivery.getCheckedRadioButtonId();
        String deliveryTime = "Today Evening"; // Default
        RadioButton selectedBtn = findViewById(selectedId);
        if (selectedBtn != null) {
            deliveryTime = selectedBtn.getText().toString();
        }

        String finalOrder = "Monthly Provision Request\n\n" + orderBuilder.toString() + "\n\nDelivery: " + deliveryTime;

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, 101);
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Submitting...");

        String finalOrderStr = finalOrder;
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            double lat = (location != null) ? location.getLatitude() : 0.0;
            double lng = (location != null) ? location.getLongitude() : 0.0;
            submitToFirestore(finalOrderStr, lat, lng);
        }).addOnFailureListener(e -> submitToFirestore(finalOrderStr, 0.0, 0.0));
    }

    private void submitToFirestore(String orderDetails, double lat, double lng) {
        Map<String, Object> request = new HashMap<>();
        request.put("userId", auth.getCurrentUser().getUid());
        request.put("type", Constants.TYPE_GROCERY);
        request.put("description", orderDetails);
        request.put("status", Constants.STATUS_PENDING);
        request.put("priority", "Normal");
        request.put("timestamp", FieldValue.serverTimestamp());
        request.put("location", "Shared via GPS");
        request.put("latitude", lat);
        request.put("longitude", lng);

        firestore.collection(Constants.KEY_COLLECTION_REQUESTS).add(request)
                .addOnSuccessListener(ref -> {
                    hideProgressDialog();
                    new AlertDialog.Builder(this)
                            .setTitle("Order Placed!")
                            .setMessage("Your provision list has been sent to our volunteers. We will confirm shortly.")
                            .setPositiveButton("OK", (dialog, which) -> finish())
                            .setCancelable(false)
                            .show();
                })
                .addOnFailureListener(e -> {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("SUBMIT PROVISION REQUEST");
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchLocationAndSubmit();
        }
    }
}
