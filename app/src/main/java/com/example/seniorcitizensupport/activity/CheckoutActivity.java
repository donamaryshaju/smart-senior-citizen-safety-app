package com.example.seniorcitizensupport.activity;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.seniorcitizensupport.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckoutActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView textTotal;
    private Button btnConfirm;
    private List<MedicineModel> cartList;
    private FirebaseFirestore fStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        fStore = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.recycler_cart_items);
        textTotal = findViewById(R.id.text_checkout_total);
        btnConfirm = findViewById(R.id.btn_confirm_order);

        // Get the list passed from MedicalActivity
        try {
            cartList = (List<MedicineModel>) getIntent().getSerializableExtra("cartList");
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (cartList == null || cartList.isEmpty()) {
            Toast.makeText(this, "No items in cart", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupRecyclerView();
        calculateTotal();

        // CHANGED: Call the fetch method instead of placing order immediately
        btnConfirm.setOnClickListener(v -> fetchProfileAndPlaceOrder());
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        CartAdapter adapter = new CartAdapter(cartList);
        recyclerView.setAdapter(adapter);
    }

    private void calculateTotal() {
        double total = 0;
        if (cartList != null) {
            for (MedicineModel m : cartList) {
                total += m.getDisplayPrice();
            }
        }
        textTotal.setText("Total: ₹" + String.format("%.2f", total));
    }

    // --- STEP 1: Fetch Profile Data & Current Location ---
    private void fetchProfileAndPlaceOrder() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check Permissions first
        if (androidx.core.content.ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(this,
                    new String[] { android.Manifest.permission.ACCESS_FINE_LOCATION }, 101);
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        btnConfirm.setEnabled(false);
        btnConfirm.setText("Fetching Details...");

        // 1. Fetch Profile
        fStore.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String address = "Address not provided";
                    String phone = "No phone provided";

                    if (documentSnapshot.exists()) {
                        if (documentSnapshot.contains("address") && documentSnapshot.getString("address") != null)
                            address = documentSnapshot.getString("address");
                        if (documentSnapshot.contains("phone") && documentSnapshot.getString("phone") != null)
                            phone = documentSnapshot.getString("phone");
                    }

                    if (address.equals("Address not provided") || address.trim().isEmpty()) {
                        Toast.makeText(CheckoutActivity.this, "Please update address in Profile!", Toast.LENGTH_LONG)
                                .show();
                        btnConfirm.setEnabled(true);
                        btnConfirm.setText("CONFIRM ORDER");
                        return;
                    }

                    // 2. Fetch Location
                    fetchLocationAndProceed(userId, address, phone);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CheckoutActivity.this, "Error fetching profile", Toast.LENGTH_SHORT).show();
                    btnConfirm.setEnabled(true);
                    btnConfirm.setText("CONFIRM ORDER");
                });
    }

    private void fetchLocationAndProceed(String userId, String address, String phone) {
        com.google.android.gms.location.FusedLocationProviderClient fusedLocationClient = com.google.android.gms.location.LocationServices
                .getFusedLocationProviderClient(this);

        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        double lat = 0.0;
                        double lng = 0.0;
                        if (location != null) {
                            lat = location.getLatitude();
                            lng = location.getLongitude();
                        }
                        placeOrderFinal(userId, address, phone, lat, lng);
                    })
                    .addOnFailureListener(e -> {
                        // If location fails, proceed with 0.0 (or handle error)
                        placeOrderFinal(userId, address, phone, 0.0, 0.0);
                    });
        } catch (SecurityException e) {
            placeOrderFinal(userId, address, phone, 0.0, 0.0);
        }
    }

    // --- STEP 2: Place Order with Real Address & Location ---
    private void placeOrderFinal(String userId, String userAddress, String userPhone, double lat, double lng) {
        btnConfirm.setText("Processing...");

        // 1. Prepare Description
        double finalTotal = 0;
        StringBuilder orderSummary = new StringBuilder("Medicine Order:\n");
        for (MedicineModel m : cartList) {
            String displayName = m.getName();
            String strength = m.getStrength();
            if (!strength.isEmpty()) {
                displayName += " (" + strength + ")";
            }
            double itemPrice = m.getDisplayPrice();
            orderSummary.append("- ").append(displayName).append(" (₹").append(itemPrice).append(")\n");
            finalTotal += itemPrice;
        }
        orderSummary.append("\nTotal Value: ₹").append(String.format("%.2f", finalTotal));

        Map<String, Object> request = new HashMap<>();
        request.put("userId", userId);
        request.put("type", "Medical Assistance");
        request.put("description", orderSummary.toString());
        request.put("status", "Pending");
        request.put("priority", "Normal");
        request.put("timestamp", FieldValue.serverTimestamp());

        // USE REAL PROFILE DATA HERE
        request.put("location", userAddress);
        request.put("contactNumber", userPhone);
        request.put("latitude", lat);
        request.put("longitude", lng);

        // 2. Batch Write (Reduce Stock)
        WriteBatch batch = fStore.batch();
        DocumentReference newRequestRef = fStore.collection("requests").document();
        batch.set(newRequestRef, request);

        for (MedicineModel m : cartList) {
            if (m.getDocId() != null && !m.getDocId().isEmpty()) {
                DocumentReference medRef = fStore.collection("medicines").document(m.getDocId());
                batch.update(medRef, "stock", FieldValue.increment(-1));
            } else {
                System.out.println("Warning: Cannot update stock for " + m.getName() + " - ID is missing.");
            }
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> showSuccessDialog())
                .addOnFailureListener(e -> {
                    btnConfirm.setEnabled(true);
                    btnConfirm.setText("CONFIRM ORDER");
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                });
    }

    private void showSuccessDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_success);
        dialog.setCancelable(false);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        Button btnOk = dialog.findViewById(R.id.btn_success_ok);
        btnOk.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(CheckoutActivity.this, MedicalActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
        dialog.show();
    }

    // --- Simple Adapter for Checkout List ---
    class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {
        List<MedicineModel> list;

        public CartAdapter(List<MedicineModel> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_medicine, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MedicineModel model = list.get(position);
            String strength = model.getStrength();
            if (!strength.isEmpty())
                holder.name.setText(model.getName() + " (" + strength + ")");
            else
                holder.name.setText(model.getName());

            holder.price.setText("₹" + String.format("%.2f", model.getDisplayPrice()));
            holder.desc.setVisibility(View.GONE);
            holder.btnAdd.setVisibility(View.GONE);

            String formattedName = model.getName().toLowerCase().replaceAll("\\s+", "").replaceAll("[^a-z0-9]", "");
            int resId = holder.itemView.getResources().getIdentifier(formattedName, "drawable",
                    holder.itemView.getContext().getPackageName());
            if (resId != 0)
                holder.img.setImageResource(resId);
            else
                holder.img.setImageResource(R.drawable.paracetamol);
        }

        @Override
        public int getItemCount() {
            return list == null ? 0 : list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView name, desc, price;
            Button btnAdd;
            android.widget.ImageView img;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.text_med_name);
                desc = itemView.findViewById(R.id.text_med_desc);
                price = itemView.findViewById(R.id.text_med_price);
                btnAdd = itemView.findViewById(R.id.btn_add_to_cart);
                img = itemView.findViewById(R.id.img_medicine);
            }
        }
    }
}