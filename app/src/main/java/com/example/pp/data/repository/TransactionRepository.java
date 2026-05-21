package com.example.pp.data.repository;

import android.content.Context;

import androidx.sqlite.db.SupportSQLiteQuery;

import com.example.pp.core.task.AppExecutors;
import com.example.pp.data.local.AppDatabase;
import com.example.pp.data.local.dao.TransactionDao;
import com.example.pp.data.local.entity.TransactionEntity;
import com.example.pp.data.local.query.TableQueryBuilder;
import com.example.pp.features.analytics.model.MonthlyExpense;
import com.example.pp.ui.components.table.model.TableQuery;

import java.util.Arrays;
import java.util.List;

public class TransactionRepository {
    private final TransactionDao transactionDao;
    private final AppExecutors executors;

    public TransactionRepository(Context context) {
        this.transactionDao = AppDatabase.getInstance(context).transactionDao();
        this.executors = AppExecutors.getInstance();
    }

    public List<TransactionEntity> getTableData(TableQuery query) {
        SupportSQLiteQuery sqliteQuery = TableQueryBuilder.build("transactions", query);
        return transactionDao.getTableData(sqliteQuery);
    }

    public void saveTransaction(TransactionEntity transaction, List<String> tagIds, Runnable onSuccess, Runnable onError) {
        executors.diskIO().execute(() -> {
            try {
                transaction.setLastUpdatedTimestamp(System.currentTimeMillis());
                if (tagIds == null || tagIds.isEmpty()) {
                    transactionDao.insert(transaction);
                } else {
                    transactionDao.insertTransactionWithTags(transaction, tagIds);
                }
                executors.mainThread().execute(onSuccess);
            } catch (Exception e) {
                executors.mainThread().execute(onError);
            }
        });
    }

