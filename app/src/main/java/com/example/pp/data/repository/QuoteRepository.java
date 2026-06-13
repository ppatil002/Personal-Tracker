package com.example.pp.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.pp.BuildConfig;
import com.example.pp.core.task.AppExecutors;
import com.example.pp.data.local.AppDatabase;
import com.example.pp.data.local.dao.QuotesDao;
import com.example.pp.data.local.entity.QuotesApiTrack;
import com.example.pp.data.remote.ApiNinjasService;
import com.example.pp.core.preferences.QuoteHistoryPrefs;
import com.example.pp.utils.DateUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class QuoteRepository {
    private static final String TAG = "QuoteRepository";
    private static final String BASE_URL = "https://api.api-ninjas.com/";

    private final ApiNinjasService apiService;
    private final QuotesDao quotesDao;
    private final QuoteHistoryPrefs prefs;
    private final AppExecutors appExecutors;
    
    private final MutableLiveData<ApiNinjasService.QuoteResponse> currentQuote = new MutableLiveData<>();

    public QuoteRepository(Context context) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        
        this.apiService = retrofit.create(ApiNinjasService.class);
        AppDatabase db = AppDatabase.getInstance(context);
        this.quotesDao = db.quotesDao();
        this.prefs = new QuoteHistoryPrefs(context);
        this.appExecutors = AppExecutors.getInstance();
        
        loadInitialData();
    }

    private void loadInitialData() {
        appExecutors.diskIO().execute(() -> {
            ApiNinjasService.QuoteResponse q = prefs.getLatestQuote();
            Log.d(TAG, "loadInitialData: Prefs latest quote: " + (q != null ? q.quote : "null"));
            if (q != null) currentQuote.postValue(q);
            checkAndFetchToday();
        });
    }

    private void checkAndFetchToday() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String lastFetch = prefs.getLastFetchDate();
        Log.d(TAG, "checkAndFetchToday: today=" + today + ", lastFetch=" + lastFetch);

        if (today.equals(lastFetch)) {
            Log.d(TAG, "Already fetched today according to Prefs.");
            return;
        }

        appExecutors.diskIO().execute(() -> {
            int count = quotesDao.getCount();
            Log.d(TAG, "checkAndFetchToday: DB count=" + count);
            if (count == 0) {
                Log.d(TAG, "Quotes Database is empty. Skipping auto-fetch (Requirement #6).");
                return;
            }

            QuotesApiTrack latestDbQuote = quotesDao.getLatestQuote();
            boolean isToday = latestDbQuote != null && DateUtils.getStartOfDay(latestDbQuote.fetchTimestamp) == DateUtils.getStartOfDay(System.currentTimeMillis());
            Log.d(TAG, "checkAndFetchToday: Latest DB quote is from today: " + isToday);

            if (isToday) {
                ApiNinjasService.QuoteResponse qr = new ApiNinjasService.QuoteResponse();
                qr.quote = latestDbQuote.quote;
                qr.author = latestDbQuote.author;
                qr.work = latestDbQuote.work;
                prefs.saveLatestQuote(qr);
                prefs.saveLastFetchDate(today);
                currentQuote.postValue(qr);
                Log.d(TAG, "Loaded Quote from DB and updated Prefs.");
            } else {
                fetchQuoteOfTheDay(today);
            }
        });
    }

    public void fetchQuoteOfTheDay(String dateKey) {
        apiService.getQuoteOfTheDay(BuildConfig.QUOTES_API_KEY).enqueue(new Callback<List<ApiNinjasService.QuoteResponse>>() {
            @Override
            public void onResponse(Call<List<ApiNinjasService.QuoteResponse>> call, Response<List<ApiNinjasService.QuoteResponse>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    ApiNinjasService.QuoteResponse quote = response.body().get(0);
                    currentQuote.postValue(quote);
                    saveQuote(quote, dateKey);
                } else {
                    try {
                        String errorMsg = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                        Log.e(TAG, "fetchQuoteOfTheDay: Error body=" + errorMsg);
                    } catch (IOException e) {
                        Log.e(TAG, "fetchQuoteOfTheDay: Could not read error body");
                    }
                }
            }

            @Override
            public void onFailure(Call<List<ApiNinjasService.QuoteResponse>> call, Throwable t) {
                Log.e(TAG, "fetchQuoteOfTheDay: Network Failure", t);
            }
        });
    }

    public void refreshQuoteManually() {
        Log.d(TAG, "refreshQuoteManually: Triggered");
        apiService.getRandomQuotes(BuildConfig.QUOTES_API_KEY, "art,writing,nature,death").enqueue(new Callback<List<ApiNinjasService.QuoteResponse>>() {
            @Override
            public void onResponse(Call<List<ApiNinjasService.QuoteResponse>> call, Response<List<ApiNinjasService.QuoteResponse>> response) {
                Log.d(TAG, "refreshQuoteManually: Response code=" + response.code());
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    ApiNinjasService.QuoteResponse quote = response.body().get(0);
                    Log.d(TAG, "refreshQuoteManually: Success. New Quote: " + quote.quote);
                    currentQuote.postValue(quote);
                    saveQuote(quote, null);
                } else {
                    Log.e(TAG, "refreshQuoteManually: Unsuccessful or empty body");
                }
            }

            @Override
            public void onFailure(Call<List<ApiNinjasService.QuoteResponse>> call, Throwable t) {
                Log.e(TAG, "refreshQuoteManually: Failed", t);
            }
        });
    }

    private void saveQuote(ApiNinjasService.QuoteResponse quote, String dateKey) {
        appExecutors.diskIO().execute(() -> {
            Log.d(TAG, "saveQuote: Saving to Prefs and DB. dateKey=" + dateKey);
            prefs.saveLatestQuote(quote);
            if (dateKey != null) prefs.saveLastFetchDate(dateKey);
            
            String cat = (quote.categories != null && !quote.categories.isEmpty()) ? quote.categories.get(0) : "";
            quotesDao.insert(new QuotesApiTrack(quote.quote, quote.author, quote.work, cat, System.currentTimeMillis()));
        });
    }

    public LiveData<ApiNinjasService.QuoteResponse> getCurrentQuote() {
        return currentQuote;
    }
}
