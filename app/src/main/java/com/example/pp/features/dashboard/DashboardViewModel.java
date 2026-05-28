package com.example.pp.features.dashboard;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.pp.data.local.model.NutritionSummary;
import com.example.pp.data.local.entity.HydrationIntake;
import com.example.pp.data.repository.HydrationRepository;
import com.example.pp.data.repository.NutritionRepository;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import java.time.LocalDate;
import java.time.ZoneId;

public class DashboardViewModel extends AndroidViewModel {
    private final HydrationRepository hydrationRepository;
    private final NutritionRepository nutritionRepository;
    private final MutableLiveData<String> userName = new MutableLiveData<>("Champion");

    public DashboardViewModel(@NonNull Application application) {
        super(application);
        hydrationRepository = new HydrationRepository(application);
        nutritionRepository = new NutritionRepository(application);
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

    public void addWater(int ml, long timestamp) {
        hydrationRepository.addIntake(ml, timestamp);
    }

    public void addNutrition(String food, double cal, double carbs, double protein, double fats, long timestamp) {
        nutritionRepository.addIntake(food, cal, carbs, protein, fats, timestamp);
    }

    private long getStartOfDay() {
        return LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
