package com.example.pp.core.orchestrator.screentime;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.example.pp.R;
import com.example.pp.core.constants.AppConstants;
import com.example.pp.features.dashboard.DashboardActivity;
import com.example.pp.utils.DateUtils;

public class ScreenTimeNotificationHelper {
    private static final String CHANNEL_ID_ALERTS = AppConstants.CHANNEL_SCREEN_TIME;
    private static final String CHANNEL_ID_SERVICE = AppConstants.CHANNEL_SCREEN_TIME_SERVICE;
    
    private static final int BREAK_NOTIFY_ID = AppConstants.NOTIFY_ID_SCREEN_BREAK;

    public static void showBreakNotification(Context context, long elapsedMillis) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        createAlertChannel(context);

        Intent dashboardIntent = new Intent(context, DashboardActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context, 
                0, 
                dashboardIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String timeStr = DateUtils.convertTimestampIntoHoursAndMinutes(elapsedMillis);
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
                .setSmallIcon(R.drawable.ic_smartphone)
                .setContentTitle("Take a break!")
                .setContentText("You've been on your phone for " + timeStr + " continuously.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .build();

        manager.notify(BREAK_NOTIFY_ID, notification);
    }

    public static Notification getServiceNotification(Context context) {
        createServiceChannel(context);
        
        Intent dashboardIntent = new Intent(context, DashboardActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context, 
                0, 
                dashboardIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(context, CHANNEL_ID_SERVICE)
                .setSmallIcon(R.drawable.ic_smartphone)
                .setContentTitle("Screen Monitoring Active")
                .setContentText("Tracking usage and wellness sessions")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setContentIntent(contentIntent)
                .build();
    }

    public static Notification getPausedServiceNotification(Context context) {
        createServiceChannel(context);

        Intent dashboardIntent = new Intent(context, DashboardActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                0,
                dashboardIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(context, CHANNEL_ID_SERVICE)
                .setSmallIcon(R.drawable.ic_smartphone)
                .setContentTitle("Screen Monitoring Paused")
                .setContentText("Monitoring is temporarily disabled")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setContentIntent(contentIntent)
                .build();
    }

    private static void createAlertChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID_ALERTS,
                "Screen Time Alerts",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Notifications for screen time limits and break reminders");
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    private static void createServiceChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID_SERVICE,
                "Screen Monitoring Status",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Shows if screen tracking is currently active or paused");
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }
}
