package com.example.pp;

import android.app.Application;

import com.example.pp.data.repository.BackgroundJobLogRepository;
import com.example.pp.data.repository.CategoryRepository;
import com.example.pp.data.repository.GenericTableRepository;
import com.example.pp.data.repository.HydrationRepository;
import com.example.pp.data.repository.NutrientRepository;
import com.example.pp.data.repository.NutritionRepository;
import com.example.pp.data.repository.QuoteRepository;
import com.example.pp.data.repository.HistoryRepository;
import com.example.pp.data.repository.ScreenTimeRepository;
import com.example.pp.data.repository.SleepRepository;
import com.example.pp.data.repository.TagRepository;
import com.example.pp.data.repository.TaskRepository;
import com.example.pp.data.repository.TransactionRepository;
import com.jakewharton.threetenabp.AndroidThreeTen;

public class PPApplication extends Application {
    private CategoryRepository categoryRepository;
    private TagRepository tagRepository;
    private TransactionRepository transactionRepository;
    private GenericTableRepository genericTableRepository;
    private TaskRepository taskRepository;
    private HydrationRepository hydrationRepository;
    private NutritionRepository nutritionRepository;
    private NutrientRepository nutrientRepository;
    private ScreenTimeRepository screenTimeRepository;
    private SleepRepository sleepRepository;
    private BackgroundJobLogRepository backgroundJobLogRepository;
    private QuoteRepository quoteRepository;
    private HistoryRepository historyRepository;

    @Override
    public void onCreate() {
        super.onCreate();
        AndroidThreeTen.init(this);
        categoryRepository = new CategoryRepository(this);
        tagRepository = new TagRepository(this);
        transactionRepository = new TransactionRepository(this);
        genericTableRepository = new GenericTableRepository(this);
        taskRepository = new TaskRepository(this);
        screenTimeRepository = new ScreenTimeRepository(this);
        sleepRepository = new SleepRepository(this);
        hydrationRepository = new HydrationRepository(this);
        nutritionRepository = new NutritionRepository(this);
        nutrientRepository = new NutrientRepository(this);
        backgroundJobLogRepository = new BackgroundJobLogRepository(this);
        quoteRepository = new QuoteRepository(this);
        historyRepository = new HistoryRepository(this);
    }

    public CategoryRepository getCategoryRepository() {
        return categoryRepository;
    }

    public TagRepository getTagRepository() {
        return tagRepository;
    }

    public TransactionRepository getTransactionRepository() {
        return transactionRepository;
    }

    public GenericTableRepository getGenericTableRepository() {
        return genericTableRepository;
    }

    public TaskRepository getTaskRepository() {
        return taskRepository;
    }

    public ScreenTimeRepository getScreenTimeRepository() {
        return screenTimeRepository;
    }

    public SleepRepository getSleepRepository() {
        return sleepRepository;
    }

    public HydrationRepository getHydrationRepository() {
        return hydrationRepository;
    }

    public NutritionRepository getNutritionRepository() {
        return nutritionRepository;
    }

    public NutrientRepository getNutrientRepository() {
        return nutrientRepository;
    }

    public BackgroundJobLogRepository getBackgroundJobLogRepository() {
        return backgroundJobLogRepository;
    }
    
    public QuoteRepository getQuoteRepository() {
        return quoteRepository;
    }

    public HistoryRepository getHistoryRepository() {
        return historyRepository;
    }
}
