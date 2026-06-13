package com.example.pp.features.dashboard;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.pp.PPApplication;
import com.example.pp.core.orchestrator.water.WaterIntakeScheduler;
import com.example.pp.core.task.AppExecutors;
import com.example.pp.data.local.dao.ScreenTimeDao;
import com.example.pp.data.local.entity.HydrationIntake;
import com.example.pp.data.local.entity.NutrientEntity;
import com.example.pp.data.local.entity.NutritionIntake;
import com.example.pp.data.local.entity.SleepEntity;
import com.example.pp.data.local.model.NutritionSummary;
import com.example.pp.data.repository.HydrationRepository;
import com.example.pp.data.repository.NutrientRepository;
import com.example.pp.data.repository.NutritionRepository;
import com.example.pp.data.repository.QuoteRepository;
import com.example.pp.data.repository.HistoryRepository;
import com.example.pp.data.repository.ScreenTimeRepository;
import com.example.pp.data.repository.SleepRepository;
import com.example.pp.data.remote.ApiNinjasService;
import com.example.pp.utils.DateUtils;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import java.util.List;

public class DashboardViewModel extends AndroidViewModel {
    private final HydrationRepository hydrationRepository;
    private final NutritionRepository nutritionRepository;
    private final NutrientRepository nutrientRepository;
    private final ScreenTimeRepository screenTimeRepository;
    private final SleepRepository sleepRepository;
    private final QuoteRepository quoteRepository;
    private final HistoryRepository historyRepository;
    private final MutableLiveData<String> userName = new MutableLiveData<>("Champion");

    public DashboardViewModel(@NonNull Application application) {
        super(application);
        hydrationRepository = ((PPApplication) application).getHydrationRepository();
        nutritionRepository = ((PPApplication) application).getNutritionRepository();
        nutrientRepository = ((PPApplication) application).getNutrientRepository();
        screenTimeRepository = ((PPApplication) application).getScreenTimeRepository();
        sleepRepository = ((PPApplication) application).getSleepRepository();
        quoteRepository = ((PPApplication) application).getQuoteRepository();
        historyRepository = ((PPApplication) application).getHistoryRepository();
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
        return hydrationRepository.getTodayTotalML(getStartOfDay());
    }

    public LiveData<Integer> getTodayWaterCount() {
        return hydrationRepository.getIntakeCountSince(getStartOfDay());
    }

    public LiveData<HydrationIntake> getLastWaterIntakeForCurrentDate() {
        return hydrationRepository.getLastIntakeForDate(getStartOfDay());
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

    public void addNutrition(Long nutrientId, double quantity, long timestamp, String tags) {
        nutritionRepository.addIntake(nutrientId, quantity, timestamp, tags);
    }

    public void addNewNutrient(String foodName, OnNutrientAddedCallback callback) {
        long now = System.currentTimeMillis();
        NutrientEntity newNutrient = new NutrientEntity(foodName, 0, 0, 0, 0, 100, "gram", now, now);
        nutrientRepository.addNutrient(newNutrient, () -> {
            nutrientRepository.getAllNutrients(nutrients -> {
                for (NutrientEntity n : nutrients) {
                    if (n.getFoodItem().equalsIgnoreCase(foodName)) {
                        callback.onAdded(n.getNutrientId());
                        return;
                    }
                }
            });
        });
    }

    public interface OnNutrientAddedCallback {
        void onAdded(long nutrientId);
    }

    public LiveData<List<ScreenTimeDao.AppUsageSummary>> getTodayScreenTime() {
        return screenTimeRepository.getTodayAppUsage(getStartOfDay());
    }

    public LiveData<SleepEntity> getLastSleep() {
        return sleepRepository.getLastSleep();
    }

    public void getAllNutrients(NutrientRepository.OnNutrientsLoadedCallback callback) {
        nutrientRepository.getAllNutrients(callback);
    }

    public void getLastSleepSync(OnSleepLoadedCallback callback) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            SleepEntity sleep = sleepRepository.getLastSleepSync();
            AppExecutors.getInstance().mainThread().execute(() -> callback.onLoaded(sleep));
        });
    }

    public void getSleepByDateSync(long date, OnSleepLoadedCallback callback) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            SleepEntity sleep = sleepRepository.getSleepByDateSync(date);
            AppExecutors.getInstance().mainThread().execute(() -> callback.onLoaded(sleep));
        });
    }

    public interface OnSleepLoadedCallback {
        void onLoaded(SleepEntity sleep);
    }


    public void saveSleepEntry(long start, long end, long target, long date, String reason) {
        sleepRepository.saveManualSleepEntry(start, end, target, date, reason);
    }

    public LiveData<ApiNinjasService.QuoteResponse> getCurrentQuote() {
        return quoteRepository.getCurrentQuote();
    }

    public LiveData<ApiNinjasService.HistoryResponse> getCurrentHistory() {
        return historyRepository.getCurrentHistory();
    }

    public void refreshQuote() {
        quoteRepository.refreshQuoteManually();
    }

    private long getStartOfDay() {
        return DateUtils.getStartOfDay(System.currentTimeMillis());
    }
}
