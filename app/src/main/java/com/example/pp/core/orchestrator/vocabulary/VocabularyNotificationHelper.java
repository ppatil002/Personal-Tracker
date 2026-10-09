package com.example.pp.core.orchestrator.vocabulary;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

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
                .setSmallIcon(R.drawable.ic_book)
                .setContentTitle("Lets Revise")
                .setContentText("Do you recall the meaning of \"" + vocab.word + "\"?")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(vocab.word + "\nDo you recall its meaning?"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(getContentIntent(context))
                .addAction(R.drawable.ic_bulb, "Hint", piHint)
                .addAction(R.drawable.ic_check, "Remember", piRemember)
                .addAction(R.drawable.ic_delete, "Forget", piForget);

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
                : "Usage: N/A";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_book)
                .setContentTitle("Hint for: " + vocab.word)
                .setContentText(usageText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText("💡 " + usageText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(getContentIntent(context))
                .addAction(R.drawable.ic_check, "Remember", piRemember)
                .addAction(R.drawable.ic_delete, "Forget", piForget);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    /**
     * Stage 3: Rating Notification
     * Prompts the user to rate recall efficiency using a 5-star layout in a custom view,
     * revealing the actual meaning of the word so the user can revalidate their recall.
     */
    public static void showRatingNotification(Context context, Vocabulary vocab, boolean remembered, boolean hintUsed, String taskId) {
        createNotificationChannel(context);

        String title = "Rate Recall: " + vocab.word;
        String meaningText = (vocab.meaning != null && !vocab.meaning.trim().isEmpty())
                ? "Meaning: " + vocab.meaning
                : "Meaning: N/A";

        RemoteViews ratingViews = new RemoteViews(context.getPackageName(), R.layout.notification_vocabulary_rating);
        ratingViews.setTextViewText(R.id.tv_rating_title, title);
        ratingViews.setTextViewText(R.id.tv_rating_status, remembered ? "Remembered" : "Forgotten");
        ratingViews.setTextColor(R.id.tv_rating_status, remembered ? 0xFF22C55E : 0xFFF87171);
        ratingViews.setTextViewText(R.id.tv_rating_subtitle, meaningText);

        int[] starIds = new int[]{
                R.id.star_1, R.id.star_2, R.id.star_3, R.id.star_4, R.id.star_5
        };

        for (int i = 0; i < starIds.length; i++) {
            int starRating = i + 1;
            Intent rateIntent = new Intent(context, VocabularyReceiver.class);
            rateIntent.setAction(ACTION_RATE);
            rateIntent.putExtra(EXTRA_VOCAB_ID, vocab.id);
            rateIntent.putExtra(EXTRA_TASK_ID, taskId);
            rateIntent.putExtra(EXTRA_REMEMBERED, remembered);
            rateIntent.putExtra(EXTRA_HINT_USED, hintUsed);
            rateIntent.putExtra(EXTRA_RATING, starRating);

            PendingIntent piRate = PendingIntent.getBroadcast(
                    context, 300 + starRating, rateIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            ratingViews.setOnClickPendingIntent(starIds[i], piRate);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_book)
                .setContentTitle(title)
                .setContentText(meaningText)
                .setCustomContentView(ratingViews)
                .setCustomBigContentView(ratingViews)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(getContentIntent(context));

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
