package com.example.pp.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.pp.data.local.AppDatabase;
import com.example.pp.data.local.dao.HydrationDao;
import com.example.pp.data.local.entity.HydrationIntake;

import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HydrationRepository {
    private final HydrationDao hydrationDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public HydrationRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.hydrationDao = db.hydrationDao();
    }

    public void addIntakeSync(int amountMl, long timestamp) {
        executor.execute(() -> {
            hydrationDao.insert(new HydrationIntake(amountMl, timestamp));
        });
    }

    public void addIntake(int amountMl, long timestamp) {
        executor.execute(() -> {
            hydrationDao.insert(new HydrationIntake(amountMl, timestamp));
        });
    }

    public HydrationIntake findLatestIntake() {
        return hydrationDao.findLatestIntake();
    }

    public LiveData<Integer> getTodayTotalML(long startOfDay) {
        return hydrationDao.getTotalMLSince(startOfDay);
    }

    public LiveData<Integer> getIntakeCountSince(long startOfDay) {
        return hydrationDao.getIntakeCountSince(startOfDay);
    }

    public LiveData<HydrationIntake> getLastIntakeForDate(long startOfDay) {
        return hydrationDao.getLatestIntakeForDate(startOfDay);
    }
}
