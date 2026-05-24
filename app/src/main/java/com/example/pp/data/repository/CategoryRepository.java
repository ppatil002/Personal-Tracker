package com.example.pp.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.pp.core.cache.CategoryCacheManager;
import com.example.pp.core.enums.AppEvent;
import com.example.pp.core.event.EventBus;
import com.example.pp.core.task.AppExecutors;
import com.example.pp.data.local.AppDatabase;
import com.example.pp.data.local.dao.CategoryDao;
import com.example.pp.data.local.entity.CategoryEntity;
import com.example.pp.domain.mapper.CategoryMapper;
import com.example.pp.domain.model.CategoryTreeNode;

import java.util.List;

public class CategoryRepository {
    private final CategoryDao categoryDao;
    private final CategoryCacheManager cacheManager;
    private final AppExecutors executors;
    private final MutableLiveData<List<CategoryTreeNode>> categoryTreeLiveData = new MutableLiveData<>();

    public CategoryRepository(Context context) {
        this.categoryDao = AppDatabase.getInstance(context).categoryDao();
        this.cacheManager = CategoryCacheManager.getInstance();
        this.executors = AppExecutors.getInstance();
    }

    /**
     * Public observer for the category tree.
     */
    public LiveData<List<CategoryTreeNode>> getCategoryTree() {
        refreshCategoryTree(); // Trigger async refresh if needed
        return categoryTreeLiveData;
    }

    private void refreshCategoryTree() {
        executors.diskIO().execute(() -> {
            if (cacheManager.isDirty() || categoryTreeLiveData.getValue() == null) {
                List<CategoryEntity> entities = categoryDao.getAllCategories();
                List<CategoryTreeNode> tree = CategoryMapper.buildTree(entities);
                cacheManager.updateCache(tree);
                categoryTreeLiveData.postValue(tree);

                // Publish completion event
                EventBus.getInstance().publish(AppEvent.CATEGORIES_LOADED_FROM_DB);
            }
        });
    }

    public void addCategory(String parentId, String name, String icon, String colour, Runnable onComplete) {
        executors.diskIO().execute(() -> {
            String categoryId;
            if (parentId == null) {
                Integer maxId = categoryDao.getMaxRootId();
                categoryId = String.valueOf(maxId == null ? 1 : maxId + 1);
            } else {
                Double maxId = categoryDao.getMaxChildId(parentId);
                if (maxId == null) {
                    categoryId = parentId + ".1";
                } else {
                    categoryId = String.format(java.util.Locale.US, "%.1f", maxId + 0.1);
                }
            }
            CategoryEntity category = new CategoryEntity(categoryId, parentId, name, icon, colour, System.currentTimeMillis());
            categoryDao.insert(category);
            cacheManager.setDirty(true);
            refreshCategoryTree();
            if (onComplete != null) executors.mainThread().execute(onComplete);
        });
    }

    public void updateCategory(CategoryEntity category, Runnable onComplete) {
        executors.diskIO().execute(() -> {
            category.setLastUpdatedTimestamp(System.currentTimeMillis());
            categoryDao.update(category);
            cacheManager.setDirty(true);
            refreshCategoryTree();
            if (onComplete != null) executors.mainThread().execute(onComplete);
        });
    }

    public void deleteCategory(CategoryEntity category, Runnable onComplete) {
        executors.diskIO().execute(() -> {
            categoryDao.delete(category);
            cacheManager.setDirty(true);
            refreshCategoryTree();
            if (onComplete != null) executors.mainThread().execute(onComplete);
        });
    }