    // TODO: Remove this seed data method after testing
    public void seedTransactionsIfEmpty() {
        executors.diskIO().execute(() -> {
            List<TransactionEntity> existing = transactionDao.getAllTransactionsList();
            if (existing.isEmpty()) {
                // March 2026 transactions
                transactionDao.insertTransactionWithTags(
                    new TransactionEntity("t1", "1.1", "Poha and Tea", 50.0, "Breakfast at home",
                        getTimestamp(2026, 3, 1), System.currentTimeMillis(), System.currentTimeMillis(), "UPI", "expense"),
                    Arrays.asList("1")
                );
                transactionDao.insertTransactionWithTags(
                    new TransactionEntity("t2", "1.2", "Office Lunch", 120.0, "Canteen",
                        getTimestamp(2026, 3, 2), System.currentTimeMillis(), System.currentTimeMillis(), "Cash", "expense"),
                    Arrays.asList("1", "2")
                );
                transactionDao.insert(
                    new TransactionEntity("t3", "1.4", "Dinner at Restaurant", 350.0, "Family dinner",
                        getTimestamp(2026, 3, 3), System.currentTimeMillis(), System.currentTimeMillis(), "Card", "expense")
                );
                transactionDao.insertTransactionWithTags(
                    new TransactionEntity("t4", "1.6", "Grocery Shopping", 850.0, "Weekly groceries",
                        getTimestamp(2026, 3, 5), System.currentTimeMillis(), System.currentTimeMillis(), "UPI", "expense"),
                    Arrays.asList("4")
                );
                transactionDao.insert(
                    new TransactionEntity("t5", "2.1", "Rent Payment", 15000.0, "Monthly rent",
                        getTimestamp(2026, 3, 5), System.currentTimeMillis(), System.currentTimeMillis(), "UPI", "expense")
                );
                transactionDao.insertTransactionWithTags(
                    new TransactionEntity("t6", "1.5", "Fresh Juice", 80.0, "Orange juice",
                        getTimestamp(2026, 3, 6), System.currentTimeMillis(), System.currentTimeMillis(), "Cash", "expense"),
                    Arrays.asList("3")
                );
                transactionDao.insertTransactionWithTags(
                    new TransactionEntity("t7", "1.8", "Gym Workout", 500.0, "Monthly gym fee",
                        getTimestamp(2026, 3, 7), System.currentTimeMillis(), System.currentTimeMillis(), "UPI", "expense"),
                    Arrays.asList("6")
                );
                transactionDao.insert(
                    new TransactionEntity("t8", "2.2", "Phone Recharge", 299.0, "Jio recharge",
                        getTimestamp(2026, 3, 8), System.currentTimeMillis(), System.currentTimeMillis(), "UPI", "expense")
                );
                transactionDao.insertTransactionWithTags(
                    new TransactionEntity("t9", "1.3", "Evening Snacks", 45.0, "Samosa and chai",
                        getTimestamp(2026, 3, 10), System.currentTimeMillis(), System.currentTimeMillis(), "Cash", "expense"),
                    Arrays.asList("1")
                );
                transactionDao.insertTransactionWithTags(
                    new TransactionEntity("t10", "5", "Auto to Office", 60.0, "Daily commute",
                        getTimestamp(2026, 3, 11), System.currentTimeMillis(), System.currentTimeMillis(), "Cash", "expense"),
                    Arrays.asList("2")
                );
                transactionDao.insert(
                    new TransactionEntity("t11", "2.3", "Electricity Bill", 1200.0, "Monthly bill",
                        getTimestamp(2026, 3, 12), System.currentTimeMillis(), System.currentTimeMillis(), "UPI", "expense")
                );
                transactionDao.insertTransactionWithTags(
                    new TransactionEntity("t12", "1.2", "Team Lunch", 250.0, "Office team outing",
                        getTimestamp(2026, 3, 13), System.currentTimeMillis(), System.currentTimeMillis(), "Card", "expense"),
                    Arrays.asList("1")
                );
                transactionDao.insert(
                    new TransactionEntity("t13", "6.1", "Movie Tickets", 400.0, "Weekend movie",
                        getTimestamp(2026, 3, 14), System.currentTimeMillis(), System.currentTimeMillis(), "UPI", "expense")
                );
                transactionDao.insertTransactionWithTags(
                    new TransactionEntity("t14", "1.6", "Vegetables", 200.0, "Local market",
                        getTimestamp(2026, 3, 15), System.currentTimeMillis(), System.currentTimeMillis(), "Cash", "expense"),
                    Arrays.asList("4")
                );
                transactionDao.insertTransactionWithTags(
                    new TransactionEntity("t15", "3.1", "Transfer to Dad", 5000.0, "Emergency",
                        getTimestamp(2026, 3, 16), System.currentTimeMillis(), System.currentTimeMillis(), "UPI", "expense"),
                    Arrays.asList("8")
                );
                transactionDao.insert(
                    new TransactionEntity("t16", "7", "Miscellaneous", 150.0, "Stationery",
                        getTimestamp(2026, 3, 18), System.currentTimeMillis(), System.currentTimeMillis(), "Cash", "expense")
                );
                transactionDao.insertTransactionWithTags(
                    new TransactionEntity("t17", "1.7", "Milk", 60.0, "Daily milk",
                        getTimestamp(2026, 3, 19), System.currentTimeMillis(), System.currentTimeMillis(), "Cash", "expense"),
                    Arrays.asList("5")
                );
                transactionDao.insertTransactionWithTags(
                    new TransactionEntity("t18", "2.4", "Gym Membership", 3000.0, "Quarterly membership",
                        getTimestamp(2026, 3, 20), System.currentTimeMillis(), System.currentTimeMillis(), "UPI", "expense"),
                    Arrays.asList("6")
                );
                transactionDao.insert(
                    new TransactionEntity("t19", "4", "New Shoes", 2500.0, "Running shoes",
                        getTimestamp(2026, 3, 22), System.currentTimeMillis(), System.currentTimeMillis(), "Card", "expense")
                );
                transactionDao.insertTransactionWithTags(
                    new TransactionEntity("t20", "6.2", "Taxi to Airport", 450.0, "Trip to airport",
                        getTimestamp(2026, 3, 25), System.currentTimeMillis(), System.currentTimeMillis(), "UPI", "expense"),
                    Arrays.asList("2")
                );
                transactionDao.insert(
                    new TransactionEntity("t21", "1.4", "Order Food", 300.0, "Swiggy order",
                        getTimestamp(2026, 3, 26), System.currentTimeMillis(), System.currentTimeMillis(), "UPI", "expense")
                );
                transactionDao.insertTransactionWithTags(
                    new TransactionEntity("t22", "1.5", "Fruit Juice", 70.0, "Mixed fruit",
                        getTimestamp(2026, 3, 28), System.currentTimeMillis(), System.currentTimeMillis(), "Cash", "expense"),
                    Arrays.asList("3")
                );
                transactionDao.insert(
                    new TransactionEntity("t23", "2.5", "Water Bill", 350.0, "Monthly water",
                        getTimestamp(2026, 3, 30), System.currentTimeMillis(), System.currentTimeMillis(), "UPI", "expense")
                );

                // April 2026 transactions
                transactionDao.insertTransactionWithTags(
                    new TransactionEntity("t24", "1.1", "Breakfast", 55.0, "Idli sambar",
                        getTimestamp(2026, 4, 2), System.currentTimeMillis(), System.currentTimeMillis(), "Cash", "expense"),
                    Arrays.asList("1")
                );
                transactionDao.insertTransactionWithTags(
                    new TransactionEntity("t25", "1.6", "Groceries", 920.0, "Monthly stock",
                        getTimestamp(2026, 4, 5), System.currentTimeMillis(), System.currentTimeMillis(), "UPI", "expense"),
                    Arrays.asList("4", "5")
                );
                transactionDao.insert(
                    new TransactionEntity("t26", "2.1", "Rent Payment", 15000.0, "Monthly rent",
                        getTimestamp(2026, 4, 5), System.currentTimeMillis(), System.currentTimeMillis(), "UPI", "expense")
                );
                transactionDao.insertTransactionWithTags(
                    new TransactionEntity("t27", "9.2", "Vacation Food", 800.0, "Trip dining",
                        getTimestamp(2026, 4, 8), System.currentTimeMillis(), System.currentTimeMillis(), "Card", "expense"),
                    Arrays.asList("2")
                );
                transactionDao.insertTransactionWithTags(
                    new TransactionEntity("t28", "9.1", "Flight Tickets", 4500.0, "Vacation travel",
                        getTimestamp(2026, 4, 10), System.currentTimeMillis(), System.currentTimeMillis(), "UPI", "expense"),
                    Arrays.asList("2")
                );
                transactionDao.insert(
                    new TransactionEntity("t29", "9.3", "Hotel Stay", 3500.0, "2 nights",
                        getTimestamp(2026, 4, 10), System.currentTimeMillis(), System.currentTimeMillis(), "Card", "expense")
                );
                transactionDao.insertTransactionWithTags(
                    new TransactionEntity("t30", "1.2", "Lunch Out", 180.0, "Restaurant",
                        getTimestamp(2026, 4, 12), System.currentTimeMillis(), System.currentTimeMillis(), "UPI", "expense"),
                    Arrays.asList("2")
                );
                transactionDao.insertTransactionWithTags(
                    new TransactionEntity("t31", "3.2", "Transfer to Mom", 3000.0, "Help",
                        getTimestamp(2026, 4, 15), System.currentTimeMillis(), System.currentTimeMillis(), "UPI", "expense"),
                    Arrays.asList("8")
                );
                transactionDao.insert(
                    new TransactionEntity("t32", "6.1", "Bowling", 600.0, "With friends",
                        getTimestamp(2026, 4, 18), System.currentTimeMillis(), System.currentTimeMillis(), "UPI", "expense")
                );
                transactionDao.insertTransactionWithTags(
                    new TransactionEntity("t33", "1.3", "Evening Tea", 30.0, "Chai break",
                        getTimestamp(2026, 4, 20), System.currentTimeMillis(), System.currentTimeMillis(), "Cash", "expense"),
                    Arrays.asList("1")
                );
                transactionDao.insert(
                    new TransactionEntity("t34", "2.2", "Phone Recharge", 299.0, "Jio recharge",
                        getTimestamp(2026, 4, 22), System.currentTimeMillis(), System.currentTimeMillis(), "UPI", "expense")
                );
                transactionDao.insertTransactionWithTags(
                    new TransactionEntity("t35", "5", "Bus Pass", 1500.0, "Monthly pass",
                        getTimestamp(2026, 4, 25), System.currentTimeMillis(), System.currentTimeMillis(), "UPI", "expense"),
                    Arrays.asList("2")
                );
                transactionDao.insertTransactionWithTags(
                    new TransactionEntity("t36", "1.7", "Milk and Curd", 120.0, "Dairy products",
                        getTimestamp(2026, 4, 27), System.currentTimeMillis(), System.currentTimeMillis(), "Cash", "expense"),
                    Arrays.asList("5")
                );
                transactionDao.insert(
                    new TransactionEntity("t37", "2.3", "Electricity Bill", 1350.0, "Monthly bill",
                        getTimestamp(2026, 4, 30), System.currentTimeMillis(), System.currentTimeMillis(), "UPI", "expense")
                );

                // May 2026 transactions
                transactionDao.insertTransactionWithTags(
                    new TransactionEntity("t38", "1.1", "Breakfast", 60.0, "Paratha",
                        getTimestamp(2026, 5, 2), System.currentTimeMillis(), System.currentTimeMillis(), "Cash", "expense"),
                    Arrays.asList("1")
                );
                transactionDao.insertTransactionWithTags(
                    new TransactionEntity("t39", "1.6", "Fruits", 350.0, "Seasonal fruits",
                        getTimestamp(2026, 5, 5), System.currentTimeMillis(), System.currentTimeMillis(), "Cash", "expense"),
                    Arrays.asList("3")
                );
                transactionDao.insert(
                    new TransactionEntity("t40", "2.1", "Rent Payment", 15000.0, "Monthly rent",
                        getTimestamp(2026, 5, 5), System.currentTimeMillis(), System.currentTimeMillis(), "UPI", "expense")
                );
            }
        });
    }

    public List<TransactionEntity> getTransactionsBetween(long start, long end) {
        return transactionDao.getTransactionsBetween(start, end);
    }

    public List<MonthlyExpense> getMonthlyExpensesBetween(long start, long end) {
        return transactionDao.getMonthlyExpensesBetween(start, end);
    }

    public int getTransactionCountBetween(long start, long end) {
        return transactionDao.getTransactionCountBetween(start, end);
    }

    public double getTotalAmountBetween(long start, long end) {
        return transactionDao.getTotalAmountBetween(start, end);
    }

    private long getTimestamp(int year, int month, int day) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(year, month - 1, day, 12, 0, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}