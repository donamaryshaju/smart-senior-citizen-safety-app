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
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat; // --- IMPORT FOR FORMATTING ---
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroceryCheckoutActivity extends AppCompatActivity {

    private RecyclerView recyclerCheckout;
    private TextView txtTotalAmount;
    private Button btnConfirmOrder;
    private List<GroceryItem> cartItems;
    private CheckoutAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grocery_checkout);

        // 1. Initialize Views
        recyclerCheckout = findViewById(R.id.recycler_cart_items);
        txtTotalAmount = findViewById(R.id.text_checkout_total);
        btnConfirmOrder = findViewById(R.id.btn_confirm_order);

        // 2. Receive Data
        cartItems = new ArrayList<>();
        if (getIntent().hasExtra("cartList")) {
            try {
                cartItems = (ArrayList<GroceryItem>) getIntent().getSerializableExtra("cartList");
            } catch (Exception e) {
                Log.e("CheckoutError", "Error receiving list: " + e.getMessage());
                Toast.makeText(this, "Could not load cart items.", Toast.LENGTH_SHORT).show();
            }
        }

        if (cartItems == null || cartItems.isEmpty()) {
            Toast.makeText(this, "Your cart is empty.", Toast.LENGTH_LONG).show();
        }

        // 3. Setup RecyclerView
        recyclerCheckout.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CheckoutAdapter(cartItems);
        recyclerCheckout.setAdapter(adapter);

        // 4. Calculate Total
        calculateTotal();

        // 5. Confirm Order Button
        btnConfirmOrder.setOnClickListener(v -> {
            if (cartItems != null && !cartItems.isEmpty()) {
                placeOrder();
            } else {
                Toast.makeText(this, "Your cart is empty!", Toast.LENGTH_SHORT).show();
            }
        });
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
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login to place an order", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();

        // --- *** THIS IS THE NEW LOGIC TO CREATE THE DESCRIPTION *** ---
        StringBuilder descriptionBuilder = new StringBuilder();
        descriptionBuilder.append("Grocery Order: - ");
        DecimalFormat df = new DecimalFormat("0.00");

        List<Map<String, Object>> itemsAsMaps = new ArrayList<>();
        double totalAmountValue = 0;

        for (int i = 0; i < cartItems.size(); i++) {
            GroceryItem item = cartItems.get(i);

            // 1. Build the human-readable description string
            descriptionBuilder.append(item.getName())
                    .append(" (₹")
                    .append(df.format(item.getPrice()))
                    .append(")");

            if (i < cartItems.size() - 1) {
                descriptionBuilder.append(", "); // Add comma between items
            }

            // 2. Build the list of maps for the 'items' field in Firestore
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("name", item.getName());
            itemMap.put("price", item.getPrice());
            itemMap.put("unit", item.getUnit());
            itemsAsMaps.add(itemMap);
            totalAmountValue += item.getPrice();
        }

        descriptionBuilder.append(" Total Value: ₹").append(df.format(totalAmountValue));
        String finalDescription = descriptionBuilder.toString();
        // --- *** END OF NEW LOGIC *** ---


        Map<String, Object> orderData = new HashMap<>();
        orderData.put("userId", userId);
        orderData.put("items", itemsAsMaps);
        orderData.put("totalAmount", totalAmountValue);
        orderData.put("status", "Pending");
        orderData.put("timestamp", Timestamp.now());
        orderData.put("type", "Grocery");
        // ** ADDING THE NEW DESCRIPTION FIELD TO THE ORDER DATA **
        orderData.put("description", finalDescription);

        btnConfirmOrder.setEnabled(false);
        btnConfirmOrder.setText("Processing...");

        db.collection("orders")
                .add(orderData)
                .addOnSuccessListener(documentReference -> {
                    Log.d("CheckoutSuccess", "Order placed successfully: " + documentReference.getId());
                    showSuccessDialog();
                })
                .addOnFailureListener(e -> {
                    btnConfirmOrder.setEnabled(true);
                    btnConfirmOrder.setText("CONFIRM ORDER");
                    Log.e("CheckoutFailure", "Failed to place order", e);
                    Toast.makeText(GroceryCheckoutActivity.this, "Failed to place order: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // --- Show Success Popup (No changes needed here) ---
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

    // --- Helper to get images safely (No changes needed here) ---
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

    // --- Adapter (No changes needed here) ---
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
