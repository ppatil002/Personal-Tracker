package com.example.pp.features.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import com.example.pp.PPApplication;
import com.example.pp.R;
import com.example.pp.features.tracker.edit.EditTrackerActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ((PPApplication) getApplication()).getCategoryRepository().seedDatabaseIfEmpty();
        ((PPApplication) getApplication()).getTagRepository().seedTagsIfEmpty();
        ((PPApplication) getApplication()).getTransactionRepository().seedTransactionsIfEmpty();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, EditTrackerActivity.class));
            finish();
        }, 2000); // 2 seconds delay
    }
}