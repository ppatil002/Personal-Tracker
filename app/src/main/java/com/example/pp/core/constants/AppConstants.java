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
    public static final int RC_WATER_ALARM = 8001;

    // Intent Actions
    public static final String ACTION_SLEEP_ALARM = "com.example.pp.ACTION_SLEEP_ALARM";
}
