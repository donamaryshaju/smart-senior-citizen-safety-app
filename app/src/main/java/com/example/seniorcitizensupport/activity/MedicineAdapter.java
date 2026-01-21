package com.example.seniorcitizensupport.activity;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.seniorcitizensupport.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MedicineAdapter extends RecyclerView.Adapter<MedicineAdapter.MedicineViewHolder> {

    // This upper section is correct.
    private List<MedicineModel> medicineList;
    private List<String> documentIds;
    private List<String> imageNames;
    private final Context context;

    public MedicineAdapter(List<MedicineModel> medicineList, Context context) {
        this.medicineList = new ArrayList<>(medicineList);
        this.documentIds = new ArrayList<>();
        this.imageNames = new ArrayList<>();
        this.context = context;
    }

    public void updateData(List<MedicineModel> newMedicineList, List<String> newDocumentIds, List<String> newImageNames) {
        this.medicineList.clear();
        this.medicineList.addAll(newMedicineList);
        this.documentIds.clear();
        this.documentIds.addAll(newDocumentIds);
        this.imageNames.clear();
        this.imageNames.addAll(newImageNames);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MedicineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_medicine_admin, parent, false);
        return new MedicineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MedicineViewHolder holder, int position) {
        // This onBindViewHolder logic is correct.
        if (!holder.isInitialized) {
            Log.e("MedicineAdapter", "ViewHolder at position " + position + " was not initialized correctly. Skipping bind.");
            return;
        }

        MedicineModel medicine = medicineList.get(position);
        String docId = documentIds.get(position);
        String imageName = imageNames.get(position);

        holder.medName.setText(medicine.getName());
        holder.medDescription.setText(medicine.getDescription());
        holder.medPrice.setText(String.format(Locale.getDefault(), "₹%.2f", medicine.getDisplayPrice()));

        if (imageName != null && !imageName.isEmpty()) {
            int imageResId = context.getResources().getIdentifier(imageName, "drawable", context.getPackageName());
            if (imageResId != 0) {
                holder.medImage.setImageResource(imageResId);
            } else {
                holder.medImage.setImageResource(android.R.drawable.ic_dialog_alert);
            }
        } else {
            holder.medImage.setImageResource(android.R.drawable.ic_dialog_alert);
        }

        holder.deleteButton.setOnClickListener(v -> {
            FirebaseFirestore.getInstance().collection("medicines").document(docId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, medicine.getName() + " deleted", Toast.LENGTH_SHORT).show();
                        int currentPosition = holder.getAdapterPosition();
                        if (currentPosition != RecyclerView.NO_POSITION) {
                            medicineList.remove(currentPosition);
                            documentIds.remove(currentPosition);
                            imageNames.remove(currentPosition);
                            notifyItemRemoved(currentPosition);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Failed to delete item", Toast.LENGTH_SHORT).show();
                        Log.w("MedicineAdapter", "Error deleting document", e);
                    });
        });

        // Inside onBindViewHolder method in MedicineAdapter.java

        holder.editButton.setOnClickListener(v -> {
            // --- THIS IS THE NEW CODE ---
            Context context = v.getContext();
            Intent intent = new Intent(context, AddEditMedicineActivity.class);
            // Pass the document ID of the medicine to be edited
            intent.putExtra(AddEditMedicineActivity.KEY_DOC_ID, docId);
            context.startActivity(intent);
        });

    }

    @Override
    public int getItemCount() {
        return medicineList.size();
    }

    // --- THIS IS THE CORRECTED SECTION ---
    public static class MedicineViewHolder extends RecyclerView.ViewHolder {
        ImageView medImage;
        TextView medName, medDescription, medPrice;
        Button editButton, deleteButton;
        boolean isInitialized = false;

        public MedicineViewHolder(@NonNull View itemView) {
            super(itemView);
            try {
                // Find all views using the IDs from your XML
                // FIX: Changed R.id.image_med to R.id.img_medicine
                medImage = itemView.findViewById(R.id.img_medicine);

                // The rest of the IDs are correct as per your XML
                medName = itemView.findViewById(R.id.text_med_name);
                medDescription = itemView.findViewById(R.id.text_med_desc);
                medPrice = itemView.findViewById(R.id.text_med_price);
                editButton = itemView.findViewById(R.id.btn_edit);
                deleteButton = itemView.findViewById(R.id.btn_delete);

                // Safety check
                if (medImage == null || medName == null || medDescription == null || medPrice == null || editButton == null || deleteButton == null) {
                    Log.e("MedicineViewHolder", "Initialization failed. A view ID was not found in item_medicine_admin.xml.");
                    isInitialized = false;
                } else {
                    isInitialized = true;
                }
            } catch (Exception e) {
                Log.e("MedicineViewHolder", "Crash during ViewHolder initialization!", e);
                isInitialized = false;
            }
        }
    }
}
