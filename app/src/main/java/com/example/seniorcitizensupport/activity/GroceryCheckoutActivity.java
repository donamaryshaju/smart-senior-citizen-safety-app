package com.example.seniorcitizensupport.activity;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText; // <-- IMPORT ADDED
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.seniorcitizensupport.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser; // <-- IMPORT ADDED
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroceryCheckoutActivity extends AppCompatActivity {

    private static final String TAG = "GroceryCheckout"; // Best practice for logging

    private RecyclerView recyclerCheckout;
    private TextView txtTotalAmount;
    private Button btnConfirmOrder;
    private List<GroceryItem> cartItems;
    private CheckoutAdapter adapter;

    // *** ADDED: UI elements for new fields ***
    private EditText inputLocation;
    private EditText inputContact;

    private FirebaseFirestore db;
    private FirebaseAuth auth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grocery_checkout);

        // Initialize Firebase instances once
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // 1. Initialize Views
        recyclerCheckout = findViewById(R.id.recycler_cart_items);
        txtTotalAmount = findViewById(R.id.text_checkout_total);
        btnConfirmOrder = findViewById(R.id.btn_confirm_order);

        // *** ADDED: Initialize new EditTexts (These IDs must exist in your XML) ***
        inputLocation = findViewById(R.id.input_location);
        inputContact = findViewById(R.id.input_contact);

        // 2. Receive Data
        cartItems = new ArrayList<>();
        if (getIntent().hasExtra("cartList")) {
            cartItems = (ArrayList<GroceryItem>) getIntent().getSerializableExtra("cartList");
        }

        if (cartItems == null || cartItems.isEmpty()) {
            Toast.makeText(this, "Your cart is empty.", Toast.LENGTH_LONG).show();
            finish(); // It's better to close the activity if the cart is empty
            return;
        }

        // 3. Setup RecyclerView
        recyclerCheckout.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CheckoutAdapter(cartItems);
        recyclerCheckout.setAdapter(adapter);

        // 4. Calculate Total
        calculateTotal();

        // 5. Pre-fill user data from their profile
        loadUserDetails();

        // 6. Confirm Order Button
        btnConfirmOrder.setOnClickListener(v -> {
            if (cartItems != null && !cartItems.isEmpty()) {
                placeOrder();
            } else {
                Toast.makeText(this, "Your cart is empty!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Fetches the current user's details from Firestore and pre-fills the input fields.
     */
    private void loadUserDetails() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // NOTE: Field names like "address" and "phone" must match your Firestore 'users' collection
                            String locationFromProfile = documentSnapshot.getString("address");
                            String contactFromProfile = documentSnapshot.getString("phone");

                            if (locationFromProfile != null && !locationFromProfile.isEmpty()) {
                                inputLocation.setText(locationFromProfile);
                            }
                            if (contactFromProfile != null && !contactFromProfile.isEmpty()) {
                                inputContact.setText(contactFromProfile);
                            }
                        } else {
                            Log.w(TAG, "User profile document does not exist for UID: " + currentUser.getUid());
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to load user details", e));
        }
    }


    private void calculateTotal() {
        double total = 0;
        if (cartItems != null) {
            for (GroceryItem item : cartItems) {
                total += item.getPrice();
            }
        }
        txtTotalAmount.setText("Total: ₹" + String.format("%.2f", total));
    }

    private void placeOrder() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login to place an order", Toast.LENGTH_SHORT).show();
            return;
        }

        // *** ADDED: Get text from new fields and validate ***
        String location = inputLocation.getText().toString().trim();
        String contactNumber = inputContact.getText().toString().trim();

        if (location.isEmpty()) {
            inputLocation.setError("Delivery location is required");
            inputLocation.requestFocus();
            return;
        }

        if (contactNumber.isEmpty() || contactNumber.length() < 10) {
            inputContact.setError("A valid 10-digit contact number is required");
            inputContact.requestFocus();
            return;
        }
        // *** END OF VALIDATION ***

        String userId = auth.getCurrentUser().getUid();

        // --- Logic to create the description string ---
        StringBuilder descriptionBuilder = new StringBuilder();
        descriptionBuilder.append("Grocery Order: - ");
        DecimalFormat df = new DecimalFormat("0.00");

        List<Map<String, Object>> itemsAsMaps = new ArrayList<>();
        double totalAmountValue = 0;

        for (int i = 0; i < cartItems.size(); i++) {
            GroceryItem item = cartItems.get(i);
            descriptionBuilder.append(item.getName()).append(" (₹").append(df.format(item.getPrice())).append(")");
            if (i < cartItems.size() - 1) {
                descriptionBuilder.append(", ");
            }
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("name", item.getName());
            itemMap.put("price", item.getPrice());
            itemMap.put("unit", item.getUnit());
            itemsAsMaps.add(itemMap);
            totalAmountValue += item.getPrice();
        }

        descriptionBuilder.append(" Total Value: ₹").append(df.format(totalAmountValue));
        String finalDescription = descriptionBuilder.toString();

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("userId", userId);
        orderData.put("items", itemsAsMaps);
        orderData.put("totalAmount", totalAmountValue);
        orderData.put("status", "Pending");
        orderData.put("timestamp", Timestamp.now());
        orderData.put("type", "Grocery");
        orderData.put("description", finalDescription);

        // *** ADDED: Save the new fields to Firestore ***
        orderData.put("location", location);
        orderData.put("contactNumber", contactNumber);
        orderData.put("priority", "Normal"); // Set a default priority

        btnConfirmOrder.setEnabled(false);
        btnConfirmOrder.setText("Processing...");

        db.collection("orders")
                .add(orderData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Order placed successfully: " + documentReference.getId());
                    showSuccessDialog();
                })
                .addOnFailureListener(e -> {
                    btnConfirmOrder.setEnabled(true);
                    btnConfirmOrder.setText("CONFIRM ORDER");
                    Log.e(TAG, "Failed to place order", e);
                    Toast.makeText(GroceryCheckoutActivity.this, "Failed to place order: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // --- Show Success Popup (No changes needed) ---
    private void showSuccessDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_grocery_success);
        dialog.setCancelable(false);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        Button btnOk = dialog.findViewById(R.id.btn_success_ok);
        if (btnOk != null) {
            btnOk.setOnClickListener(v -> {
                dialog.dismiss();
                finish();
            });
        }

        dialog.show();
    }

    // --- Helper to get images safely (No changes needed) ---
    private int getGroceryImageResource(String itemName) {
        if (itemName == null) return R.drawable.ic_launcher_foreground;
        try {
            String formattedName = itemName.toLowerCase().replaceAll("\\s+", "").replaceAll("[^a-z0-9]", "");
            int resId = getResources().getIdentifier(formattedName, "drawable", getPackageName());
            return (resId != 0) ? resId : R.drawable.ic_launcher_foreground;
        } catch (Exception e) {
            return R.drawable.ic_launcher_foreground;
        }
    }

    // --- Adapter (No changes needed) ---
    private class CheckoutAdapter extends RecyclerView.Adapter<CheckoutAdapter.ViewHolder> {
        List<GroceryItem> list;

        public CheckoutAdapter(List<GroceryItem> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_grocery, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            GroceryItem item = list.get(position);

            if (holder.txtName != null) holder.txtName.setText(item.getName());
            if (holder.txtStock != null) {
                holder.txtStock.setText("₹" + String.format("%.2f", item.getPrice()));
            }
            if (holder.imgIcon != null) {
                holder.imgIcon.setImageResource(getGroceryImageResource(item.getName()));
            }
            if (holder.btnAdd != null) holder.btnAdd.setVisibility(View.GONE);
            if (holder.txtDesc != null) {
                if (item.getUnit() != null && !item.getUnit().isEmpty()) {
                    holder.txtDesc.setText(item.getUnit());
                } else {
                    holder.txtDesc.setVisibility(View.GONE);
                }
            }
        }

        @Override
        public int getItemCount() {
            return (list != null) ? list.size() : 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtName, txtDesc, txtStock;
            Button btnAdd;
            ImageView imgIcon;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                txtName = itemView.findViewById(R.id.txt_grocery_name);
                txtDesc = itemView.findViewById(R.id.txt_grocery_desc);
                txtStock = itemView.findViewById(R.id.txt_grocery_stock);
                btnAdd = itemView.findViewById(R.id.btn_add_item);
                imgIcon = itemView.findViewById(R.id.img_grocery_item);
            }
        }
    }
}
