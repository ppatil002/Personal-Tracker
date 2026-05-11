package com.example.pp.features.analytics;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.pp.PPApplication;
import com.example.pp.core.task.AppExecutors;
import com.example.pp.data.local.entity.CategoryEntity;
import com.example.pp.data.local.entity.TransactionEntity;
import com.example.pp.data.repository.CategoryRepository;
import com.example.pp.data.repository.TransactionRepository;
import com.example.pp.domain.model.CategoryTreeNode;
import com.example.pp.features.analytics.model.AnalyticsSummary;
import com.example.pp.features.analytics.model.CategoryExpense;
import com.example.pp.features.analytics.model.DailyExpense;
import com.example.pp.features.analytics.model.MonthlyExpense;
import com.example.pp.features.analytics.model.PeriodType;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalyticsViewModel extends AndroidViewModel {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final AppExecutors executors;

    private final MutableLiveData<PeriodType> periodType = new MutableLiveData<>(PeriodType.MONTH);
    private final MutableLiveData<Integer> selectedYear = new MutableLiveData<>();
    private final MutableLiveData<Integer> selectedMonth = new MutableLiveData<>();
    private final MutableLiveData<AnalyticsSummary> analyticsSummary = new MutableLiveData<>();
    private final MutableLiveData<List<CategoryExpense>> categoryExpenses = new MutableLiveData<>();
    private final MutableLiveData<List<DailyExpense>> dailyTrend = new MutableLiveData<>();
    private final MutableLiveData<String> periodLabel = new MutableLiveData<>();
    private final MutableLiveData<List<CategoryExpense>> drillDownExpenses = new MutableLiveData<>();
    private final MutableLiveData<String> drillDownTitle = new MutableLiveData<>();

    public AnalyticsViewModel(@NonNull Application application) {
        super(application);
        PPApplication app = (PPApplication) application;
        this.transactionRepository = app.getTransactionRepository();
        this.categoryRepository = app.getCategoryRepository();
        this.executors = AppExecutors.getInstance();

        Calendar now = Calendar.getInstance();
        selectedYear.setValue(now.get(Calendar.YEAR));
        selectedMonth.setValue(now.get(Calendar.MONTH));
        updatePeriodLabel();
    }

    public LiveData<PeriodType> getPeriodType() { return periodType; }
    public LiveData<Integer> getSelectedYear() { return selectedYear; }
    public LiveData<Integer> getSelectedMonth() { return selectedMonth; }
    public LiveData<AnalyticsSummary> getAnalyticsSummary() { return analyticsSummary; }
    public LiveData<List<CategoryExpense>> getCategoryExpenses() { return categoryExpenses; }
    public LiveData<List<DailyExpense>> getDailyTrend() { return dailyTrend; }
    public LiveData<String> getPeriodLabel() { return periodLabel; }
    public LiveData<List<CategoryExpense>> getDrillDownExpenses() { return drillDownExpenses; }
    public LiveData<String> getDrillDownTitle() { return drillDownTitle; }

    public void setPeriodType(PeriodType type) {
        periodType.setValue(type);
        updatePeriodLabel();
        refreshAnalytics();
    }

    public void setSelectedYear(int year) {
        selectedYear.setValue(year);
        updatePeriodLabel();
        refreshAnalytics();
    }

    public void setSelectedMonth(int year, int month) {
        selectedYear.setValue(year);
        selectedMonth.setValue(month);
        updatePeriodLabel();
        refreshAnalytics();
    }

    public void refreshAnalytics() {
        PeriodType type = periodType.getValue();
        Integer year = selectedYear.getValue();
        Integer month = selectedMonth.getValue();
        if (type == null || year == null || month == null) return;

        long[] range = calculateDateRange(type, year, month);
        long start = range[0];
        long end = range[1];

        executors.diskIO().execute(() -> {
            List<TransactionEntity> transactions = transactionRepository.getTransactionsBetween(start, end);
            int count = transactionRepository.getTransactionCountBetween(start, end);
            double total = transactionRepository.getTotalAmountBetween(start, end);

            List<CategoryTreeNode> categoryTree = categoryRepository.getCategoryTree().getValue();
            if (categoryTree == null) {
                categoryTree = fetchCategoryTreeSync();
            }

            Map<String, CategoryEntity> allCategories = flattenCategories(categoryTree);
            Map<String, String> categoryToRoot = buildCategoryToRootMap(categoryTree);

            // Aggregate by root category
            Map<String, Double> rootSums = new HashMap<>();
            for (TransactionEntity t : transactions) {
                String rootId = categoryToRoot.get(t.getCategoryId());
                if (rootId == null) rootId = t.getCategoryId();
                rootSums.put(rootId, rootSums.getOrDefault(rootId, 0.0) + t.getAmount());
            }

            // Build CategoryExpense list
            List<CategoryExpense> catExpenses = new ArrayList<>();
            double highestAmount = 0;
            String highestName = "";
            String highestColour = "";

            for (Map.Entry<String, Double> entry : rootSums.entrySet()) {
                CategoryEntity cat = allCategories.get(entry.getKey());
                if (cat == null) continue;
                double amt = entry.getValue();
                double pct = total > 0 ? (amt / total) * 100 : 0;
                catExpenses.add(new CategoryExpense(
                        cat.getCategoryId(), cat.getParentCategoryId(), cat.getName(),
                        cat.getIcon(), cat.getColour(), amt, pct
                ));
                if (amt > highestAmount) {
                    highestAmount = amt;
                    highestName = cat.getName();
                    highestColour = cat.getColour();
                }
            }

            // Sort by amount desc
            catExpenses.sort((a, b) -> Double.compare(b.getAmount(), a.getAmount()));

            // Summary
            int daysInPeriod = calculateDaysInPeriod(type, year, month);
            double avgPerDay = daysInPeriod > 0 ? total / daysInPeriod : 0;
            AnalyticsSummary summary = new AnalyticsSummary(total, count, avgPerDay, highestName, highestColour, highestAmount);

            // Trend
            List<DailyExpense> trend;
            if (type == PeriodType.YEAR) {
                trend = buildMonthlyTrend(start, end);
            } else {
                trend = buildDailyTrend(transactions, start, end);
            }

            executors.mainThread().execute(() -> {
                analyticsSummary.setValue(summary);
                categoryExpenses.setValue(catExpenses);
                dailyTrend.setValue(trend);
            });
        });
    }

    private List<CategoryTreeNode> fetchCategoryTreeSync() {
        List<CategoryEntity> entities = com.example.pp.data.local.AppDatabase.getInstance(getApplication())
                .categoryDao().getAllCategories();
        return com.example.pp.domain.mapper.CategoryMapper.buildTree(entities);
    }

    private Map<String, CategoryEntity> flattenCategories(List<CategoryTreeNode> tree) {
        Map<String, CategoryEntity> map = new HashMap<>();
        if (tree == null) return map;
        for (CategoryTreeNode node : tree) {
            map.put(node.getCategoryId(), node.getCategory());
            for (CategoryTreeNode child : node.getChildren()) {
                map.put(child.getCategoryId(), child.getCategory());
            }
        }
        return map;
    }

    private Map<String, String> buildCategoryToRootMap(List<CategoryTreeNode> tree) {
        Map<String, String> map = new HashMap<>();
        if (tree == null) return map;
        for (CategoryTreeNode root : tree) {
            map.put(root.getCategoryId(), root.getCategoryId());
            for (CategoryTreeNode child : root.getChildren()) {
                map.put(child.getCategoryId(), root.getCategoryId());
            }
        }
        return map;
    }

    private long[] calculateDateRange(PeriodType type, int year, int month) {
        Calendar startCal = Calendar.getInstance();
        Calendar endCal = Calendar.getInstance();
        if (type == PeriodType.MONTH) {
            startCal.set(year, month, 1, 0, 0, 0);
            startCal.set(Calendar.MILLISECOND, 0);
            endCal.set(year, month, 1, 0, 0, 0);
            endCal.set(Calendar.MILLISECOND, 0);
            endCal.add(Calendar.MONTH, 1);
        } else {
            startCal.set(year, Calendar.JANUARY, 1, 0, 0, 0);
            startCal.set(Calendar.MILLISECOND, 0);
            endCal.set(year + 1, Calendar.JANUARY, 1, 0, 0, 0);
            endCal.set(Calendar.MILLISECOND, 0);
        }
        return new long[]{startCal.getTimeInMillis(), endCal.getTimeInMillis()};
    }

    private int calculateDaysInPeriod(PeriodType type, int year, int month) {
        if (type == PeriodType.MONTH) {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, 1);
            return cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        } else {
            Calendar cal = Calendar.getInstance();
            cal.set(year, Calendar.JANUARY, 1);
            return cal.getActualMaximum(Calendar.DAY_OF_YEAR);
        }
    }

    private List<DailyExpense> buildMonthlyTrend(long start, long end) {
        List<MonthlyExpense> monthlyData = transactionRepository.getMonthlyExpensesBetween(start, end);
        Map<Integer, Double> monthlyMap = new HashMap<>();
        for (MonthlyExpense me : monthlyData) {
            monthlyMap.put(me.getMonth(), me.getTotalAmount());
        }

        List<DailyExpense> trend = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(start);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        for (int i = 0; i < 12; i++) {
            int monthIdx = cal.get(Calendar.MONTH) + 1; // 1-based (Jan=1) to match strftime %m
            double amt = monthlyMap.getOrDefault(monthIdx, 0.0);
            trend.add(new DailyExpense(cal.getTimeInMillis(), amt));
            cal.add(Calendar.MONTH, 1);
        }
        return trend;
    }

    private List<DailyExpense> buildDailyTrend(List<TransactionEntity> transactions, long start, long end) {
        Map<Long, Double> dailyMap = new HashMap<>();
        for (TransactionEntity t : transactions) {
            double amount = Math.max(0, t.getAmount()); // Replace negative with 0
            long day = floorToDay(t.getTransactionDate());
            dailyMap.put(day, dailyMap.getOrDefault(day, 0.0) + amount);
        }

        List<DailyExpense> trend = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(start);
        long currentDay = floorToDay(cal.getTimeInMillis());
        long endDay = floorToDay(end - 1); // end is exclusive

        while (currentDay <= endDay) {
            double amt = dailyMap.getOrDefault(currentDay, 0.0);
            trend.add(new DailyExpense(currentDay, amt));
            currentDay += 86400000L;
        }
        return trend;
    }

    private long floorToDay(long timestamp) {
        return timestamp - (timestamp % 86400000L);
    }

    public void loadDrillDown(String rootCategoryId) {
        PeriodType type = periodType.getValue();
        Integer year = selectedYear.getValue();
        Integer month = selectedMonth.getValue();
        if (type == null || year == null || month == null) return;

        long[] range = calculateDateRange(type, year, month);
        long start = range[0];
        long end = range[1];

        executors.diskIO().execute(() -> {
            List<TransactionEntity> transactions = transactionRepository.getTransactionsBetween(start, end);

            List<CategoryTreeNode> categoryTree = categoryRepository.getCategoryTree().getValue();
            if (categoryTree == null) {
                categoryTree = fetchCategoryTreeSync();
            }

            Map<String, CategoryEntity> allCategories = flattenCategories(categoryTree);
            Map<String, String> categoryToRoot = buildCategoryToRootMap(categoryTree);

            // Aggregate by actual category for transactions under selected root
            Map<String, Double> subSums = new HashMap<>();
            for (TransactionEntity t : transactions) {
                String rootId = categoryToRoot.get(t.getCategoryId());
                if (rootId == null) rootId = t.getCategoryId();
                if (rootId.equals(rootCategoryId)) {
                    String actualId = t.getCategoryId();
                    subSums.put(actualId, subSums.getOrDefault(actualId, 0.0) + t.getAmount());
                }
            }

            double total = 0;
            for (double v : subSums.values()) total += v;

            List<CategoryExpense> subExpenses = new ArrayList<>();
            for (Map.Entry<String, Double> entry : subSums.entrySet()) {
                CategoryEntity cat = allCategories.get(entry.getKey());
                if (cat == null) continue;
                double amt = entry.getValue();
                double pct = total > 0 ? (amt / total) * 100 : 0;
                subExpenses.add(new CategoryExpense(
                        cat.getCategoryId(), cat.getParentCategoryId(), cat.getName(),
                        cat.getIcon(), cat.getColour(), amt, pct
                ));
            }
            subExpenses.sort((a, b) -> Double.compare(b.getAmount(), a.getAmount()));

            CategoryEntity rootCat = allCategories.get(rootCategoryId);
            String title = rootCat != null ? rootCat.getName() : "Breakdown";

            executors.mainThread().execute(() -> {
                drillDownTitle.setValue(title);
                drillDownExpenses.setValue(subExpenses);
            });
        });
    }

    private void updatePeriodLabel() {
        PeriodType type = periodType.getValue();
        Integer year = selectedYear.getValue();
        Integer month = selectedMonth.getValue();
        if (type == null || year == null || month == null) return;

        Calendar cal = Calendar.getInstance();
        cal.set(year, month, 1);
        java.text.SimpleDateFormat sdf;
        if (type == PeriodType.MONTH) {
            sdf = new java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault());
        } else {
            sdf = new java.text.SimpleDateFormat("yyyy", java.util.Locale.getDefault());
        }
        periodLabel.setValue(sdf.format(cal.getTime()));
    }
}
