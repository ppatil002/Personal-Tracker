package com.example.pp.features.dashboard;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.pp.PPApplication;
import com.example.pp.core.orchestrator.water.WaterIntakeScheduler;
import com.example.pp.data.local.dao.ScreenTimeDao;
import com.example.pp.data.local.entity.HydrationIntake;
import com.example.pp.data.local.entity.NutritionIntake;
import com.example.pp.data.local.entity.SleepEntity;
import com.example.pp.data.local.model.NutritionSummary;
import com.example.pp.data.repository.HydrationRepository;
import com.example.pp.data.repository.NutritionRepository;
import com.example.pp.data.repository.ScreenTimeRepository;
import com.example.pp.data.repository.SleepRepository;
import com.example.pp.utils.DateUtils;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import java.util.List;

public class DashboardViewModel extends AndroidViewModel {
    private final HydrationRepository hydrationRepository;
    private final NutritionRepository nutritionRepository;
    private final ScreenTimeRepository screenTimeRepository;
    private final SleepRepository sleepRepository;
    private final MutableLiveData<String> userName = new MutableLiveData<>("Champion");

    public DashboardViewModel(@NonNull Application application) {
        super(application);
        hydrationRepository = ((PPApplication) application).getHydrationRepository();
        nutritionRepository = ((PPApplication) application).getNutritionRepository();
        screenTimeRepository = ((PPApplication) application).getScreenTimeRepository();
        sleepRepository = ((PPApplication) application).getSleepRepository();
        loadUserName();
    }

    private void loadUserName() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getApplication());
        if (account != null && account.getDisplayName() != null) {
            userName.setValue(account.getDisplayName());
        }
    }

    public LiveData<String> getUserName() {
        return userName;
    }

    public LiveData<Integer> getTodayWaterTotal() {
        return hydrationRepository.getTodayTotal(getStartOfDay());
    }

    public LiveData<Integer> getTodayWaterCount() {
        return hydrationRepository.getTodayCount(getStartOfDay());
    }

    public LiveData<HydrationIntake> getLastWaterIntake() {
        return hydrationRepository.getLastIntake(getStartOfDay());
    }

    public LiveData<NutritionSummary> getTodayNutritionSummary() {
        return nutritionRepository.getTodaySummary(getStartOfDay());
    }

    public LiveData<NutritionIntake> getLastNutritionIntake() {
        return nutritionRepository.getLastIntake(getStartOfDay());
    }

    public void addWater(int ml, long timestamp) {
        WaterIntakeScheduler.logIntakeAndReschedule(getApplication(), ml, timestamp);
    }

    public void addNutrition(String food, double cal, double carbs, double protein, double fats, long timestamp) {
        nutritionRepository.addIntake(food, cal, carbs, protein, fats, timestamp);
    }

    public LiveData<List<ScreenTimeDao.AppUsageSummary>> getTodayScreenTime() {
        return screenTimeRepository.getTodayAppUsage(getStartOfDay());
    }

    public LiveData<SleepEntity> getLastSleep() {
        return sleepRepository.getLastSleep();
    }

    public void updateTargetWakeTime(long targetWakeTime) {
        sleepRepository.updateTargetWakeTime(targetWakeTime);
    }

    public void saveSleepEntry(long start, long end, long target, long date) {
        sleepRepository.saveManualSleepEntry(start, end, target, date);
    }

    private long getStartOfDay() {
        return DateUtils.getStartOfDay(System.currentTimeMillis());
    }
}
