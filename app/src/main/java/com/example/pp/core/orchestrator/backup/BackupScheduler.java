package com.example.pp.core.orchestrator.backup;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.pp.data.local.entity.TaskDefinition;

import java.util.Calendar;

public class BackupScheduler {
    private static final String TAG = "BackupScheduler";
    public static final int ALARM_REQUEST_CODE = 8000;

    public static void schedule(Context context, TaskDefinition def) {
        Log.i(TAG, "Scheduling 24H backup alarm at 9:00 AM.");

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, BackupAlarmReceiver.class);
        intent.putExtra("TASK_ID", def.getTaskId());
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 
                ALARM_REQUEST_CODE, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Calculate 9 AM today or tomorrow
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        Log.d(TAG, "Alarm set for: " + calendar.getTime().toString());

        if (alarmManager != null) {
            // Use setExactAndAllowWhileIdle to bypass Doze mode
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
            );
        }
    }
}
