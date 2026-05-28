package com.example.pp.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.pp.data.local.AppDatabase;
import com.example.pp.data.local.dao.HydrationDao;
import com.example.pp.data.local.entity.HydrationIntake;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HydrationRepository {
    private final HydrationDao hydrationDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public HydrationRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.hydrationDao = db.hydrationDao();
    }

    public void addIntake(int amountMl, long timestamp) {
        executor.execute(() -> {
            hydrationDao.insert(new HydrationIntake(amountMl, timestamp));
        });
    }

    public LiveData<Integer> getTodayTotal(long startOfDay) {
        return hydrationDao.getTodayTotalMl(startOfDay);
    }

    public LiveData<Integer> getTodayCount(long startOfDay) {
        return hydrationDao.getTodayCount(startOfDay);
    }

    public LiveData<HydrationIntake> getLastIntake(long startOfDay) {
        return hydrationDao.getLastIntake(startOfDay);
    }
}
