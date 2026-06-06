package com.example.pp.core.orchestrator.screentime;

import static com.example.pp.core.constants.AppConstants.SCREEN_WORKER;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.pp.PPApplication;
import com.example.pp.core.constants.AppConstants;
import com.example.pp.core.preferences.PreferenceManager;
import com.example.pp.core.task.AppExecutors;
import com.example.pp.data.local.entity.BackgroundJobLog;
import com.example.pp.utils.DateUtils;

public class ScreenTimeReceiver extends BroadcastReceiver {
    private static final String TAG = "ScreenTimeReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        long now = System.currentTimeMillis();
        PreferenceManager prefManager = PreferenceManager.getInstance(context);
        long sessionStartTime = prefManager.getLong(AppConstants.KEY_SCREEN_SESSION_START, 0);

        if (sessionStartTime == 0) {
            Log.d(TAG, "Alarm fired but session is not active. Ignoring.");
            return;
        }

        long elapsedMillis = now - sessionStartTime;
        Log.i(TAG, "Break Alarm Fired! Session active for: " + elapsedMillis + "ms");
        // Show Notification with elapsed time
        ScreenTimeNotificationHelper.showBreakNotification(context, elapsedMillis);

        // Log to Background Job Logs
        BackgroundJobLog screenLog = new BackgroundJobLog(
                TAG + "_" + DateUtils.convertTimestampintoHH_MM_SS_String(System.currentTimeMillis()),
                SCREEN_WORKER,
                "BREAK FIRED",
                "Running since @ " + DateUtils.convertTimestampIntoHoursAndMinutes(elapsedMillis),
                "Continuous usage alert triggered. Elapsed: " + (elapsedMillis / 60000) + "m",
                System.currentTimeMillis()
        );
        AppExecutors.getInstance().diskIO().execute(() -> {
            ((PPApplication) context.getApplicationContext())
                    .getBackgroundJobLogRepository()
                    .mergeDuplicateDailyWorkerLog(screenLog);
        });

        // Reschedule for next 20 minutes since session is still active
        scheduleNextAlarm(context);
    }

    private void scheduleNextAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ScreenTimeReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 
                0, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long triggerAtMillis = System.currentTimeMillis() + AppConstants.SCREEN_BREAK_INTERVAL_MS;
        if (alarmManager != null) {
            AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(triggerAtMillis, pendingIntent);
            alarmManager.setAlarmClock(info, pendingIntent);
//            Log.d(TAG, "Next break alarm scheduled for +20m.");
        }
    }
}
