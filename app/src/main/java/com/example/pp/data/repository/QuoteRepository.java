package com.example.pp.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.pp.BuildConfig;
import com.example.pp.core.task.AppExecutors;
import com.example.pp.data.local.AppDatabase;
import com.example.pp.data.local.dao.QuotesDao;
import com.example.pp.data.local.entity.QuotesApiTrack;
import com.example.pp.data.local.entity.SavedQuote;
import com.example.pp.data.remote.ApiNinjasService;
import com.example.pp.core.preferences.QuoteHistoryPrefs;
import com.example.pp.utils.DateUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        if (hour < 6) {
            Log.d(TAG, "checkAndFetchToday: Too early (" + hour + " AM). Skipping fetch until after 6 AM.");
            return;
        }

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
                    saveQuoteToHistory(quote, dateKey);
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
                    saveQuoteToHistory(quote, null);
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

    private void saveQuoteToHistory(ApiNinjasService.QuoteResponse quote, String dateKey) {
        appExecutors.diskIO().execute(() -> {
            Log.d(TAG, "saveQuoteToHistory: Saving to Prefs and DB. dateKey=" + dateKey);
            prefs.saveLatestQuote(quote);
            if (dateKey != null) prefs.saveLastFetchDate(dateKey);
            
            String sortedCategories = "";
            if (quote.categories != null && !quote.categories.isEmpty()) {
                List<String> cats = new ArrayList<>(quote.categories);
                Collections.sort(cats);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < cats.size(); i++) {
                    sb.append(cats.get(i).trim());
                    if (i < cats.size() - 1) sb.append(", ");
                }
                sortedCategories = sb.toString();
            }
            quotesDao.insert(new QuotesApiTrack(quote.quote, quote.author, quote.work, sortedCategories, System.currentTimeMillis()));
        });
    }

    public LiveData<ApiNinjasService.QuoteResponse> getCurrentQuote() {
        return currentQuote;
    }

    // --- Saved Quotes Logic ---

    public void saveQuote(String quote, String author, List<String> categories, String notes) {
        appExecutors.diskIO().execute(() -> {
            if (quotesDao.existsByQuote(quote)) {
                Log.d(TAG, "saveQuote: Quote already exists. Skipping save.");
                return;
            }
            performSave(quote, author, categories, notes);
        });
    }

    private void performSave(String quote, String author, List<String> categories, String notes) {
        long now = System.currentTimeMillis();
        List<SavedQuote> toSave = new ArrayList<>();
        if (categories == null || categories.isEmpty()) {
            toSave.add(new SavedQuote(quote, author, "", notes, now));
        } else {
            for (String cat : categories) {
                toSave.add(new SavedQuote(quote, author, cat.trim(), notes, now));
            }
        }
        quotesDao.insertSavedQuotesBatch(toSave);
    }

    public void deleteQuote(String quote) {
        appExecutors.diskIO().execute(() -> quotesDao.deleteByQuote(quote));
    }

    public void updateQuote(String oldQuote, String newQuote, String author, List<String> categories, String notes) {
        appExecutors.diskIO().execute(() -> {
            quotesDao.deleteByQuote(oldQuote);
            performSave(newQuote, author, categories, notes);
        });
    }

    public LiveData<Boolean> isQuoteSaved(String quote) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        appExecutors.diskIO().execute(() -> {
            result.postValue(quotesDao.existsByQuote(quote));
        });
        return result;
    }

    public LiveData<List<String>> getAllDistinctCategories() {
        MediatorLiveData<List<String>> mediator = new MediatorLiveData<>();
        LiveData<List<String>> savedCats = quotesDao.getSavedDistinctCategories();
        LiveData<List<String>> apiCats = quotesDao.getApiDistinctCategories();

        mediator.addSource(savedCats, saved -> {
            List<String> api = apiCats.getValue();
            mediator.setValue(combineCategories(saved, api));
        });

        mediator.addSource(apiCats, api -> {
            List<String> saved = savedCats.getValue();
            mediator.setValue(combineCategories(saved, api));
        });

        return mediator;
    }

    private List<String> combineCategories(List<String> saved, List<String> api) {
        Set<String> set = new HashSet<>();
        if (saved != null) set.addAll(saved);
        if (api != null) {
            for (String s : api) {
                if (s == null) continue;
                for (String part : s.split(",")) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) set.add(trimmed);
                }
            }
        }
        List<String> result = new ArrayList<>(set);
        Collections.sort(result);
        return result;
    }

    public LiveData<List<SavedQuote>> getSavedQuotes(int page, int pageSize, List<String> categoryFilters) {
        int offset = (page - 1) * pageSize;
        if (categoryFilters == null || categoryFilters.isEmpty()) {
            return quotesDao.getSavedQuotesPaged(pageSize, offset);
        } else {
            // Room doesn't handle List in @Query directly for paged results in this specific way easily without custom query building
            // But we can simplify: if only one category is selected, use the existing DAO method.
            // For multiple, we'll need a new DAO method or filter in memory (less ideal for pagination but simple)
            if (categoryFilters.size() == 1) {
                return quotesDao.getSavedQuotesByCategoryPaged(categoryFilters.get(0), pageSize, offset);
            }
            // For multiple categories, we use a custom IN query
            return quotesDao.getSavedQuotesByMultipleCategoriesPaged(categoryFilters, pageSize, offset);
        }
    }

    public LiveData<Integer> getSavedQuotesCount(List<String> categoryFilters) {
        if (categoryFilters == null || categoryFilters.isEmpty()) {
            return quotesDao.getSavedQuotesCount();
        } else {
            if (categoryFilters.size() == 1) {
                return quotesDao.getSavedQuotesCountByCategory(categoryFilters.get(0));
            }
            return quotesDao.getSavedQuotesCountByMultipleCategories(categoryFilters);
        }
    }
}
