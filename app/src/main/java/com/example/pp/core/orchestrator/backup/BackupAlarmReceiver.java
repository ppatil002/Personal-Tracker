package com.example.pp.core.orchestrator.backup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BackupAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "BackupAlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String taskId = intent.getStringExtra("TASK_ID");
        Log.i(TAG, "Alarm triggered for backup task: " + taskId);

        Intent serviceIntent = new Intent(context, BackupForegroundService.class);
        serviceIntent.putExtra("TASK_ID", taskId);
        
        // Start Foreground Service
        context.startForegroundService(serviceIntent);
    }
}
