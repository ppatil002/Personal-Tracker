package com.example.pp.infra.background;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.pp.R;
import com.example.pp.data.local.AppDatabase;
import com.example.pp.data.local.dao.BackgroundJobLogDao;
import com.example.pp.data.local.entity.BackgroundJobLog;

public abstract class BaseFeatureWorker extends Worker {

    protected final BackgroundJobLogDao logDao;
    protected final String jobId;
    private static final String CHANNEL_ID = "BG_SYNC_CHANNEL";
    private static final int NOTIFICATION_ID = 1001;

    public BaseFeatureWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.logDao = AppDatabase.getInstance(context).backgroundJobLogDao();
        this.jobId = params.getInputData().getString("EXECUTION_SESSION_ID") != null ? 
                     params.getInputData().getString("EXECUTION_SESSION_ID") : getId().toString();
    }

    protected void updateLog(String status, String step, String message) {
        BackgroundJobLog log = new BackgroundJobLog(
                jobId,
                getClass().getSimpleName(),
                status,
                step,
                message,
                System.currentTimeMillis()
        );
        logDao.insert(log);
    }

    protected ForegroundInfo createForegroundInfo(String taskName, String currentStep, int statusStyle) {
        Context ctx = getApplicationContext();
        createNotificationChannel();

        Notification notification = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Fallback icon
                .setContentTitle("PP Tracker Ecosystem Engine")
                .setContentText("Running: " + taskName + " [" + currentStep + "]")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setColor(statusStyle == 1 ? Color.parseColor("#22D3EE") : Color.parseColor("#B3B3C3"))
                .setOngoing(statusStyle == 1)
                .build();

        return new ForegroundInfo(NOTIFICATION_ID, notification);
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Background Sync",
                NotificationManager.IMPORTANCE_LOW
        );
        NotificationManager manager = getApplicationContext().getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }
}