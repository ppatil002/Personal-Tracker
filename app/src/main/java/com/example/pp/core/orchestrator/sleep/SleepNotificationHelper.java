package com.example.pp.core.orchestrator.sleep;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;

import androidx.core.app.NotificationCompat;
import androidx.core.app.RemoteInput;

import com.example.pp.R;
import com.example.pp.core.constants.AppConstants;
import com.example.pp.features.dashboard.DashboardActivity;

public class SleepNotificationHelper {
    public static final String CHANNEL_ID = AppConstants.CHANNEL_SLEEP;
    public static final int NOTIFICATION_ID = AppConstants.NOTIFY_ID_SLEEP;

    public static final String ACTION_START_SLEEP = "com.example.pp.ACTION_START_SLEEP";
    public static final String ACTION_STOP_SLEEP = "com.example.pp.ACTION_STOP_SLEEP";
    public static final String ACTION_DELAY_REASON = "com.example.pp.ACTION_DELAY_REASON";
    public static final String KEY_TEXT_REPLY = "key_text_reply";

    public static void showBedtimeReminder(Context context, String message, boolean showBedButton, boolean showDelayButton) {
        createChannel(context);

        Intent intent = new Intent(context, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_sleep)
                .setContentTitle("Bedtime Reminder")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.sleep_notification))
                .setAutoCancel(true)
                .setContentIntent(contentIntent);

        if (showBedButton) {
            Intent bedIntent = new Intent(context, SleepReceiver.class);
            bedIntent.setAction(ACTION_START_SLEEP);
            PendingIntent bedPI = PendingIntent.getBroadcast(context, 1, bedIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            builder.addAction(R.drawable.ic_sleep, "Good Night", bedPI);
        }

        if (showDelayButton) {
            // Add Delay Reason action
            RemoteInput remoteInput = new RemoteInput.Builder(KEY_TEXT_REPLY)
                    .setLabel("Why the delay?")
                    .build();

            Intent replyIntent = new Intent(context, SleepReceiver.class);
            replyIntent.setAction(ACTION_DELAY_REASON);
            PendingIntent replyPI = PendingIntent.getBroadcast(context, 3, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);

            NotificationCompat.Action action = new NotificationCompat.Action.Builder(
                    R.drawable.ic_note, "Delay Reason", replyPI)
                    .addRemoteInput(remoteInput)
                    .build();

            builder.addAction(action);
        }

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    public static void showWakeUpNotification(Context context) {
        createChannel(context);

        Intent intent = new Intent(context, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        Intent wakeIntent = new Intent(context, SleepReceiver.class);
        wakeIntent.setAction(ACTION_STOP_SLEEP);
        PendingIntent wakePI = PendingIntent.getBroadcast(context, 2, wakeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // TODO Can add daily quotes here at the Good Morning Notification.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_sleep)
                .setContentTitle("Good Morning!")
                .setContentText("Hope you had a great sleep. Time to start your day!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.sleep_notification))
                .setOngoing(true)
                .setContentIntent(contentIntent)
                .addAction(R.drawable.ic_plus, "Good Morning", wakePI);

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

    private static void createChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Sleep Monitor", NotificationManager.IMPORTANCE_HIGH);
        Uri soundUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.sleep_notification);
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build();
        channel.setSound(soundUri,audioAttributes);
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }
}
