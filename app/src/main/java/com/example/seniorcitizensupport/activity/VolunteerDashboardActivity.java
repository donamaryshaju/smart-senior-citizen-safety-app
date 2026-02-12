package com.example.seniorcitizensupport.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.seniorcitizensupport.BaseActivity;
import com.example.seniorcitizensupport.Constants;
import com.example.seniorcitizensupport.R;
import com.example.seniorcitizensupport.model.RequestModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class VolunteerDashboardActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private RequestAdapter adapter;
    private List<RequestModel> requestList;

    private CardView btnMedical, btnGrocery, btnTransport, btnHomecare;
    private TextView txtVolunteerName;
    private TextView txtBottomSheetTitle;
    private Button btnLogout;
    private Button btnMyTasks; // Added

    private ListenerRegistration firestoreListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_volunteer_dashboard);

        // -------- UI References --------
        btnMedical = findViewById(R.id.card_medical);
        btnGrocery = findViewById(R.id.card_grocery);
        btnTransport = findViewById(R.id.card_transport);
        btnHomecare = findViewById(R.id.card_homecare);
        btnHomecare = findViewById(R.id.card_homecare);
        txtVolunteerName = findViewById(R.id.text_volunteer_name);
        // Bind new Profile fields (local vars or class fields? better class fields if
        // used elsewhere, but local is fine for one-time load)
        TextView txtVolunteerPhone = findViewById(R.id.text_volunteer_phone);
        TextView txtVolunteerAddress = findViewById(R.id.text_volunteer_address);

        txtBottomSheetTitle = findViewById(R.id.bottom_sheet_title);
        recyclerView = findViewById(R.id.recycler_requests);
        btnLogout = findViewById(R.id.btn_logout);
        Button btnProfile = findViewById(R.id.btn_profile);
        btnMyTasks = findViewById(R.id.btn_my_tasks); // Added

        // Add Profile Click Listener
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        }

        // -------- Recycler Setup --------
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        requestList = new ArrayList<>();
        adapter = new RequestAdapter(requestList, this, firestore, auth);
        recyclerView.setAdapter(adapter);

        // -------- Button Actions --------
        btnMedical.setOnClickListener(v -> loadRequests(Constants.TYPE_MEDICAL));
        btnGrocery.setOnClickListener(v -> loadRequests(Constants.TYPE_GROCERY));
        btnTransport.setOnClickListener(v -> loadRequests(Constants.TYPE_TRANSPORT));
        btnHomecare.setOnClickListener(v -> loadRequests(Constants.TYPE_HOMECARE));

        // New Button Action
        if (btnMyTasks != null) {
            btnMyTasks.setOnClickListener(v -> loadMyTasks());
        }

        btnLogout.setOnClickListener(v -> {
            if (firestoreListener != null)
                firestoreListener.remove();
            if (activeMissionListener != null)
                activeMissionListener.remove();
            auth.signOut();
            Intent intent = new Intent(VolunteerDashboardActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // -------- Load Initial Data --------
        loadVolunteerInfo();
        checkLocationPermission(); // Request Permission

        // Dynamic Dashboard: Check for Active Mission first
        loadActiveMission();
    }

    // ---------------- ACTIVE MISSION LOGIC ----------------
    private ListenerRegistration activeMissionListener;
    private CardView cardActiveMission;
    private View containerCategories;
    private TextView activeStatus, activeSenior, activeLocation;
    private Button btnActiveAction;
    private RequestModel currentActiveReq;

    private void loadActiveMission() {
        cardActiveMission = findViewById(R.id.card_active_mission);
        containerCategories = findViewById(R.id.container_categories);
        activeStatus = findViewById(R.id.mission_status);
        activeSenior = findViewById(R.id.mission_senior_name);
        activeLocation = findViewById(R.id.mission_location);
        btnActiveAction = findViewById(R.id.btn_mission_action);

        if (auth.getCurrentUser() == null)
            return;

        // Query for any request where volunteerId is ME and status is NOT
        // Completed/Pending
        // Actually, status should be Accepted, On the way, Arrived, In Progress.
        // Simplified: status != Completed && status != Pending.
        // But Firestore != queries are limited.
        // Let's query for volunteerId = Me, and filter client side or order by status.

        Query query = firestore.collection(Constants.KEY_COLLECTION_REQUESTS)
                .whereEqualTo("volunteerId", auth.getCurrentUser().getUid())
                .whereNotEqualTo("status", Constants.STATUS_COMPLETED);
        // Note: whereNotEqualTo requires composite index often. If it fails, we catch
        // error.
        // Alternative: Query *all* my tasks and find the first one that is active.

        activeMissionListener = query.addSnapshotListener((value, error) -> {
            if (error != null)
                return;
            if (value != null && !value.isEmpty()) {
                // Find true active task
                RequestModel activeReq = null;
                for (DocumentSnapshot doc : value.getDocuments()) {
                    RequestModel r = doc.toObject(RequestModel.class);
                    if (r != null && !Constants.STATUS_PENDING.equals(r.getStatus())) {
                        r.setDocumentId(doc.getId());
                        activeReq = r;
                        break; // Take first active task
                    }
                }

                if (activeReq != null) {
                    showActiveMissionMode(activeReq);
                } else {
                    showStandardMode();
                }
            } else {
                showStandardMode();
            }
        });
    }

    private void showActiveMissionMode(RequestModel req) {
        currentActiveReq = req;
        cardActiveMission.setVisibility(View.VISIBLE);
        // containerCategories.setVisibility(View.GONE); // REMOVED: Keep categories
        // visible
        txtBottomSheetTitle.setText("Mission In Progress");

        // Bind Data
        activeStatus.setText(req.getStatus().toUpperCase());
        activeLocation.setText(req.getLocation());

        // Fetch Senior Name
        firestore.collection(Constants.KEY_COLLECTION_USERS).document(req.getUserId()).get()
                .addOnSuccessListener(ds -> {
                    String name = ds.getString(Constants.KEY_NAME);
                    if (name == null)
                        name = ds.getString("fName");
                    activeSenior.setText("Mission for: " + (name != null ? name : "Senior"));
                });

        // Bind Button
        configureActiveButton(req);
    }

    private void showStandardMode() {
        cardActiveMission.setVisibility(View.GONE);
        // containerCategories.setVisibility(View.VISIBLE);
        loadRequests(Constants.TYPE_MEDICAL); // Default load
    }

    private void configureActiveButton(RequestModel req) {
        String status = req.getStatus();
        btnActiveAction.setOnClickListener(null); // Reset

        if (Constants.STATUS_ACCEPTED.equals(status)) {
            btnActiveAction.setText("START TRIP");
            btnActiveAction
                    .setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_blue_dark));
            btnActiveAction.setOnClickListener(
                    v -> updateMissionStatus(req, Constants.STATUS_ON_THE_WAY, "Volunteer is on the way!"));
        } else if (Constants.STATUS_ON_THE_WAY.equals(status)) {
            btnActiveAction.setText("ARRIVED");
            btnActiveAction
                    .setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_orange_dark));
            btnActiveAction.setOnClickListener(
                    v -> updateMissionStatus(req, Constants.STATUS_ARRIVED, "Volunteer has arrived!"));
        } else if (Constants.STATUS_ARRIVED.equals(status)) {
            btnActiveAction.setText("START SERVICE");
            btnActiveAction.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_purple));
            btnActiveAction.setOnClickListener(
                    v -> updateMissionStatus(req, Constants.STATUS_IN_PROGRESS, "Service started."));
        } else if (Constants.STATUS_IN_PROGRESS.equals(status)) {
            btnActiveAction.setText("MARK COMPLETED");
            btnActiveAction
                    .setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_green_light));
            btnActiveAction.setOnClickListener(
                    v -> updateMissionStatus(req, Constants.STATUS_COMPLETED, "Service Completed."));
        }
    }

    private void updateMissionStatus(RequestModel req, String newStatus, String msg) {
        firestore.collection(Constants.KEY_COLLECTION_REQUESTS).document(req.getDocumentId())
                .update("status", newStatus)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Status Updated", Toast.LENGTH_SHORT).show();
                    com.example.seniorcitizensupport.utils.NotificationHelper.sendNotification(req.getUserId(),
                            "Update", msg);
                });
    }

    // ---------------- LOAD VOLUNTEER NAME & PROFILE ----------------
    private void loadVolunteerInfo() {
        if (auth.getCurrentUser() == null)
            return;
        firestore.collection(Constants.KEY_COLLECTION_USERS).document(auth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String name = documentSnapshot.getString(Constants.KEY_NAME);
                    if (name == null)
                        name = documentSnapshot.getString("fName");
                    if (name == null)
                        name = documentSnapshot.getString("fullName");
                    if (name == null || name.trim().isEmpty())
                        name = "Volunteer";
                    txtVolunteerName.setText("Hello, " + name);

                    // Load Phone & Address
                    String phone = documentSnapshot.getString(Constants.KEY_PHONE);
                    String address = documentSnapshot.getString("address"); // Using raw key as it might not be in
                                                                            // Constants

                    TextView txtPhone = findViewById(R.id.text_volunteer_phone);
                    TextView txtAddr = findViewById(R.id.text_volunteer_address);

                    if (txtPhone != null)
                        txtPhone.setText(phone != null ? "Phone: " + phone : "Phone: N/A");
                    if (txtAddr != null)
                        txtAddr.setText(address != null ? "Address: " + address : "Address: N/A");
                })
                .addOnFailureListener(e -> txtVolunteerName.setText("Hello, Volunteer"));
    }

    // ---------------- LOAD MY ACCEPTED TASKS ----------------
    private void loadMyTasks() {
        txtBottomSheetTitle.setText("My Accepted Tasks");
        if (firestoreListener != null)
            firestoreListener.remove();
        if (auth.getCurrentUser() == null)
            return;

        Query query = firestore.collection(Constants.KEY_COLLECTION_REQUESTS)
                .whereEqualTo("volunteerId", auth.getCurrentUser().getUid())
                .orderBy("timestamp", Query.Direction.DESCENDING);

        firestoreListener = query.addSnapshotListener((value, error) -> {
            if (error != null) {
                // showToast("Failed to load my tasks");
                return;
            }
            if (value != null) {
                requestList.clear();
                for (DocumentSnapshot doc : value.getDocuments()) {
                    try {
                        RequestModel req = doc.toObject(RequestModel.class);
                        if (req != null) {
                            req.setDocumentId(doc.getId());
                            requestList.add(req);
                        }
                    } catch (Exception ignored) {
                    }
                }
                adapter.setViewingMyTasks(true);
                adapter.notifyDataSetChanged();
            }
        });
    }

    // ---------------- LOAD REQUESTS ----------------
    private void loadRequests(String category) {
        // Ensure standard mode is visible
        if (cardActiveMission.getVisibility() == View.VISIBLE) {
            // Logic to handle visibility if needed
        }

        txtBottomSheetTitle.setText("Showing: " + category + " Requests (Nearby)");
        if (firestoreListener != null)
            firestoreListener.remove();

        Query query;
        if (Constants.TYPE_MEDICAL.equals(category)) {
            // Include all medical sub-types
            query = firestore.collection(Constants.KEY_COLLECTION_REQUESTS)
                    .whereIn("type", java.util.Arrays.asList(
                            Constants.TYPE_MEDICAL,
                            "Medical Emergency",
                            "Doctor Appointment",
                            "Hospital Accompaniment",
                            "Doctor Home Visit"))
                    .whereEqualTo("status", Constants.STATUS_PENDING);
        } else {
            query = firestore.collection(Constants.KEY_COLLECTION_REQUESTS)
                    .whereEqualTo("type", category)
                    .whereEqualTo("status", Constants.STATUS_PENDING);
        }

        firestoreListener = query.addSnapshotListener((value, error) -> {
            if (error != null) {
                requestList.clear();
                adapter.notifyDataSetChanged();
                return;
            }
            if (value != null) {
                requestList.clear();
                for (DocumentSnapshot doc : value.getDocuments()) {
                    try {
                        RequestModel req = doc.toObject(RequestModel.class);
                        if (req != null) {
                            req.setDocumentId(doc.getId());

                            // FILTER logic:
                            double reqLat = req.getLatitude();
                            double reqLng = req.getLongitude();

                            if (currentLat != 0 && currentLng != 0 && reqLat != 0 && reqLng != 0) {
                                double dist = calculateDistance(currentLat, currentLng, reqLat, reqLng);
                                if (dist <= MAX_DISTANCE_KM) {
                                    requestList.add(req);
                                }
                            } else {
                                // Fallback: If locations are missing, show by default so we don't hide
                                // everything
                                requestList.add(req);
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
                adapter.setViewingMyTasks(false);
                adapter.notifyDataSetChanged();
            }
        });
    }

    // ---------------- LOCATION & FILTERING ----------------
    private double currentLat = 0.0;
    private double currentLng = 0.0;
    private static final double MAX_DISTANCE_KM = 50.0; // Filter requests within 50km

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(this,
                    new String[] { android.Manifest.permission.ACCESS_FINE_LOCATION }, 100);
        } else {
            fetchVolunteerLocation();
        }
    }

    private void fetchVolunteerLocation() {
        com.google.android.gms.location.FusedLocationProviderClient fusedLocationClient = com.google.android.gms.location.LocationServices
                .getFusedLocationProviderClient(this);
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    currentLat = location.getLatitude();
                    currentLng = location.getLongitude();
                    // Optional: Refresh list if already loaded but filter wasn't applied
                    if (adapter != null)
                        adapter.notifyDataSetChanged();
                }
            });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0
                && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            fetchVolunteerLocation();
        } else {
            Toast.makeText(this, "Location permission required for nearby requests", Toast.LENGTH_SHORT).show();
        }
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        if (lat1 == 0 || lat2 == 0)
            return 0; // If either is missing, assume 0 distance or handle differently
        // Haversine formula
        double R = 6371; // Radius of the earth in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // Distance in km
    }

    // ---------------- ADAPTER ----------------
    static class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.ViewHolder> {

        private final List<RequestModel> list;
        private final Context context;
        private final FirebaseFirestore fStore;
        private final FirebaseAuth mAuth;
        private boolean isViewingMyTasks = false;

        public RequestAdapter(List<RequestModel> list, Context context,
                FirebaseFirestore firestore, FirebaseAuth auth) {
            this.list = list;
            this.context = context;
            this.fStore = firestore;
            this.mAuth = auth;
        }

        public void setViewingMyTasks(boolean viewingMyTasks) {
            this.isViewingMyTasks = viewingMyTasks;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_request_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

            final RequestModel req = list.get(position);
            if (req == null)
                return;

            String type = req.getType() != null ? req.getType() : "N/A";
            holder.txtType.setText(type);

            String location = req.getLocation() != null
                    ? req.getLocation()
                    : "No location provided";
            holder.txtLocation.setText(location);

            // Date & Time Binding
            if (req.getTimestamp() != null) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());
                holder.txtDate.setText(sdf.format(req.getTimestamp().toDate()));
                holder.txtDate.setVisibility(View.VISIBLE);
            } else {
                holder.txtDate.setVisibility(View.GONE);
            }

            // Show description or items count
            String description = req.getDescription();
            if (description != null && !description.trim().isEmpty()) {
                holder.txtDesc.setText(description);
            } else if (Constants.TYPE_GROCERY.equalsIgnoreCase(type)) {
                List<Map<String, Object>> items = req.getItems();
                holder.txtDesc.setText(
                        (items != null ? items.size() : 0) + " items in order");
            } else {
                holder.txtDesc.setText("No details provided");
            }

            // Priority Logic
            if (isViewingMyTasks) {
                holder.txtPriority.setText("Status: " + req.getStatus());
                holder.txtPriority.setTextColor(ContextCompat.getColor(context, android.R.color.black));
            } else {
                // Priority logic...
                String priority = req.getPriority() != null ? req.getPriority() : "Normal";
                holder.txtPriority.setText(priority.toUpperCase(Locale.US) + " PRIORITY");
                holder.txtPriority.setTextColor(
                        ContextCompat.getColor(context,
                                "High".equalsIgnoreCase(priority)
                                        ? android.R.color.holo_red_dark
                                        : android.R.color.holo_green_dark));
            }

            // Fetch Senior Name
            String userId = req.getUserId();
            if (userId != null && !userId.isEmpty()) {
                fStore.collection(Constants.KEY_COLLECTION_USERS).document(userId).get()
                        .addOnSuccessListener(ds -> {
                            String name = ds.getString(Constants.KEY_NAME);
                            if (name == null)
                                name = ds.getString("fName");
                            if (name == null)
                                name = ds.getString("fullName");
                            holder.txtName.setText(name != null ? "Senior: " + name : "Senior: Name not found");
                        });
            }

            // Image Logic
            if (req.getImageUrl() != null && !req.getImageUrl().isEmpty()) {
                holder.imgReq.setVisibility(View.VISIBLE);
                com.bumptech.glide.Glide.with(context)
                        .load(req.getImageUrl())
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(holder.imgReq);

                // Optional: Click to view full screen (simple implementation for now)
                holder.imgReq.setOnClickListener(v -> {
                    // In a real app, open a dialog or new activity
                    Toast.makeText(context, "Prescription Image", Toast.LENGTH_SHORT).show();
                });
            } else {
                holder.imgReq.setVisibility(View.GONE);
            }

            // Button Logic - Dynamic State Machine
            holder.btnAccept.setVisibility(View.VISIBLE);

            if (!isViewingMyTasks) {
                // Available Request -> Accept
                holder.btnAccept.setText("ACCEPT REQUEST");
                holder.btnAccept.setBackgroundTintList(
                        ContextCompat.getColorStateList(context, android.R.color.holo_green_dark));
                holder.btnAccept.setOnClickListener(v -> {
                    if (mAuth.getCurrentUser() != null) {
                        acceptRequest(req, mAuth.getCurrentUser().getUid());
                    }
                });
            } else {
                // My Task -> Status Progression
                String status = req.getStatus();

                if (Constants.STATUS_ACCEPTED.equals(status)) {
                    holder.btnAccept.setText("START TRIP (On My Way)");
                    holder.btnAccept.setBackgroundTintList(
                            ContextCompat.getColorStateList(context, android.R.color.holo_blue_dark));
                    holder.btnAccept.setOnClickListener(
                            v -> updateStatus(req, Constants.STATUS_ON_THE_WAY, "Volunteer is on the way!"));
                } else if (Constants.STATUS_ON_THE_WAY.equals(status)) {
                    holder.btnAccept.setText("ARRIVED");
                    holder.btnAccept.setBackgroundTintList(
                            ContextCompat.getColorStateList(context, android.R.color.holo_orange_dark));
                    holder.btnAccept.setOnClickListener(
                            v -> updateStatus(req, Constants.STATUS_ARRIVED, "Volunteer has arrived!"));
                } else if (Constants.STATUS_ARRIVED.equals(status)) {
                    holder.btnAccept.setText("START SERVICE");
                    holder.btnAccept.setBackgroundTintList(
                            ContextCompat.getColorStateList(context, android.R.color.holo_purple));
                    holder.btnAccept.setOnClickListener(
                            v -> updateStatus(req, Constants.STATUS_IN_PROGRESS, "Service has started."));
                } else if (Constants.STATUS_IN_PROGRESS.equals(status)) {
                    holder.btnAccept.setText("MARK COMPLETED");
                    holder.btnAccept.setBackgroundTintList(
                            ContextCompat.getColorStateList(context, android.R.color.holo_green_light));
                    holder.btnAccept.setOnClickListener(
                            v -> updateStatus(req, Constants.STATUS_COMPLETED, "Service Completed."));
                } else if (Constants.STATUS_COMPLETED.equals(status)) {
                    holder.btnAccept.setVisibility(View.GONE);
                }
            }
        }

        private void acceptRequest(RequestModel req, String volunteerId) {
            final com.google.firebase.firestore.DocumentReference docRef = fStore
                    .collection(Constants.KEY_COLLECTION_REQUESTS).document(req.getDocumentId());

            fStore.runTransaction(transaction -> {
                com.google.firebase.firestore.DocumentSnapshot snapshot = transaction.get(docRef);
                String currentStatus = snapshot.getString("status");

                if (Constants.STATUS_PENDING.equals(currentStatus)) {
                    transaction.update(docRef, "status", Constants.STATUS_ACCEPTED);
                    transaction.update(docRef, "volunteerId", volunteerId);
                    return null; // Success
                } else {
                    throw new com.google.firebase.firestore.FirebaseFirestoreException(
                            "Request already taken",
                            com.google.firebase.firestore.FirebaseFirestoreException.Code.ABORTED);
                }
            }).addOnSuccessListener(result -> {
                Toast.makeText(context, "Request Accepted Successfully!", Toast.LENGTH_SHORT).show();
                // Notify Senior
                com.example.seniorcitizensupport.utils.NotificationHelper.sendNotification(
                        req.getUserId(),
                        "Request Accepted",
                        "A volunteer has accepted your request.");

            }).addOnFailureListener(e -> {
                if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException) {
                    Toast.makeText(context, "Too late! Request already taken.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context, "Error accepting: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        private void updateStatus(RequestModel req, String newStatus, String notificationMsg) {
            fStore.collection(Constants.KEY_COLLECTION_REQUESTS).document(req.getDocumentId())
                    .update("status", newStatus)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "Status Updated: " + newStatus, Toast.LENGTH_SHORT).show();
                        // Send Notification to Senior
                        com.example.seniorcitizensupport.utils.NotificationHelper.sendNotification(
                                req.getUserId(),
                                "Status Update",
                                notificationMsg);
                    })
                    .addOnFailureListener(
                            e -> Toast.makeText(context, "Error updating status", Toast.LENGTH_SHORT).show());
        }

        @Override
        public int getItemCount() {
            return list != null ? list.size() : 0;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtType, txtDesc, txtPriority, txtName, txtLocation, txtDate;
            android.widget.ImageView imgReq;
            Button btnAccept;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                txtType = itemView.findViewById(R.id.req_type);
                txtDesc = itemView.findViewById(R.id.req_desc);
                txtPriority = itemView.findViewById(R.id.req_priority);
                txtName = itemView.findViewById(R.id.req_senior_name);
                txtLocation = itemView.findViewById(R.id.req_location);
                txtDate = itemView.findViewById(R.id.req_date_time);
                imgReq = itemView.findViewById(R.id.req_image);
                btnAccept = itemView.findViewById(R.id.btn_accept);
            }
        }
    }
}
