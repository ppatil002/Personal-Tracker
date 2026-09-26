package com.example.pp.core.constants;

public class AppConstants {
    // Notification Channels
    public static final String CHANNEL_BACKUP = "BACKUP_SERVICE_CHANNEL";
    public static final String CHANNEL_WATER = "WATER_REMINDER_CHANNEL_V1";
    public static final String CHANNEL_SCREEN_TIME = "SCREEN_TIME_ALERTS";
    public static final String CHANNEL_SCREEN_TIME_SERVICE = "SCREEN_TIME_SERVICE";
    public static final String CHANNEL_SLEEP = "SLEEP_MONITOR_CHANNEL_V2";
    public static final String CHANNEL_TASK_ENGINE = "BG_SYNC_CHANNEL";

    // Notification IDs
    public static final int NOTIFY_ID_BACKUP = 5001;
    public static final int NOTIFY_ID_WATER = 5002;
    public static final int NOTIFY_ID_SCREEN_TIME = 3001;
    public static final int NOTIFY_ID_SCREEN_BREAK = 3002;
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

    // Shared Preferences
    public static final String PREF_NAME_COMMON = "PP_COMMON_PREFS";
    public static final String KEY_SCREEN_SESSION_START = "screen_session_start_time";
    public static final String KEY_SCREEN_TIME_ENABLED = "screen_time_enabled";

    // Workers
    public static final String SCREEN_WORKER = "ScreenWorker";
    public static final String SLEEP_WORKER = "SleepWorker";
    public static final String WATER_WORKER = "WaterWorker";
    public static final String QUOTES_WORKER = "QuotesWorker";
    public static final String HISTORY_WORKER = "HistoryWorker";

    public static final String[] MONTHS_OF_YEAR = new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    public static final long SCREEN_BREAK_INTERVAL_MS = 20 * 60 * 1000; // 20 minutes

    // Nutrition Targets
    public static final double TARGET_CALORIES = 2000.0;
    public static final double TARGET_CARBS = 200.0;
    public static final double TARGET_PROTEIN = 90;
    public static final double TARGET_FATS = 65.0;
    public static final double TARGET_WATER_ML = 3000.0;
    
    
    // Vocabulary Shared Status Code
    public static final int VOCABULARY_STATUS_ADDED = 1;
    public static final int VOCABULARY_STATUS_ALREADY_EXISTS = 2;
    public static final int VOCABULARY_STATUS_MEANING_ADDED = 3;
    public static final int VOCABULARY_STATUS_MEANING_SKIPPED = 4;
    public static final int VOCABULARY_STATUS_POS_ADDED = 5;
    public static final int VOCABULARY_STATUS_POS_SKIPPED = 6;
    public static final int VOCABULARY_STATUS_NO_LAST_WORD = 7;
    public static final int VOCABULARY_STATUS_WORD_LENGTH_EXCEED = 8;
}
