package com.example.pp.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.pp.BuildConfig;
import com.example.pp.PPApplication;
import com.example.pp.core.preferences.QuoteHistoryPrefs;
import com.example.pp.core.task.AppExecutors;
import com.example.pp.data.local.AppDatabase;
import com.example.pp.data.local.dao.HistoryDao;
import com.example.pp.data.local.entity.BackgroundJobLog;
import com.example.pp.data.local.entity.HistoryApiTrack;
import com.example.pp.data.remote.ApiNinjasService;
import com.example.pp.utils.DateUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class HistoryRepository {
    private static final String TAG = "HistoryRepository";
    private static final String BASE_URL = "https://api.api-ninjas.com/";
    
    private final ApiNinjasService apiService;
    private final HistoryDao historyDao;
    private final BackgroundJobLogRepository logRepository;
    private final QuoteHistoryPrefs prefs;
    private final AppExecutors appExecutors;
    
    private final MutableLiveData<ApiNinjasService.HistoryResponse> currentHistory = new MutableLiveData<>();

    public HistoryRepository(Context context) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        
        this.apiService = retrofit.create(ApiNinjasService.class);
        AppDatabase db = AppDatabase.getInstance(context);
        this.historyDao = db.historyDao();
        this.logRepository = ((PPApplication) context.getApplicationContext()).getBackgroundJobLogRepository();
        this.prefs = new QuoteHistoryPrefs(context);
        this.appExecutors = AppExecutors.getInstance();
        
        loadInitialData();
    }

    private void loadInitialData() {
        appExecutors.diskIO().execute(() -> {
            ApiNinjasService.HistoryResponse h = prefs.getLatestHistory();
            if (h != null) currentHistory.postValue(h);
            checkAndFetchToday();
        });
    }

    private void checkAndFetchToday() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        if (hour < 6) {
            Log.d(TAG, "checkAndFetchToday: Too early (" + hour + " AM). Skipping fetch until after 6 AM.");
            return;
        }

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String lastFetch = prefs.getLastFetchDate();
        
        if (today.equals(lastFetch)) {
            Log.d(TAG, "Already fetched today.");
            return;
        }

        appExecutors.diskIO().execute(() -> {
            int count = historyDao.getCount();
            if (count == 0) {
                Log.d(TAG, "History Database is empty. Skipping auto-fetch (Source of Truth check).");
                return;
            }

            HistoryApiTrack latestDbHistory = historyDao.getHistoryByDate(today);
            if (latestDbHistory != null && DateUtils.getStartOfDay(latestDbHistory.fetchTimestamp) == DateUtils.getStartOfDay(System.currentTimeMillis())) {
                ApiNinjasService.HistoryResponse hr = new ApiNinjasService.HistoryResponse();
                hr.event = latestDbHistory.history;
                prefs.saveLatestHistory(hr);
                prefs.saveLastFetchDate(today);
                currentHistory.postValue(hr);
                Log.d(TAG, "Loaded History from DB.");
            } else {
                fetchHistoryOfDay(today);
            }
        });
    }

    public void fetchHistoryOfDay(String dateKey) {
        String jobId = "history_" + dateKey;
        logBackgroundJob(jobId, "HistoryFetchWorker", "RUNNING", "Fetching History", "Initiating API call to Ninja History");

        apiService.getDayInHistory(BuildConfig.QUOTES_API_KEY).enqueue(new Callback<List<ApiNinjasService.HistoryResponse>>() {
            @Override
            public void onResponse(Call<List<ApiNinjasService.HistoryResponse>> call, Response<List<ApiNinjasService.HistoryResponse>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    ApiNinjasService.HistoryResponse history = response.body().get(0);
                    currentHistory.postValue(history);
                    saveHistory(history, dateKey);
                    logBackgroundJob(jobId, "HistoryFetchWorker", "SUCCESS", "History Refreshed", "Successfully fetched history");
                } else {
                    String errorMsg = "Unknown error";
                    try {
                        if (response.errorBody() != null) errorMsg = response.errorBody().string();
                    } catch (IOException ignored) {}
                    Log.e(TAG, "fetchHistoryOfDay: Error body=" + errorMsg);
                    logBackgroundJob(jobId, "HistoryFetchWorker", "FAILED", "History Error", "API Error: " + errorMsg);
                }
            }

            @Override
            public void onFailure(Call<List<ApiNinjasService.HistoryResponse>> call, Throwable t) {
                Log.e(TAG, "Failed to fetch history", t);
                logBackgroundJob(jobId, "HistoryFetchWorker", "FAILED", "History Network Failure", t.getMessage());
            }
        });
    }

    private void logBackgroundJob(String jobId, String worker, String status, String step, String msg) {
        appExecutors.diskIO().execute(() -> {
            logRepository.mergeDuplicateDailyWorkerLog(new BackgroundJobLog(jobId, worker, status, step, msg, System.currentTimeMillis()));
        });
    }

    private void saveHistory(ApiNinjasService.HistoryResponse history, String dateKey) {
        appExecutors.diskIO().execute(() -> {
            prefs.saveLatestHistory(history);
            if (dateKey != null) prefs.saveLastFetchDate(dateKey);
            
            historyDao.insert(new HistoryApiTrack(history.event, dateKey, System.currentTimeMillis()));
        });
    }

    public LiveData<ApiNinjasService.HistoryResponse> getCurrentHistory() {
        return currentHistory;
    }
}
