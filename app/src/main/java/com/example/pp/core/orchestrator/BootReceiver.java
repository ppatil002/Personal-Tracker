package com.example.pp.core.orchestrator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.pp.core.task.AppExecutors;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, "Received system broadcast action: " + action);

        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || 
            Intent.ACTION_MY_PACKAGE_REPLACED.equals(action) ||
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED".equals(action)) {
            
            Log.i(TAG, "Re-registering native tasks after system start...");
            
            final PendingResult pendingResult = goAsync();
            AppExecutors.getInstance().diskIO().execute(() -> {
                try {
                    SystemTaskScheduler.scheduleAllTasksSync(context);
                    Log.i(TAG, "Native tasks re-registration complete.");
                } catch (Exception e) {
                    Log.e(TAG, "Error during boot task scheduling", e);
                } finally {
                    pendingResult.finish();
                }
            });
        }
    }
}
