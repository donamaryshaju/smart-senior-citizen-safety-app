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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroceryCheckoutActivity extends AppCompatActivity {

    private RecyclerView recyclerCheckout;
    private TextView txtTotalAmount;
    private Button btnConfirmOrder;
    private List<GroceryItem> cartItems = new ArrayList<>();
    private CheckoutAdapter adapter;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;

    // ✅ Address fetched from profile
    private String userLocation = "Address not provided";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grocery_checkout);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        recyclerCheckout = findViewById(R.id.recycler_cart_items);
        txtTotalAmount = findViewById(R.id.text_checkout_total);
        btnConfirmOrder = findViewById(R.id.btn_confirm_order);

        // ✅ Fetch address from profile
        fetchUserLocation();

        // ✅ Receive cart list
        if (getIntent().hasExtra("cartList")) {
            try {
                cartItems = (ArrayList<GroceryItem>) getIntent().getSerializableExtra("cartList");
            } catch (Exception e) {
                Toast.makeText(this, "Failed to load cart", Toast.LENGTH_SHORT).show();
            }
        }

        if (cartItems == null || cartItems.isEmpty()) {
            Toast.makeText(this, "Your cart is empty.", Toast.LENGTH_LONG).show();
        }

        recyclerCheckout.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CheckoutAdapter(cartItems);
        recyclerCheckout.setAdapter(adapter);

        calculateTotal();

        btnConfirmOrder.setOnClickListener(v -> {
            if (!cartItems.isEmpty()) {
                placeOrder();
            } else {
                Toast.makeText(this, "Cart is empty!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ---------------- FETCH ADDRESS ----------------
    private void fetchUserLocation() {
        if (currentUser == null) return;

        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String address = documentSnapshot.getString("address");
                        if (address != null && !address.trim().isEmpty()) {
                            userLocation = address;
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("GroceryCheckout", "Failed to fetch address", e));
    }

    // ---------------- TOTAL CALCULATION ----------------
    private void calculateTotal() {
        double total = 0;
        for (GroceryItem item : cartItems) {
            total += item.getPrice();
        }
        txtTotalAmount.setText("Total: ₹" + String.format("%.2f", total));
    }

    // ---------------- PLACE ORDER ----------------
    private void placeOrder() {

        if (currentUser == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            return;
        }

        btnConfirmOrder.setEnabled(false);
        btnConfirmOrder.setText("Processing...");

        String userId = currentUser.getUid();
        StringBuilder descriptionBuilder = new StringBuilder("Grocery Order:\n");
        DecimalFormat df = new DecimalFormat("0.00");

        List<Map<String, Object>> itemsList = new ArrayList<>();
        double totalAmount = 0;

        for (GroceryItem item : cartItems) {

            descriptionBuilder.append("- ")
                    .append(item.getName())
                    .append(" (₹")
                    .append(df.format(item.getPrice()))
                    .append(")\n");

            Map<String, Object> map = new HashMap<>();
            map.put("name", item.getName());
            map.put("price", item.getPrice());
            map.put("unit", item.getUnit());
            itemsList.add(map);

            totalAmount += item.getPrice();
        }

        descriptionBuilder.append("\nTotal Value: ₹")
                .append(df.format(totalAmount));

        // ✅ Final order object
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("userId", userId);
        orderData.put("type", "Grocery");
        orderData.put("description", descriptionBuilder.toString());
        orderData.put("items", itemsList);
        orderData.put("totalAmount", totalAmount);
        orderData.put("status", "Pending");
        orderData.put("priority", "Normal");
        orderData.put("location", userLocation);
        orderData.put("timestamp", Timestamp.now());

        db.collection("orders")
                .add(orderData)
                .addOnSuccessListener(doc -> showSuccessDialog())
                .addOnFailureListener(e -> {
                    btnConfirmOrder.setEnabled(true);
                    btnConfirmOrder.setText("CONFIRM ORDER");
                    Toast.makeText(this, "Order failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // ---------------- SUCCESS POPUP ----------------
    private void showSuccessDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_grocery_success);
        dialog.setCancelable(false);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        Button btnOk = dialog.findViewById(R.id.btn_success_ok);
        btnOk.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });
        dialog.show();
    }

    // ---------------- IMAGE HELPER ----------------
    private int getGroceryImageResource(String itemName) {
        try {
            String formattedName = itemName.toLowerCase()
                    .replaceAll("\\s+", "")
                    .replaceAll("[^a-z0-9]", "");
            int resId = getResources().getIdentifier(formattedName, "drawable", getPackageName());
            return resId != 0 ? resId : R.drawable.ic_launcher_foreground;
        } catch (Exception e) {
            return R.drawable.ic_launcher_foreground;
        }
    }

    // ---------------- ADAPTER ----------------
    private class CheckoutAdapter extends RecyclerView.Adapter<CheckoutAdapter.ViewHolder> {

        List<GroceryItem> list;

        public CheckoutAdapter(List<GroceryItem> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_grocery, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            GroceryItem item = list.get(position);

            holder.txtName.setText(item.getName());
            holder.txtStock.setText("₹" + String.format("%.2f", item.getPrice()));

            if (item.getUnit() != null && !item.getUnit().isEmpty()) {
                holder.txtDesc.setText(item.getUnit());
            } else {
                holder.txtDesc.setVisibility(View.GONE);
            }

            holder.imgIcon.setImageResource(getGroceryImageResource(item.getName()));
            holder.btnAdd.setVisibility(View.GONE);
        }

        @Override
        public int getItemCount() {
            return list == null ? 0 : list.size();
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
