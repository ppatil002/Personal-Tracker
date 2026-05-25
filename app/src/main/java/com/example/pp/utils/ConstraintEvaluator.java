package com.example.pp.utils;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.util.Log;

import com.example.pp.data.local.entity.TaskDefinition;

import java.util.Calendar;

public class ConstraintEvaluator {

    public static boolean isInternetAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || 
                                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
    }

    public static int getBatteryLevel(Context context) {
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, intentFilter);
        if (batteryStatus == null) return -1;
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        return (int) ((level / (float) scale) * 100);
    }

    public static boolean isAfterHour(int afterHour) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        return hour >= afterHour;
    }

    public static boolean checkConstraints(Context context, TaskDefinition definition) {
        Log.d("ConstraintEvaluator", "Checking constraints for: " + definition.getTaskType());

        if (definition.isRequiresInternet()) {
            boolean internet = isInternetAvailable(context);
            android.util.Log.d("ConstraintEvaluator", "  - Internet Required: true, Available: " + internet);
            if (!internet) return false;
        }

        int battery = getBatteryLevel(context);
        android.util.Log.d("ConstraintEvaluator", "  - Minimum Battery: " + definition.getMinimumBattery() + "%, Current: " + battery + "%");
        if (battery < definition.getMinimumBattery()) {
            return false;
        }

        return true;
    }
}
