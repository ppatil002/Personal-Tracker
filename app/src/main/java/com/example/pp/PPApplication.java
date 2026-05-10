package com.example.pp;

import android.app.Application;
import com.jakewharton.threetenabp.AndroidThreeTen;

public class PPApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AndroidThreeTen.init(this);
    }
}