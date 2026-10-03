package com.example.pp.core.orchestrator.vocabulary;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.example.pp.R;
import com.example.pp.core.constants.AppConstants;
import com.example.pp.data.local.entity.Vocabulary;
import com.example.pp.features.dashboard.DashboardActivity;

public class VocabularyNotificationHelper {

    public static final String CHANNEL_ID = AppConstants.CHANNEL_VOCABULARY;
    public static final int NOTIFICATION_ID = AppConstants.NOTIFY_ID_VOCABULARY;

    public static final String ACTION_HINT = "com.example.pp.ACTION_VOCAB_HINT";
    public static final String ACTION_REMEMBER = "com.example.pp.ACTION_VOCAB_REMEMBER";
    public static final String ACTION_FORGET = "com.example.pp.ACTION_VOCAB_FORGET";
    public static final String ACTION_RATE = "com.example.pp.ACTION_VOCAB_RATE";

    public static final String EXTRA_VOCAB_ID = "VOCAB_ID";
    public static final String EXTRA_TASK_ID = "TASK_ID";
    public static final String EXTRA_HINT_USED = "HINT_USED";
    public static final String EXTRA_REMEMBERED = "REMEMBERED";
    public static final String EXTRA_RATING = "RATING";

    private static void createNotificationChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Vocabulary Revision Reminders",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Revision for new vocabulary");

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    private static PendingIntent getContentIntent(Context context) {
        Intent dashIntent = new Intent(context, DashboardActivity.class);
        dashIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, 0, dashIntent, PendingIntent.FLAG_IMMUTABLE);
    }

    /**
     * Stage 1: Initial Notification
     * Displays the word and 3 options: Hint, Remember, Forget.
     */
    public static void showInitialNotification(Context context, Vocabulary vocab, String taskId) {
        createNotificationChannel(context);

        Intent hintIntent = new Intent(context, VocabularyReceiver.class);
        hintIntent.setAction(ACTION_HINT);
        hintIntent.putExtra(EXTRA_VOCAB_ID, vocab.id);
        hintIntent.putExtra(EXTRA_TASK_ID, taskId);
        PendingIntent piHint = PendingIntent.getBroadcast(context, 101, hintIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent rememberIntent = new Intent(context, VocabularyReceiver.class);
        rememberIntent.setAction(ACTION_REMEMBER);
        rememberIntent.putExtra(EXTRA_VOCAB_ID, vocab.id);
        rememberIntent.putExtra(EXTRA_TASK_ID, taskId);
        rememberIntent.putExtra(EXTRA_HINT_USED, false);
        PendingIntent piRemember = PendingIntent.getBroadcast(context, 102, rememberIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent forgetIntent = new Intent(context, VocabularyReceiver.class);
        forgetIntent.setAction(ACTION_FORGET);
        forgetIntent.putExtra(EXTRA_VOCAB_ID, vocab.id);
        forgetIntent.putExtra(EXTRA_TASK_ID, taskId);
        forgetIntent.putExtra(EXTRA_HINT_USED, false);
        PendingIntent piForget = PendingIntent.getBroadcast(context, 103, forgetIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_glass) // Reuse available drawable icon
                .setContentTitle("Vocabulary: " + vocab.word)
                .setContentText("Do you recall the meaning of \"" + vocab.word + "\"?")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Word: " + vocab.word + "\nDo you recall its meaning?"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(getContentIntent(context))
                .addAction(0, "Hint", piHint)
                .addAction(0, "Remember", piRemember)
                .addAction(0, "Forget", piForget);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    /**
     * Stage 2: Hint Notification
     * Displays word usage/example as a hint, and 2 options: Remember, Forget.
     */
    public static void showHintNotification(Context context, Vocabulary vocab, String taskId) {
        createNotificationChannel(context);

        Intent rememberIntent = new Intent(context, VocabularyReceiver.class);
        rememberIntent.setAction(ACTION_REMEMBER);
        rememberIntent.putExtra(EXTRA_VOCAB_ID, vocab.id);
        rememberIntent.putExtra(EXTRA_TASK_ID, taskId);
        rememberIntent.putExtra(EXTRA_HINT_USED, true);
        PendingIntent piRemember = PendingIntent.getBroadcast(context, 201, rememberIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent forgetIntent = new Intent(context, VocabularyReceiver.class);
        forgetIntent.setAction(ACTION_FORGET);
        forgetIntent.putExtra(EXTRA_VOCAB_ID, vocab.id);
        forgetIntent.putExtra(EXTRA_TASK_ID, taskId);
        forgetIntent.putExtra(EXTRA_HINT_USED, true);
        PendingIntent piForget = PendingIntent.getBroadcast(context, 202, forgetIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String usageText = (vocab.example != null && !vocab.example.trim().isEmpty())
                ? "Usage: " + vocab.example
                : "Meaning: " + (vocab.meaning != null ? vocab.meaning : "N/A");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_glass)
                .setContentTitle("Hint for: " + vocab.word)
                .setContentText(usageText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText("💡 " + usageText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(getContentIntent(context))
                .addAction(0, "Remember", piRemember)
                .addAction(0, "Forget", piForget);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    /**
     * Stage 3: Rating Notification
     * Prompts the user to quickly rate recall efficiency between 1-2, 3-4, 5 star rating.
     */
    public static void showRatingNotification(Context context, Vocabulary vocab, boolean remembered, boolean hintUsed, String taskId) {
        createNotificationChannel(context);

        Intent rate12 = new Intent(context, VocabularyReceiver.class);
        rate12.setAction(ACTION_RATE);
        rate12.putExtra(EXTRA_VOCAB_ID, vocab.id);
        rate12.putExtra(EXTRA_TASK_ID, taskId);
        rate12.putExtra(EXTRA_REMEMBERED, remembered);
        rate12.putExtra(EXTRA_HINT_USED, hintUsed);
        rate12.putExtra(EXTRA_RATING, 2); // 1-2 rating bucket
        PendingIntent piRate12 = PendingIntent.getBroadcast(context, 301, rate12, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent rate34 = new Intent(context, VocabularyReceiver.class);
        rate34.setAction(ACTION_RATE);
        rate34.putExtra(EXTRA_VOCAB_ID, vocab.id);
        rate34.putExtra(EXTRA_TASK_ID, taskId);
        rate34.putExtra(EXTRA_REMEMBERED, remembered);
        rate34.putExtra(EXTRA_HINT_USED, hintUsed);
        rate34.putExtra(EXTRA_RATING, 4); // 3-4 rating bucket
        PendingIntent piRate34 = PendingIntent.getBroadcast(context, 302, rate34, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent rate5 = new Intent(context, VocabularyReceiver.class);
        rate5.setAction(ACTION_RATE);
        rate5.putExtra(EXTRA_VOCAB_ID, vocab.id);
        rate5.putExtra(EXTRA_TASK_ID, taskId);
        rate5.putExtra(EXTRA_REMEMBERED, remembered);
        rate5.putExtra(EXTRA_HINT_USED, hintUsed);
        rate5.putExtra(EXTRA_RATING, 5); // 5 rating bucket
        PendingIntent piRate5 = PendingIntent.getBroadcast(context, 303, rate5, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = "Rate Recall: " + vocab.word + " (" + (remembered ? "Remembered" : "Forgotten") + ")";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_glass)
                .setContentTitle(title)
                .setContentText("Rate how effectively you recalled this word:")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(getContentIntent(context))
                .addAction(0, "1-2 ⭐", piRate12)
                .addAction(0, "3-4 ⭐", piRate34)
                .addAction(0, "5 ⭐", piRate5);

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