    public void seedDatabaseIfEmpty() {
        executors.diskIO().execute(() -> {
            List<CategoryEntity> existing = categoryDao.getAllCategories();
            if (existing.isEmpty()) {
                long now = System.currentTimeMillis();
                // FOOD
                categoryDao.insert(new CategoryEntity("1", null,
                        "Food", "🍴", "#F97316", now));
                categoryDao.insert(new CategoryEntity("1.1", "1",
                        "Morning Breakfast", "🥞", "#FACC15", now));
                categoryDao.insert(new CategoryEntity("1.2", "1",
                        "Lunch", "🥗", "#FB923C", now));
                categoryDao.insert(new CategoryEntity("1.3", "1",
                        "Evening Breakfast", "☕", "#C084FC", now));
                categoryDao.insert(new CategoryEntity("1.4", "1",
                        "Dinner", "🍱", "#EF4444", now));
                categoryDao.insert(new CategoryEntity("1.5", "1",
                        "Juice",  "🍹", "#22C55E", now));
                categoryDao.insert(new CategoryEntity("1.6", "1",
                        "Grocery", "🛒", "#EC4899", now));
                categoryDao.insert(new CategoryEntity("1.7", "1",
                        "Milk", "🥛", "#E5E7EB", now));
                categoryDao.insert(new CategoryEntity("1.8", "1",
                        "Gym", "🏋️", "#38BDF8", now));
                
                // RECURRING
                categoryDao.insert(new CategoryEntity("2", null,
                        "Recurring", "🔁", "#A855F7", now));
                categoryDao.insert(new CategoryEntity("2.1", "2",
                        "Rent", "🏠", "#38BDF8", now));
                categoryDao.insert(new CategoryEntity("2.2", "2",
                        "Phone Recharge", "📱", "#22D3EE", now));
                categoryDao.insert(new CategoryEntity("2.3", "2",
                        "Electricity Bill", "💡", "#FACC15", now));
                categoryDao.insert(new CategoryEntity("2.4", "2",
                        "Gym Membership", "🏋️", "#06B6D4", now));
                categoryDao.insert(new CategoryEntity("2.5", "2",
                        "Water", "🚰", "#3B82F6", now));
                categoryDao.insert(new CategoryEntity("2.6", "2",
                        "Gas", "🔥", "#F97316", now));
                
                // MONEY TRANSFER
                categoryDao.insert(new CategoryEntity("3", null,
                        "Money Transfer", "💸", "#22D3EE", now));
                categoryDao.insert(new CategoryEntity("3.1", "3",
                        "Dad", "👨", "#6366F1", now));
                categoryDao.insert(new CategoryEntity("3.2", "3",
                        "Mom", "👩", "#EC4899", now));
                categoryDao.insert(new CategoryEntity("3.3", "3",
                        "Sister", "👧", "#F472B6", now));

                // NEW PURCHASE
                categoryDao.insert(new CategoryEntity("4", null,
                        "New Purchase", "🎁", "#8B5CF6", now));

                // Commute
                categoryDao.insert(new CategoryEntity("5", null,
                        "Commute", "🚗", "#0EA5E9", now));

                // ENTERTAINMENT
                categoryDao.insert(new CategoryEntity("6", null,
                        "Entertainment", "🎮", "#D946EF", now));
                categoryDao.insert(new CategoryEntity("6.1", "6",
                        "Activity", "🎯", "#A855F7", now));
                categoryDao.insert(new CategoryEntity("6.2", "6",
                        "Transport", "🚕", "#F59E0B", now));

                // MISCELLANEOUS
                categoryDao.insert(new CategoryEntity("7", null,
                        "Miscellaneous", "📦", "#6B7280", now));

                // Medical
                categoryDao.insert(new CategoryEntity("8", null,
                        "Medical", "⚕️", "#0EA5E9", now));
                categoryDao.insert(new CategoryEntity("8.1", "8",
                        "Medicines", "💊", "#F97316", now));
                categoryDao.insert(new CategoryEntity("8.2", "8",
                        "Doctor Fees", "🩺", "#14B8A6", now));

                // VACATIONS
                categoryDao.insert(new CategoryEntity("9", null,
                        "Vacations", "🏖️", "#0EA5E9", now));
                categoryDao.insert(new CategoryEntity("9.1", "9",
                        "Conveyance", "✈️", "#14B8A6", now));
                categoryDao.insert(new CategoryEntity("9.2", "9",
                        "Cuisine", "🍜", "#F97316", now));
                categoryDao.insert(new CategoryEntity("9.3", "9",
                        "Accommodation", "🏨", "#6366F1", now));
                categoryDao.insert(new CategoryEntity("9.4", "9",
                        "Shopping", "🛍️", "#EC4899", now));
                categoryDao.insert(new CategoryEntity("9.5", "9",
                        "Extra", "🎒", "#A1A1AA", now));
                cacheManager.setDirty(true);
                refreshCategoryTree();
            }
        });
    }
}