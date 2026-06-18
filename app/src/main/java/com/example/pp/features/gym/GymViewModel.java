package com.example.pp.features.gym;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.pp.core.task.AppExecutors;
import com.example.pp.data.local.AppDatabase;
import com.example.pp.data.local.dao.GymDao;
import com.example.pp.data.local.entity.BodyWeight;
import com.example.pp.data.local.entity.GymTracking;
import com.example.pp.utils.DateUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GymViewModel extends AndroidViewModel {
    private final GymDao gymDao;
    private final AppExecutors appExecutors;
    private final MutableLiveData<Integer> weightLimit = new MutableLiveData<>(10);
    private final MutableLiveData<Long> selectedLogDate = new MutableLiveData<>(DateUtils.getStartOfDay(System.currentTimeMillis()));
    private final LiveData<List<Long>> availableWorkoutDates;

    public GymViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getInstance(application);
        gymDao = db.gymDao();
        appExecutors = AppExecutors.getInstance();
        availableWorkoutDates = gymDao.getUniqueWorkoutDates();
    }

    public LiveData<List<Long>> getAvailableWorkoutDates() {
        return availableWorkoutDates;
    }

    public LiveData<List<BodyWeight>> getBodyWeightHistory() {
        return Transformations.switchMap(weightLimit, limit -> {
            if (limit == -1) {
                return gymDao.getAllWeights();
            } else {
                return gymDao.getLastWeights(limit);
            }
        });
    }

    public void setWeightLimit(int limit) {
        weightLimit.setValue(limit);
    }

    public LiveData<List<GymTracking>> getAllGymLogs() {
        return gymDao.getAllGymLogs();
    }

    public LiveData<List<GymTracking>> getGymLogsForSelectedPeriod() {
        return Transformations.switchMap(selectedLogDate, date -> {
            long end = date + 24 * 3600 * 1000L;
            return gymDao.getGymLogsForRange(date, end);
        });
    }

    public LiveData<Long> getSelectedLogDate() {
        return selectedLogDate;
    }

    public LiveData<String> getLogPeriodLabel() {
        return Transformations.map(selectedLogDate, date -> {
            long today = DateUtils.getStartOfDay(System.currentTimeMillis());
            if (date == today) return "Today";
            if (date == today - 24 * 3600 * 1000L) return "Yesterday";
            return new SimpleDateFormat("dd MMM", Locale.getDefault()).format(new Date(date));
        });
    }

    public void navigateLogPeriod(int direction) {
        List<Long> dates = availableWorkoutDates.getValue();
        Long current = selectedLogDate.getValue();
        if (dates == null || dates.isEmpty() || current == null) return;

        // Ensure we are working with start of day for comparison
        long currentStart = DateUtils.getStartOfDay(current);
        
        int currentIndex = -1;
        // Find closest date in list (it might not be EXACT if we just added one today)
        for (int i = 0; i < dates.size(); i++) {
            if (DateUtils.getStartOfDay(dates.get(i)) == currentStart) {
                currentIndex = i;
                break;
            }
        }

        int nextIndex = currentIndex + (direction * -1); // direction -1 (prev) means index +1 since sorted DESC
        if (nextIndex >= 0 && nextIndex < dates.size()) {
            selectedLogDate.setValue(dates.get(nextIndex));
        }
    }

    public void selectDate(long date) {
        selectedLogDate.setValue(DateUtils.getStartOfDay(date));
    }

    public void addBodyWeight(double weight, long date) {
        appExecutors.diskIO().execute(() -> {
            BodyWeight existing = gymDao.getWeightByDateSync(date);
            if (existing != null) {
                existing.weight = weight;
                gymDao.updateBodyWeight(existing);
            } else {
                gymDao.insertBodyWeight(new BodyWeight(weight, date));
            }
        });
    }

    public void addGymLog(String muscle, String exercise, String setsJson, String difficulty, long date) {
        appExecutors.diskIO().execute(() -> {
            gymDao.insertGymLog(new GymTracking(muscle, exercise, setsJson, difficulty, date));
        });
    }

    public void updateGymLog(GymTracking log) {
        appExecutors.diskIO().execute(() -> {
            gymDao.insertGymLog(log);
        });
    }

    public void deleteGymLog(GymTracking log) {
        appExecutors.diskIO().execute(() -> {
            gymDao.deleteGymLog(log);
        });
    }

    public BodyWeight getWeightForDateSync(long date) {
        return gymDao.getWeightByDateSync(date);
    }
}
