package com.example.pp.core.orchestrator.water;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;

import androidx.core.app.NotificationCompat;

import com.example.pp.PPApplication;
import com.example.pp.R;
import com.example.pp.core.constants.AppConstants;
import com.example.pp.data.local.entity.HydrationIntake;
import com.example.pp.data.repository.HydrationRepository;
import com.example.pp.features.dashboard.DashboardActivity;
import com.example.pp.utils.DateUtils;

import java.util.concurrent.TimeUnit;

public class WaterNotificationHelper {
    public static final String CHANNEL_ID = AppConstants.CHANNEL_WATER;
    public static final int NOTIFICATION_ID = AppConstants.NOTIFY_ID_WATER;
    
    public static final String ACTION_300ML = "com.example.pp.ACTION_300ML";
    public static final String ACTION_250ML = "com.example.pp.ACTION_250ML";
    public static final String ACTION_SNOOZE = "com.example.pp.ACTION_SNOOZE";

    private static HydrationRepository getHydrationRepository(Context context) {
        Context app = context.getApplicationContext();
        if (app instanceof PPApplication) {
            return ((PPApplication) app).getHydrationRepository();
        }
        return new HydrationRepository(context);
    }

    private static void createNotificationChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Water Intake Reminders", NotificationManager.IMPORTANCE_HIGH);
        
        Uri soundUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.water_notofication_drop);
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build();
        channel.setSound(soundUri, audioAttributes);

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    public static void showNotification(Context context, String taskId) {
        showNotification(context, taskId, 0L, 0);
    }

    public static void showNotification(Context context, String taskId, long lastIntakeTime, int lastIntakeAmount) {
        createNotificationChannel(context);

        HydrationRepository hydrationRepository = getHydrationRepository(context);

        final HydrationIntake lastIntake = (lastIntakeTime <= 0L) ? hydrationRepository.findLatestIntake() : null;
        final long intakeTime = (lastIntakeTime > 0L) ? lastIntakeTime : (lastIntake != null ? lastIntake.getTimestamp() : 0L);

        long startOfDay = DateUtils.getStartOfDay(System.currentTimeMillis());
        int totalTodayMl = hydrationRepository.getTodayTotalMLSync(startOfDay);
        int targetMl = (int) AppConstants.TARGET_WATER_ML;
        int percentage = (int) Math.min(100, Math.round(((double) totalTodayMl / targetMl) * 100));

        final String contentText;
        if (intakeTime > 0L) {
            long elapsedMillis = Math.max(0, System.currentTimeMillis() - intakeTime);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMillis) % 60;
            long hours = TimeUnit.MILLISECONDS.toHours(elapsedMillis);

            String timeGap;
            if (hours > 0 && minutes > 0) {
                timeGap = hours + "h " + minutes + "m ago";
            } else if (hours > 0) {
                timeGap = hours + (hours == 1 ? " hour ago" : " hours ago");
            } else {
                timeGap = Math.max(1, minutes) + " mins ago";
            }

            contentText = "Last drink: " + timeGap + " • " + totalTodayMl + "/" + targetMl + " ml (" + percentage + "%)";
        } else {
            contentText = "Progress: " + totalTodayMl + "/" + targetMl + " ml (" + percentage + "%) • Time to hydrate!";
        }

        Intent intent250 = new Intent(context, WaterIntakeReceiver.class);
        intent250.setAction(ACTION_250ML);
        intent250.putExtra("TASK_ID", taskId);
        PendingIntent pi250 = PendingIntent.getBroadcast(context, 1, intent250, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent intent300 = new Intent(context, WaterIntakeReceiver.class);
        intent300.setAction(ACTION_300ML);
        intent300.putExtra("TASK_ID", taskId);
        PendingIntent pi300 = PendingIntent.getBroadcast(context, 2, intent300, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent intentSnooze = new Intent(context, WaterIntakeReceiver.class);
        intentSnooze.setAction(ACTION_SNOOZE);
        intentSnooze.putExtra("TASK_ID", taskId);
        PendingIntent piSnooze = PendingIntent.getBroadcast(context, 3, intentSnooze, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent dashIntent = new Intent(context, DashboardActivity.class);
        dashIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, dashIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_glass)
                .setContentTitle("Water Intake")
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setSound(Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.water_notofication_drop))
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .addAction(0, "300ml", pi300)
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
