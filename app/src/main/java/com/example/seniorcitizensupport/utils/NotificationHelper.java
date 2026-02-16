package com.example.seniorcitizensupport.utils;

import com.example.seniorcitizensupport.Constants;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class NotificationHelper {

    /**
     * Sends a notification to the specified user by adding a document to the
     * 'notifications' collection in Firestore.
     * The app (on Senior's side) should be listening to this collection to show a
     * local notification or UI update.
     *
     * @param userId  The ID of the user to receive the notification (Senior
     *                Citizen).
     * @param title   Title of the notification.
     * @param message Body of the notification.
     */
    public static void sendNotification(String userId, String title, String message) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("title", title);
        notification.put("message", message);
        notification.put("read", false);
        notification.put("timestamp", com.google.firebase.Timestamp.now());

        db.collection(Constants.KEY_COLLECTION_NOTIFICATIONS)
                .add(notification)
                .addOnSuccessListener(documentReference -> {
                    // Log success if needed
                })
                .addOnFailureListener(e -> {
                    // Log failure
                });
    }

    /**
     * Shows a local system notification.
     * 
     * @param context Application context.
     * @param title   Notification title.
     * @param message Notification message.
     */
    public static void showNotification(android.content.Context context, String title, String message) {
        String channelId = "emergency_channel";
        android.app.NotificationManager notificationManager = (android.app.NotificationManager) context
                .getSystemService(android.content.Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                    channelId,
                    "Emergency Alerts",
                    android.app.NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(context,
                channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_alert) // Use a valid icon resource
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
