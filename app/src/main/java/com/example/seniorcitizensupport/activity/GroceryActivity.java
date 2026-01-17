package com.example.seniorcitizensupport.activity;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.seniorcitizensupport.R;
import com.example.seniorcitizensupport.activity.GroceryItem;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GroceryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText searchBar;
    private CardView layoutCheckout;
    private TextView textCartCount;
    private LinearLayout btnCheckout;

    private List<GroceryItem> allGroceries;
    private List<GroceryItem> filteredList;
    private List<GroceryItem> cartList;
    private GroceryAdapter adapter;
    private FirebaseFirestore fStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grocery);

        fStore = FirebaseFirestore.getInstance();

        // Initialize Views
        recyclerView = findViewById(R.id.recycler_grocery);
        searchBar = findViewById(R.id.search_grocery);
        layoutCheckout = findViewById(R.id.layout_checkout);
        textCartCount = findViewById(R.id.text_cart_count);
        btnCheckout = findViewById(R.id.btn_checkout);

        cartList = new ArrayList<>();
        allGroceries = new ArrayList<>();
        filteredList = new ArrayList<>();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GroceryAdapter(filteredList);
        recyclerView.setAdapter(adapter);

        loadGroceriesFromFirestore();

        // Search functionality
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Checkout Button Click
        btnCheckout.setOnClickListener(v -> goToCheckoutPage());
    }

    // --- DYNAMIC IMAGE LOADER ---
    private int getGroceryImageResource(String itemName) {
        if (itemName == null) return R.drawable.ic_launcher_foreground;
        try {
            // Clean name (e.g., "Basmati Rice" -> "basmatirice")
            String formattedName = itemName.toLowerCase().replaceAll("\\s+", "").replaceAll("[^a-z0-9]", "");
            int resId = getResources().getIdentifier(formattedName, "drawable", getPackageName());
            return (resId != 0) ? resId : R.drawable.ic_launcher_foreground;
        } catch (Exception e) {
            return R.drawable.ic_launcher_foreground;
        }
    }

    // --- POPUP DETAILS ---
    private void showGroceryDetails(GroceryItem model) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        // It is better to create a new layout R.layout.dialog_grocery_details, but this works.
        dialog.setContentView(R.layout.dialog_medicine_details);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Initialize Dialog Views
        ImageView imgItem = dialog.findViewById(R.id.dialog_med_image);
        TextView name = dialog.findViewById(R.id.dialog_med_name);
        TextView price = dialog.findViewById(R.id.dialog_med_price);
        TextView descInfo = dialog.findViewById(R.id.dialog_strip_info);
        TextView unitInfo = dialog.findViewById(R.id.dialog_calc_info);
        Button btnClose = dialog.findViewById(R.id.btn_dialog_close);

        // Set Data
        if (imgItem != null) imgItem.setImageResource(getGroceryImageResource(model.getName()));

        String displayName = model.getName();
        if(model.getUnit() != null && !model.getUnit().isEmpty()) {
            displayName += " (" + model.getUnit() + ")";
        }
        if (name != null) name.setText(displayName);
        if (price != null) price.setText("Price: ₹" + String.format("%.2f", model.getPrice()));
        if (descInfo != null) descInfo.setText(model.getDescription());

        // Hide extra info not needed for groceries
        if (unitInfo != null) unitInfo.setVisibility(View.GONE);

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    // --- LOAD DATA (CORRECTED) ---
    private void loadGroceriesFromFirestore() {
        fStore.collection("groceries")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        allGroceries.clear();
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            try {
                                String name = doc.getString("name");
                                String desc = doc.getString("description");
                                String unit = doc.getString("unit");

                                // Safe Price Loading
                                double price = 0.0;
                                if (doc.contains("price") && doc.get("price") != null) {
                                    if (doc.get("price") instanceof Number) {
                                        price = doc.getDouble("price");
                                    } else if (doc.get("price") instanceof String) {
                                        price = Double.parseDouble(doc.getString("price"));
                                    }
                                }

                                // Safe Stock Loading
                                int stock = 0;
                                if (doc.contains("stock") && doc.get("stock") != null) {
                                    Long s = doc.getLong("stock");
                                    if (s != null) stock = s.intValue();
                                }

                                boolean available = doc.exists() && doc.getBoolean("available") != null ? doc.getBoolean("available") : false;

                                if (name != null) {
                                    allGroceries.add(new GroceryItem(name, price, desc, stock, available, unit));
                                }
                            } catch (Exception e) {
                                Log.e("GroceryError", "Error parsing item: " + e.getMessage());
                            }
                        }
                        // This line is now correctly placed AFTER the loop finishes.
                        filter(""); // Populate the list for the first time
                    } else {
                        Toast.makeText(GroceryActivity.this, "No groceries found in database", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(GroceryActivity.this, "Error loading data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void filter(String text) {
        filteredList.clear();
        if (text == null || text.isEmpty()) {
            filteredList.addAll(allGroceries);
        } else {
            String searchText = text.toLowerCase();
            for (GroceryItem item : allGroceries) {
                if (item.getName() != null && item.getName().toLowerCase().contains(searchText)) {
                    filteredList.add(item);
                }
            }
        }
        if(adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void addToCart(GroceryItem item) {
        cartList.add(item);
        updateCheckoutUI();
    }

    private void updateCheckoutUI() {
        if (cartList.isEmpty()) {
            layoutCheckout.setVisibility(View.GONE);
        } else {
            layoutCheckout.setVisibility(View.VISIBLE);
            double totalCost = 0;
            for(GroceryItem m : cartList) {
                totalCost += m.getPrice();
            }
            textCartCount.setText(cartList.size() + " Items | Total: ₹" + String.format("%.2f", totalCost));
        }
    }

    private void goToCheckoutPage() {
        if (cartList.isEmpty()) {
            Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(GroceryActivity.this, GroceryCheckoutActivity.class);
        intent.putExtra("cartList", (Serializable) cartList);
        startActivity(intent);
    }

    // --- ADAPTER ---
    class GroceryAdapter extends RecyclerView.Adapter<GroceryAdapter.ViewHolder> {
        List<GroceryItem> list;

        public GroceryAdapter(List<GroceryItem> list) {
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
            GroceryItem model = list.get(position);

            holder.imgIcon.setImageResource(getGroceryImageResource(model.getName()));
            holder.txtName.setText(model.getName());

            String priceText = "₹" + String.format("%.2f", model.getPrice());
            if(model.getUnit() != null && !model.getUnit().isEmpty()) {
                priceText += " / " + model.getUnit();
            }
            holder.txtStock.setText(priceText);

            if (model.isAvailable() && model.getStock() > 0) {
                holder.txtDesc.setText(model.getDescription());
                holder.txtDesc.setTextColor(Color.parseColor("#78909C"));

                holder.btnAdd.setEnabled(true);
                holder.btnAdd.setText("ADD");
                holder.btnAdd.setTextColor(Color.WHITE);
                holder.btnAdd.setBackgroundColor(Color.parseColor("#039BE5")); // Default Blue

                holder.btnAdd.setOnClickListener(v -> {
                    addToCart(model);
                    Toast.makeText(GroceryActivity.this, model.getName() + " added", Toast.LENGTH_SHORT).show();
                });
            } else {
                holder.txtDesc.setText("Out of Stock");
                holder.txtDesc.setTextColor(Color.parseColor("#E57373"));
                holder.btnAdd.setEnabled(false);
                holder.btnAdd.setText("SOLD OUT");
                holder.btnAdd.setBackgroundColor(Color.GRAY);
                holder.btnAdd.setOnClickListener(null);
            }

            holder.itemView.setOnClickListener(v -> showGroceryDetails(model));
        }

        @Override
        public int getItemCount() { return list != null ? list.size() : 0; }

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
