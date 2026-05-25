package com.example.pp.core.orchestrator.backup;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.example.pp.R;
import com.example.pp.features.settings.WorkerListActivity;
import com.example.pp.data.local.entity.TaskDefinition;
import com.example.pp.data.local.entity.TaskState;

public class BackupNotificationHelper {
    public static final String CHANNEL_ID = "BACKUP_SERVICE_CHANNEL";
    public static final int NOTIFICATION_ID = 5001;

    public static void createNotificationChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Backup Background Service", NotificationManager.IMPORTANCE_LOW);
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    public static Notification createNotification(Context context, String content, boolean isOngoing) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Data Backup")
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_cloud_upload)
                .setPriority(isOngoing ? NotificationCompat.PRIORITY_LOW : NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(isOngoing)
                .setAutoCancel(!isOngoing);

        if (!isOngoing) {
            Intent intent = new Intent(context, WorkerListActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            builder.setContentIntent(pendingIntent);
        }

        return builder.build();
    }

    public static void updateNotification(Context context, String content) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createNotification(context, content, true));
        }
    }

    public static void showFinalNotification(Context context, String content) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createNotification(context, content, false));
        }
    }

    public static void showConstraintFailureNotification(Context context, TaskDefinition def, TaskState state) {
        StringBuilder reason = new StringBuilder("Waiting for: ");
        if (def.isRequiresInternet() && !com.example.pp.utils.ConstraintEvaluator.isInternetAvailable(context)) {
            reason.append("Wi-Fi/Internet ");
        }
        if (com.example.pp.utils.ConstraintEvaluator.getBatteryLevel(context) < def.getMinimumBattery()) {
            reason.append("Charger (Battery < ").append(def.getMinimumBattery()).append("%)");
        }
        
        updateNotification(context, reason.toString());
    }

    public static void cancelNotification(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(NOTIFICATION_ID);
        }
    }
}
