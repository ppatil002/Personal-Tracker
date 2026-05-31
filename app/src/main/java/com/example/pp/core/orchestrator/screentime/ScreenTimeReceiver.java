package com.example.pp.core.orchestrator.screentime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ScreenTimeReceiver extends BroadcastReceiver {
    private static final String TAG = "ScreenTimeReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Break Alarm Fired");
        ScreenTimeNotificationHelper.showBreakNotification(context);
    }
}
