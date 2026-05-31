package com.example.pp.core.orchestrator.sleep;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.pp.PPApplication;
import com.example.pp.core.enums.TaskType;
import com.example.pp.data.local.entity.SleepEntity;
import com.example.pp.data.local.entity.TaskDefinition;
import com.example.pp.data.repository.SleepRepository;
import com.example.pp.data.repository.TaskRepository;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class SleepReceiver extends BroadcastReceiver {
    private static final String TAG = "SleepReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, "onReceive: " + action);

        if (SleepNotificationHelper.ACTION_START_SLEEP.equals(action)) {
            handleStartSleep(context);
        } else if (SleepNotificationHelper.ACTION_STOP_SLEEP.equals(action)) {
            handleStopSleep(context);
        } else {
            // Alarm trigger for reminders or wake up
            handleAlarmTrigger(context);
        }
    }

    private void handleStartSleep(Context context) {
        SleepRepository sleepRepo = ((PPApplication) context.getApplicationContext()).getSleepRepository();
        long now = System.currentTimeMillis();
        
        // Target wake time (default 6 AM next)
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 6);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= now) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        sleepRepo.startSleep(now, cal.getTimeInMillis());
        SleepNotificationHelper.cancelNotification(context);
        SleepScheduler.reschedule(context);
    }

    private void handleStopSleep(Context context) {
        SleepRepository sleepRepo = ((PPApplication) context.getApplicationContext()).getSleepRepository();
        sleepRepo.stopSleep(System.currentTimeMillis());
        SleepNotificationHelper.cancelNotification(context);
        SleepScheduler.reschedule(context);
    }

    private void handleAlarmTrigger(Context context) {
        new Thread(() -> {
            SleepRepository sleepRepo = ((PPApplication) context.getApplicationContext()).getSleepRepository();
            SleepEntity lastSleep = sleepRepo.getLastSleepSync();
            long now = System.currentTimeMillis();

            if (lastSleep != null && lastSleep.endTime == 0) {
                // User is sleeping, it's wake up time!
                if (now >= lastSleep.targetWakeTime - 60000) {
                    SleepNotificationHelper.showWakeUpNotification(context);
                }
            } else {
                // User is awake, show bedtime reminder
                long targetWakeTime;
                if (lastSleep != null && lastSleep.targetWakeTime > now - TimeUnit.HOURS.toMillis(12)) {
                    targetWakeTime = lastSleep.targetWakeTime;
                } else {
                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.HOUR_OF_DAY, 6);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    if (cal.getTimeInMillis() <= now) cal.add(Calendar.DAY_OF_YEAR, 1);
                    targetWakeTime = cal.getTimeInMillis();
                }

                long bedtime = targetWakeTime - TimeUnit.HOURS.toMillis(8);
                String message;
                if (now < bedtime) {
                    long diff = bedtime - now;
                    long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
                    if (minutes > 60) {
                        message = "Only " + (minutes / 60) + " hours left until bedtime.";
                    } else {
                        message = "Only " + minutes + " minutes left until bedtime.";
                    }
                    SleepNotificationHelper.showBedtimeReminder(context, message, minutes <= 30);
                } else {
                    message = "Time's up! You should be in bed by now.";
                    SleepNotificationHelper.showBedtimeReminder(context, message, true);
                }
            }
            SleepScheduler.reschedule(context);
        }).start();
    }
}
