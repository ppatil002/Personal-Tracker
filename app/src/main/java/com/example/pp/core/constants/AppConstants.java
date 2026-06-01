package com.example.pp.core.constants;

public class AppConstants {
    // Notification Channels
    public static final String CHANNEL_BACKUP = "BACKUP_SERVICE_CHANNEL";
    public static final String CHANNEL_WATER = "WATER_REMINDER_CHANNEL";
    public static final String CHANNEL_SCREEN_TIME = "SCREEN_TIME_ALERTS";
    public static final String CHANNEL_SLEEP = "SLEEP_MONITOR_CHANNEL";
    public static final String CHANNEL_TASK_ENGINE = "BG_SYNC_CHANNEL";

    // Notification IDs
    public static final int NOTIFY_ID_BACKUP = 5001;
    public static final int NOTIFY_ID_WATER = 5002;
    public static final int NOTIFY_ID_SCREEN_TIME = 3001;
    public static final int NOTIFY_ID_SLEEP = 9001;
    public static final int NOTIFY_ID_TASK_ENGINE = 2001;

    // Alarm Request Codes
    public static final int RC_SLEEP_ALARM = 9002;
    public static final int RC_SLEEP_REMINDER_2H = 9003;
    public static final int RC_SLEEP_REMINDER_1H = 9004;
    public static final int RC_SLEEP_REMINDER_30M = 9005;
    public static final int RC_SLEEP_REMINDER_10M = 9006;
    public static final int RC_SLEEP_TARGET = 9007;
    public static final int RC_SLEEP_WAKEUP = 9008;
    public static final int RC_SLEEP_NUDGE = 9009;

    public static final int RC_WATER_ALARM = 8001;

    // Intent Actions
    public static final String ACTION_SLEEP_ALARM = "com.example.pp.ACTION_SLEEP_ALARM";
    public static final String ACTION_WATER_ALARM = "com.example.pp.ACTION_WATER_ALARM";
    
    // Target Time
    public static final int SLEEP_TARGET_HOUR = 8;
    
}
