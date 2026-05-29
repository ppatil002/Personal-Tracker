package com.example.pp.core.orchestrator.water;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.example.pp.R;

public class WaterNotificationHelper {
    public static final String CHANNEL_ID = "WATER_REMINDER_CHANNEL";
    public static final int NOTIFICATION_ID = 5002;
    
    public static final String ACTION_100ML = "com.example.pp.ACTION_100ML";
    public static final String ACTION_250ML = "com.example.pp.ACTION_250ML";
    public static final String ACTION_SNOOZE = "com.example.pp.ACTION_SNOOZE";

    public static void createNotificationChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Water Intake Reminders", NotificationManager.IMPORTANCE_HIGH);
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    public static void showNotification(Context context, String taskId) {
        createNotificationChannel(context);
        
        Intent intent100 = new Intent(context, WaterIntakeReceiver.class);
        intent100.setAction(ACTION_100ML);
        intent100.putExtra("TASK_ID", taskId);
        PendingIntent pi100 = PendingIntent.getBroadcast(context, 1, intent100, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent intent250 = new Intent(context, WaterIntakeReceiver.class);
        intent250.setAction(ACTION_250ML);
        intent250.putExtra("TASK_ID", taskId);
        PendingIntent pi250 = PendingIntent.getBroadcast(context, 2, intent250, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent intentSnooze = new Intent(context, WaterIntakeReceiver.class);
        intentSnooze.setAction(ACTION_SNOOZE);
        intentSnooze.putExtra("TASK_ID", taskId);
        PendingIntent piSnooze = PendingIntent.getBroadcast(context, 3, intentSnooze, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_glass)
                .setContentTitle("Water Intake")
                .setContentText("Its Water Break")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .addAction(0, "100ml", pi100)
                .addAction(0, "250ml", pi250)
                .addAction(0, "Snooze", piSnooze);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    public static void cancelNotification(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(NOTIFICATION_ID);
        }
    }
}
