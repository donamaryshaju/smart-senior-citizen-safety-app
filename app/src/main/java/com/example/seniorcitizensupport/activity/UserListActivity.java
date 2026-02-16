package com.example.seniorcitizensupport.activity;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.seniorcitizensupport.BaseActivity;
import com.example.seniorcitizensupport.Constants;
import com.example.seniorcitizensupport.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.widget.ImageView;

public class UserListActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private UserAdapter adapter;
    private List<Map<String, Object>> userList;
    private TextView titleView;
    private String roleFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_requests); // Reusing layout as it has a Recycler

        roleFilter = getIntent().getStringExtra("ROLE");

        titleView = findViewById(R.id.text_my_requests_title);
        if (titleView != null)
            titleView.setText(roleFilter != null ? roleFilter + " List" : "User List");

        recyclerView = findViewById(R.id.recycler_my_requests);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        userList = new ArrayList<>();
        // Fix: Pass context and role to adapter
        adapter = new UserAdapter(userList, this, roleFilter);
        recyclerView.setAdapter(adapter);

        loadUsers();
    }

    private void loadUsers() {
        if (roleFilter == null)
            return;

        showProgressDialog("Loading " + roleFilter + "s...");

        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> task1 = firestore
                .collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_ROLE, roleFilter).get();

        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> task2 = firestore
                .collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo("userType", roleFilter).get();

        com.google.android.gms.tasks.Tasks.whenAllSuccess(task1, task2).addOnSuccessListener(objects -> {
            hideProgressDialog();
            userList.clear();

            java.util.Set<String> addedIds = new java.util.HashSet<>();

            // Process query 1 (Role)
            com.google.firebase.firestore.QuerySnapshot qs1 = (com.google.firebase.firestore.QuerySnapshot) objects
                    .get(0);
            for (DocumentSnapshot doc : qs1) {
                if (addedIds.add(doc.getId())) {
                    Map<String, Object> data = doc.getData();
                    if (data != null) {
                        data.put("id", doc.getId());
                        userList.add(data);
                    }
                }
            }

            // Process query 2 (UserType)
            com.google.firebase.firestore.QuerySnapshot qs2 = (com.google.firebase.firestore.QuerySnapshot) objects
                    .get(1);
            for (DocumentSnapshot doc : qs2) {
                if (addedIds.add(doc.getId())) {
                    Map<String, Object> data = doc.getData();
                    if (data != null) {
                        data.put("id", doc.getId());
                        userList.add(data);
                    }
                }
            }

            adapter.notifyDataSetChanged();

            if (userList.isEmpty()) {
                showToast("No " + roleFilter + "s found.");
            }

        }).addOnFailureListener(e -> {
            hideProgressDialog();
            showToast("Error loading users: " + e.getMessage());
        });
    }

    // --- ADAPTER ---
    static class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {
        private final List<Map<String, Object>> list;
        private final Context context;
        private final String role;

        public UserAdapter(List<Map<String, Object>> list, Context context, String role) {
            this.list = list;
            this.context = context;
            this.role = role;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_request_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, Object> user = list.get(position);
            String uid = (String) user.get("id");

            String name = (String) user.get(Constants.KEY_NAME);
            if (name == null)
                name = (String) user.get("fullName");
            if (name == null)
                name = (String) user.get("fName");

            String email = (String) user.get(Constants.KEY_EMAIL);
            String phone = (String) user.get(Constants.KEY_PHONE);
            boolean isVerified = user.containsKey(Constants.KEY_IS_VERIFIED)
                    && Boolean.TRUE.equals(user.get(Constants.KEY_IS_VERIFIED));

            holder.txtType.setText(name != null ? name : "No Name");
            holder.txtDesc.setText(email != null ? email : "No Email");
            holder.txtLocation.setText(phone != null ? phone : "No Phone");

            holder.txtPriority.setVisibility(View.GONE);
            holder.txtName.setVisibility(View.GONE);

            // VOLUNTEER SPECIFIC ACTIONS
            if (Constants.ROLE_VOLUNTEER.equalsIgnoreCase(role)) {
                holder.btnAssign.setVisibility(View.VISIBLE);

                holder.btnAssign.setText(isVerified ? "DETAILS (VERIFIED)" : "VERIFY / DETAILS");

                if (isVerified) {
                    holder.btnAssign.setBackgroundTintList(
                            ContextCompat.getColorStateList(context, android.R.color.darker_gray));
                } else {
                    holder.btnAssign.setBackgroundTintList(
                            ContextCompat.getColorStateList(context, android.R.color.holo_blue_dark));
                }

                holder.btnAssign
                        .setOnClickListener(v -> ((UserListActivity) context).showVolunteerDetails(user, position));
                holder.itemView
                        .setOnClickListener(v -> ((UserListActivity) context).showVolunteerDetails(user, position));

            } else {
                holder.btnAssign.setVisibility(View.GONE);
                // Disable item click for non-volunteers to avoid confusion if no dialog exists
                // for them
                holder.itemView.setOnClickListener(null);
            }

            // DELETE BUTTON (For All)
            holder.btnAccept.setVisibility(View.VISIBLE);
            holder.btnAccept.setText("DELETE");
            holder.btnAccept
                    .setBackgroundTintList(ContextCompat.getColorStateList(context, android.R.color.holo_red_dark));
            holder.btnAccept.setOnClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(context)
                        .setTitle("Delete User")
                        .setMessage("Are you sure you want to remove this user?")
                        .setPositiveButton("DELETE",
                                (dialog, which) -> ((UserListActivity) context).deleteUser(uid, position))
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtType, txtDesc, txtPriority, txtName, txtLocation;
            Button btnAccept, btnAssign;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                txtType = itemView.findViewById(R.id.req_type);
                txtDesc = itemView.findViewById(R.id.req_desc);
                txtPriority = itemView.findViewById(R.id.req_priority);
                txtName = itemView.findViewById(R.id.req_senior_name);
                txtLocation = itemView.findViewById(R.id.req_location);
                btnAccept = itemView.findViewById(R.id.btn_accept);
                btnAssign = itemView.findViewById(R.id.btn_assign);
            }
        }
    }

    public void showVolunteerDetails(Map<String, Object> user, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_volunteer_details, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // Bind Views
        TextView txtName = view.findViewById(R.id.dialog_vol_name);
        TextView txtEmail = view.findViewById(R.id.dialog_vol_email);
        TextView txtPhone = view.findViewById(R.id.dialog_vol_phone);
        TextView txtAddress = view.findViewById(R.id.dialog_vol_address);
        TextView txtServices = view.findViewById(R.id.dialog_vol_services);
        ImageView imgIdProof = view.findViewById(R.id.dialog_vol_id_proof);
        Button btnVerify = view.findViewById(R.id.btn_dialog_verify);
        Button btnClose = view.findViewById(R.id.btn_dialog_close);
        Button btnDelete = view.findViewById(R.id.btn_dialog_delete);

        // Populate Data
        String name = (String) user.get(Constants.KEY_NAME);
        if (name == null)
            name = (String) user.get("fullName");

        txtName.setText("Name: " + (name != null ? name : "N/A"));
        txtEmail.setText("Email: " + user.get(Constants.KEY_EMAIL));
        txtPhone.setText("Phone: " + user.get(Constants.KEY_PHONE));

        String address = (String) user.get("address");
        txtAddress.setText(address != null ? address : "No Address Provided");

        Object servicesObj = user.get("services");
        if (servicesObj instanceof List) {
            List<String> services = (List<String>) servicesObj;
            txtServices.setText(android.text.TextUtils.join(", ", services));
        } else {
            txtServices.setText("No Services Listed");
        }

        // Decode Image
        String base64Image = (String) user.get("idProofImage");
        if (base64Image != null && !base64Image.isEmpty()) {
            try {
                // Remove data:image/jpeg;base64, prefix if present
                if (base64Image.contains(",")) {
                    base64Image = base64Image.split(",")[1];
                }
                byte[] decodedString = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                imgIdProof.setImageBitmap(decodedByte);
            } catch (Exception e) {
                e.printStackTrace();
                imgIdProof.setImageResource(android.R.drawable.ic_menu_report_image); // Error placeholder
            }
        } else {
            imgIdProof.setImageResource(android.R.drawable.ic_menu_gallery); // Default placeholder
        }

        // Button Logic
        boolean isVerified = user.containsKey(Constants.KEY_IS_VERIFIED)
                && Boolean.TRUE.equals(user.get(Constants.KEY_IS_VERIFIED));

        if (isVerified) {
            btnVerify.setText("VERIFIED");
            btnVerify.setEnabled(false);
            btnVerify.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.darker_gray));
        } else {
            btnVerify.setText("VERIFY");
            btnVerify.setEnabled(true);
            btnVerify.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_green_dark));
            btnVerify.setOnClickListener(v -> {
                verifyUser((String) user.get("id"), position);
                dialog.dismiss();
            });
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnDelete.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Delete User")
                    .setMessage("Are you sure you want to delete this user?")
                    .setPositiveButton("DELETE", (d, w) -> {
                        deleteUser((String) user.get("id"), position);
                        dialog.dismiss();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        dialog.show();
    }

    public void verifyUser(String uid, int position) {
        if (uid == null)
            return;
        showProgressDialog("Verifying " + uid + "...");
        firestore.collection(Constants.KEY_COLLECTION_USERS).document(uid)
                .update(Constants.KEY_IS_VERIFIED, true)
                .addOnSuccessListener(v -> {
                    hideProgressDialog();
                    showToast("User Verified");
                    // Update local list
                    userList.get(position).put(Constants.KEY_IS_VERIFIED, true);
                    adapter.notifyItemChanged(position);
                })
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    showToast("Verification failed: " + e.getMessage());
                    e.printStackTrace();
                });
    }

    public void deleteUser(String uid, int position) {
        if (uid == null)
            return;
        showProgressDialog("Deleting...");
        firestore.collection(Constants.KEY_COLLECTION_USERS).document(uid)
                .delete()
                .addOnSuccessListener(v -> {
                    hideProgressDialog();
                    showToast("User Deleted");
                    if (position >= 0 && position < userList.size()) {
                        userList.remove(position);
                        adapter.notifyItemRemoved(position);
                    }
                })
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    showToast("Deletion failed");
                });
    }
}
