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
import com.example.pp.ui.components.table.model.TableCell;
import com.example.pp.ui.components.table.model.TableColumn;
import com.example.pp.ui.components.table.model.TableRow;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.text.SimpleDateFormat;
import java.util.Locale;

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
    
    private final MutableLiveData<String> selectedTableCategoryId = new MutableLiveData<>();
    private final MutableLiveData<List<CategoryEntity>> rootCategories = new MutableLiveData<>();
    private final MutableLiveData<List<TableColumn>> tableColumns = new MutableLiveData<>();
    private final MutableLiveData<List<TableRow>> tableRows = new MutableLiveData<>();
    private final MutableLiveData<List<CategoryGroup>> tableColumnGroups = new MutableLiveData<>();
    private final MutableLiveData<Boolean> tableExpandable = new MutableLiveData<>(false);
    private final MutableLiveData<List<TransactionEntity>> periodTransactions = new MutableLiveData<>();

    public static class CategoryGroup {
        public String name;
        public int span;
        public CategoryGroup(String name, int span) { this.name = name; this.span = span; }
    }

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
    
    public LiveData<String> getSelectedTableCategoryId() { return selectedTableCategoryId; }
    public LiveData<List<CategoryEntity>> getRootCategories() { return rootCategories; }
    public LiveData<List<TableColumn>> getTableColumns() { return tableColumns; }
    public LiveData<List<TableRow>> getTableRows() { return tableRows; }
    public LiveData<List<CategoryGroup>> getTableColumnGroups() { return tableColumnGroups; }
    public LiveData<Boolean> getTableExpandable() { return tableExpandable; }
    public LiveData<List<TransactionEntity>> getPeriodTransactions() { return periodTransactions; }

    public void setTableExpanded(boolean expanded) {
        tableExpandable.setValue(expanded);
    }

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

            final List<CategoryTreeNode> finalTree = (categoryTree != null) ? categoryTree : new ArrayList<>();

            executors.mainThread().execute(() -> {
                analyticsSummary.setValue(summary);
                categoryExpenses.setValue(catExpenses);
                dailyTrend.setValue(trend);
                
                List<CategoryEntity> roots = new ArrayList<>();
                for (CategoryTreeNode node : finalTree) roots.add(node.getCategory());
                rootCategories.setValue(roots);
                
                // Use currently selected category if available, otherwise default to first
                String currentId = selectedTableCategoryId.getValue();
                if (currentId == null && !roots.isEmpty()) {
                    currentId = roots.get(0).getCategoryId();
                    selectedTableCategoryId.setValue(currentId);
                }
                
                if (currentId != null) {
                    loadTableData(currentId);
                }
            });
        });
    }

    public void loadTableData(String rootCategoryId) {
        selectedTableCategoryId.postValue(rootCategoryId);
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
            if (categoryTree == null) categoryTree = fetchCategoryTreeSync();

            // Find subcategories
            List<CategoryEntity> subCats = new ArrayList<>();
            List<CategoryGroup> groups = new ArrayList<>();
            
            if ("all".equals(rootCategoryId)) {
                groups.add(new CategoryGroup("", 1)); // For the date column
                for (CategoryTreeNode root : categoryTree) {
                    int initialSize = subCats.size();
                    if (root.getChildren().isEmpty()) {
                        subCats.add(root.getCategory());
                    } else {
                        for (CategoryTreeNode child : root.getChildren()) {
                            subCats.add(child.getCategory());
                        }
                    }
                    int span = subCats.size() - initialSize;
                    if (span > 0) {
                        groups.add(new CategoryGroup(root.getName(), span));
                    }
                }
            } else {
                for (CategoryTreeNode root : categoryTree) {
                    if (root.getCategoryId().equals(rootCategoryId)) {
                        if (root.getChildren().isEmpty()) {
                            subCats.add(root.getCategory());
                        } else {
                            for (CategoryTreeNode child : root.getChildren()) {
                                subCats.add(child.getCategory());
                            }
                        }
                        break;
                    }
                }
            }

            // Map subcategories to column keys
            List<TableColumn> cols = new ArrayList<>();
            cols.add(new TableColumn("date", type == PeriodType.YEAR ? "Month" : "Date", TableColumn.ColumnType.TEXT));
            for (CategoryEntity sc : subCats) {
                cols.add(new TableColumn(sc.getCategoryId(), sc.getName(), TableColumn.ColumnType.AMOUNT));
            }

            // Group by date -> subcategory -> total
            Map<Long, Map<String, Double>> matrix = new TreeMap<>();
            for (TransactionEntity t : transactions) {
                // Check if transaction belongs to one of our subcategories
                boolean relevant = false;
                for (CategoryEntity sc : subCats) {
                    if (sc.getCategoryId().equals(t.getCategoryId())) {
                        relevant = true;
                        break;
                    }
                }
                if (!relevant) continue;

                long periodKey = type == PeriodType.YEAR ? floorToMonth(t.getTransactionDate()) : floorToDay(t.getTransactionDate());
                Map<String, Double> dayMap = matrix.get(periodKey);
                if (dayMap == null) {
                    dayMap = new HashMap<>();
                    matrix.put(periodKey, dayMap);
                }
                Double current = dayMap.get(t.getCategoryId());
                dayMap.put(t.getCategoryId(), (current != null ? current : 0.0) + t.getAmount());
            }

            // Convert matrix to TableRows
            List<TableRow> rows = new ArrayList<>();
            SimpleDateFormat sdf = (type == PeriodType.YEAR) 
                    ? new SimpleDateFormat("MMM", Locale.getDefault())
                    : new SimpleDateFormat("dd/MM", Locale.getDefault());
            Map<String, Double> totals = new HashMap<>();

            for (Map.Entry<Long, Map<String, Double>> entry : matrix.entrySet()) {
                List<TableCell> cells = new ArrayList<>();
                cells.add(new TableCell("date", sdf.format(entry.getKey()), entry.getKey(), TableColumn.ColumnType.TEXT));
                
                boolean hasData = false;
                for (CategoryEntity sc : subCats) {
                    Double amt = entry.getValue().get(sc.getCategoryId());
                    String val = (amt != null && amt != 0) ? String.format(Locale.getDefault(), "%.0f", amt) : "";
                    cells.add(new TableCell(sc.getCategoryId(), val, amt, TableColumn.ColumnType.AMOUNT));
                    if (amt != null && amt != 0) {
                        hasData = true;
                        totals.put(sc.getCategoryId(), totals.getOrDefault(sc.getCategoryId(), 0.0) + amt);
                    }
                }
                if (hasData) {
                    rows.add(new TableRow(entry.getKey().toString(), cells, entry.getKey()));
                }
            }

            // Add Footer Row (Total)
            List<TableCell> footerCells = new ArrayList<>();
            footerCells.add(new TableCell("date", "Total", null, TableColumn.ColumnType.TEXT));
            for (CategoryEntity sc : subCats) {
                Double total = totals.get(sc.getCategoryId());
                String val = (total != null && total != 0) ? String.format(Locale.getDefault(), "%.0f", total) : "0";
                footerCells.add(new TableCell(sc.getCategoryId(), val, total, TableColumn.ColumnType.AMOUNT));
            }
            rows.add(new TableRow("footer", footerCells, null));

            // Add Merged Category Total Row
            List<TableCell> groupFooterCells = new ArrayList<>();
            groupFooterCells.add(new TableCell("date", "", null, TableColumn.ColumnType.TEXT));
            
            if ("all".equals(rootCategoryId)) {
                // For "All", we have groups
                int subCatIdx = 0;
                for (int i = 1; i < groups.size(); i++) { // Skip the date group at index 0
                    CategoryGroup group = groups.get(i);
                    double groupTotal = 0;
                    String firstSubCatId = subCats.get(subCatIdx).getCategoryId();
                    for (int j = 0; j < group.span; j++) {
                        CategoryEntity sc = subCats.get(subCatIdx++);
                        groupTotal += totals.getOrDefault(sc.getCategoryId(), 0.0);
                    }
                    String val = groupTotal != 0 ? String.format(Locale.getDefault(), "%.0f", groupTotal) : "0";
                    TableCell groupCell = new TableCell(firstSubCatId, val, groupTotal, TableColumn.ColumnType.AMOUNT);
                    groupCell.setColSpan(group.span);
                    groupFooterCells.add(groupCell);
                }
            } else {
                // For single category, just one merged cell for all subcategories
                double totalAll = 0;
                for (Double v : totals.values()) totalAll += v;
                String val = totalAll != 0 ? String.format(Locale.getDefault(), "%.0f", totalAll) : "0";
                String firstSubCatId = !subCats.isEmpty() ? subCats.get(0).getCategoryId() : "unknown";
                TableCell groupCell = new TableCell(firstSubCatId, val, totalAll, TableColumn.ColumnType.AMOUNT);
                groupCell.setColSpan(subCats.size());
                groupFooterCells.add(groupCell);
            }
            rows.add(new TableRow("group_footer", groupFooterCells, null));

            // For Grand Total if All Category
            if ("all".equals(rootCategoryId)) {
                double grandTotal = 0;
                for (Double v : totals.values()) grandTotal += v;
                List<TableCell> grandFooterCells = new ArrayList<>();
                grandFooterCells.add(new TableCell("date", "Grand Total", null, TableColumn.ColumnType.TEXT));
                String firstSubCatId = !subCats.isEmpty() ? subCats.get(0).getCategoryId() : "unknown";
                TableCell grandCell = new TableCell(firstSubCatId, String.format(Locale.getDefault(), "%.0f", grandTotal), grandTotal, TableColumn.ColumnType.AMOUNT);
                grandCell.setColSpan(subCats.size());
                grandFooterCells.add(grandCell);
                rows.add(new TableRow("grand_footer", grandFooterCells, null));
            }

            executors.mainThread().execute(() -> {
                tableColumnGroups.setValue(groups);
                tableColumns.setValue(cols);
                tableRows.setValue(rows);
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

    private long floorToMonth(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    public void loadTransactionsForPeriod(long timestamp, String rootCategoryId) {
        PeriodType type = periodType.getValue();
        if (type == null) return;

        long start, end;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        
        if (type == PeriodType.YEAR) {
            cal.set(Calendar.DAY_OF_MONTH, 1);
            start = cal.getTimeInMillis();
            cal.add(Calendar.MONTH, 1);
            end = cal.getTimeInMillis();
        } else {
            start = timestamp;
            cal.add(Calendar.DAY_OF_MONTH, 1);
            end = cal.getTimeInMillis();
        }

        executors.diskIO().execute(() -> {
            List<TransactionEntity> transactions = transactionRepository.getTransactionsBetween(start, end);
            List<CategoryTreeNode> tree = categoryRepository.getCategoryTree().getValue();
            if (tree == null) tree = fetchCategoryTreeSync();
            
            Map<String, String> catToRoot = buildCategoryToRootMap(tree);
            List<TransactionEntity> filtered = new ArrayList<>();
            for (TransactionEntity t : transactions) {
                String rId = catToRoot.get(t.getCategoryId());
                if (rId == null) rId = t.getCategoryId();
                if ("all".equals(rootCategoryId) || rId.equals(rootCategoryId)) {
                    filtered.add(t);
                }
            }

            executors.mainThread().execute(() -> periodTransactions.setValue(filtered));
        });
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
