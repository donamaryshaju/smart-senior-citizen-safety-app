package com.example.seniorcitizensupport.activity;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.example.seniorcitizensupport.activity.MedicineModel;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MedicalActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText searchBar;
    private CardView layoutCheckout;
    private TextView textCartCount;
    private LinearLayout btnCheckout;

    private List<MedicineModel> allMedicines;
    private List<MedicineModel> filteredList;
    private List<MedicineModel> cartList;
    private MedicineAdapter adapter;
    private FirebaseFirestore fStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medical);

        fStore = FirebaseFirestore.getInstance();

        recyclerView = findViewById(R.id.recycler_medicines);
        searchBar = findViewById(R.id.search_medicine);
        layoutCheckout = findViewById(R.id.layout_checkout);
        textCartCount = findViewById(R.id.text_cart_count);
        btnCheckout = findViewById(R.id.btn_checkout);

        cartList = new ArrayList<>();
        allMedicines = new ArrayList<>();
        filteredList = new ArrayList<>();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MedicineAdapter(filteredList);
        recyclerView.setAdapter(adapter);

        loadMedicinesFromFirestore();

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        btnCheckout.setOnClickListener(v -> goToCheckoutPage());
    }

    // This new method clears the cart when you return from the checkout screen.
    @Override
    protected void onResume() {
        super.onResume();
        if (cartList != null) {
            cartList.clear();
        }
        updateCheckoutUI();
    }

    private int getMedicineImageResource(String medicineName) {
        if (medicineName == null)
            return R.drawable.ic_medical;
        String formattedName = medicineName.toLowerCase().replaceAll("\\s+", "").replaceAll("[^a-z0-9]", "");
        int resId = getResources().getIdentifier(formattedName, "drawable", getPackageName());
        return (resId != 0) ? resId : R.drawable.ic_medical;
    }

    private void showMedicineDetails(MedicineModel model) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_medicine_details);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        ImageView imgMed = dialog.findViewById(R.id.dialog_med_image);
        TextView name = dialog.findViewById(R.id.dialog_med_name);
        TextView price = dialog.findViewById(R.id.dialog_med_price);
        TextView stripInfo = dialog.findViewById(R.id.dialog_strip_info);
        TextView calcInfo = dialog.findViewById(R.id.dialog_calc_info);
        Button btnClose = dialog.findViewById(R.id.btn_dialog_close);

        imgMed.setImageResource(getMedicineImageResource(model.getName()));

        String strength = model.getStrength();
        if (strength != null && !strength.isEmpty()) {
            name.setText(model.getName() + " (" + strength + ")");
        } else {
            name.setText(model.getName());
        }

        double displayPrice = model.getDisplayPrice();
        price.setText("Price: ₹" + String.format("%.2f", displayPrice));

        int tablets = model.getTabletsPerStrip();
        if (tablets > 1) {
            stripInfo.setText("1 Strip contains " + tablets + " tablets/items");
            double costPerTablet = displayPrice / tablets;
            calcInfo.setText("Cost per unit: ₹" + String.format("%.2f", costPerTablet));
            calcInfo.setVisibility(View.VISIBLE);
        } else {
            stripInfo.setText("Single Unit Item");
            calcInfo.setVisibility(View.GONE);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void loadMedicinesFromFirestore() {
        fStore.collection("medicines")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        allMedicines.clear();
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            String name = doc.getString("name");
                            String desc = doc.getString("description");
                            String docId = doc.getId();

                            int stock = 0;
                            if (doc.contains("stock") && doc.get("stock") != null) {
                                Long s = doc.getLong("stock");
                                if (s != null)
                                    stock = s.intValue();
                            }

                            boolean available = true;
                            if (doc.contains("available") && doc.get("available") != null) {
                                available = doc.getBoolean("available");
                            }

                            List<Map<String, Object>> variants = null;
                            if (doc.contains("variants") && doc.get("variants") != null) {
                                try {
                                    variants = (List<Map<String, Object>>) doc.get("variants");
                                } catch (Exception e) {
                                    variants = null;
                                }
                            }

                            if (name != null) {
                                if (variants != null && !variants.isEmpty()) {
                                    for (Map<String, Object> variant : variants) {
                                        double vPrice = 0.0;
                                        if (variant.get("price") instanceof Number) {
                                            vPrice = ((Number) variant.get("price")).doubleValue();
                                        }
                                        List<Map<String, Object>> singleVariantList = new ArrayList<>();
                                        singleVariantList.add(variant);
                                        allMedicines.add(new MedicineModel(name, vPrice, desc, stock, available,
                                                singleVariantList, docId));
                                    }
                                } else {
                                    double price = 0.0;
                                    if (doc.contains("price") && doc.get("price") != null) {
                                        price = doc.getDouble("price");
                                    }
                                    allMedicines
                                            .add(new MedicineModel(name, price, desc, stock, available, null, docId));
                                }
                            }
                        }
                        filter("");
                    }
                })
                .addOnFailureListener(e -> Toast
                        .makeText(MedicalActivity.this, "Error loading medicines", Toast.LENGTH_SHORT).show());
    }

    private void filter(String text) {
        filteredList.clear();
        if (text.isEmpty()) {
            filteredList.addAll(allMedicines);
        } else {
            String searchText = text.toLowerCase();
            for (MedicineModel d : allMedicines) {
                String fullName = d.getName();
                String strength = d.getStrength();
                if (strength != null && !strength.isEmpty()) {
                    fullName += " " + strength;
                }
                if (fullName.toLowerCase().contains(searchText)) {
                    filteredList.add(d);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void addToCart(MedicineModel medicine) {
        cartList.add(medicine);
        updateCheckoutUI();
    }

    private void updateCheckoutUI() {
        if (cartList.isEmpty()) {
            layoutCheckout.setVisibility(View.GONE);
        } else {
            layoutCheckout.setVisibility(View.VISIBLE);
            double totalCost = 0;
            for (MedicineModel m : cartList) {
                totalCost += m.getDisplayPrice();
            }
            textCartCount.setText(cartList.size() + " Items | Total: ₹" + String.format("%.2f", totalCost));
        }
    }

    private void goToCheckoutPage() {
        if (cartList.isEmpty()) {
            Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(MedicalActivity.this, CheckoutActivity.class);
        intent.putExtra("cartList", (Serializable) cartList);
        startActivity(intent);
    }

    // --- ADAPTER ---
    class MedicineAdapter extends RecyclerView.Adapter<MedicineAdapter.ViewHolder> {
        List<MedicineModel> list;

        public MedicineAdapter(List<MedicineModel> list) {
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

            holder.imgMedicine.setImageResource(getMedicineImageResource(model.getName()));

            String strength = model.getStrength();
            if (strength != null && !strength.isEmpty()) {
                holder.name.setText(model.getName() + " (" + strength + ")");
            } else {
                holder.name.setText(model.getName());
            }

            double displayPrice = model.getDisplayPrice();
            holder.price.setText("₹" + String.format("%.2f", displayPrice));

            // *** THE BUG IS FIXED HERE. THIS LOGIC NOW RUNS FOR ALL ITEMS. ***
            if (model.isAvailable() && model.getStock() > 0) {
                holder.desc.setText(model.getDescription());
                holder.desc.setTextColor(Color.parseColor("#78909C"));

                holder.btnAdd.setEnabled(true);
                holder.btnAdd.setText("ADD");
                holder.btnAdd.setTextColor(Color.parseColor("#4FC3F7"));
                holder.btnAdd.setOnClickListener(v -> {
                    addToCart(model);
                    Toast.makeText(MedicalActivity.this, model.getName() + " added", Toast.LENGTH_SHORT).show();
                });
            } else {
                holder.desc.setText("Currently Unavailable");
                holder.desc.setTextColor(Color.parseColor("#E57373"));
                holder.btnAdd.setEnabled(false);
                holder.btnAdd.setText("SOLD OUT");
                holder.btnAdd.setTextColor(Color.LTGRAY);
                holder.btnAdd.setOnClickListener(null);
            }

            holder.itemView.setOnClickListener(v -> showMedicineDetails(model));
        }

        @Override
        public int getItemCount() {
            return (list != null) ? list.size() : 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView name, desc, price;
            Button btnAdd;
            ImageView imgMedicine;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.text_med_name);
                desc = itemView.findViewById(R.id.text_med_desc);
                price = itemView.findViewById(R.id.text_med_price);
                btnAdd = itemView.findViewById(R.id.btn_add_to_cart);
                imgMedicine = itemView.findViewById(R.id.img_medicine);
            }
        }
    }
}
