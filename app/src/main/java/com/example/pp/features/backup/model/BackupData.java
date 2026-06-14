package com.example.pp.features.backup.model;

import com.example.pp.data.local.entity.BackgroundJobLog;
import com.example.pp.data.local.entity.CategoryEntity;
import com.example.pp.data.local.entity.HistoryApiTrack;
import com.example.pp.data.local.entity.HydrationIntake;
import com.example.pp.data.local.entity.NutrientEntity;
import com.example.pp.data.local.entity.NutritionIntake;
import com.example.pp.data.local.entity.QuotesApiTrack;
import com.example.pp.data.local.entity.SavedQuote;
import com.example.pp.data.local.entity.ScreenTime;
import com.example.pp.data.local.entity.SleepEntity;
import com.example.pp.data.local.entity.TagEntity;
import com.example.pp.data.local.entity.TaskDefinition;
import com.example.pp.data.local.entity.TaskState;
import com.example.pp.data.local.entity.TransactionEntity;
import com.example.pp.data.local.entity.TransactionTagCrossRef;

import java.util.List;

public class BackupData {
    public List<CategoryEntity> categories;
    public List<TagEntity> tags;
    public List<TransactionEntity> transactions;
    public List<TransactionTagCrossRef> transactionTags;
    public List<TaskDefinition> taskDefinitions;
    public List<TaskState> taskStates;
    public List<BackgroundJobLog> backgroundJobLogs;
    public List<HydrationIntake> hydrationIntakes;
    public List<NutrientEntity> nutrientData;
    public List<NutritionIntake> nutritionIntakes;
    public List<SleepEntity> sleep;
    public List<ScreenTime> screenTime;
    public List<HistoryApiTrack> history;
    public List<QuotesApiTrack> quotes;
    public List<SavedQuote> savedQuotes;

    public BackupData(List<CategoryEntity> categories, List<TagEntity> tags, 
                      List<TransactionEntity> transactions, List<TransactionTagCrossRef> transactionTags,
                      List<TaskDefinition> taskDefinitions, List<TaskState> taskStates,
                      List<BackgroundJobLog> backgroundJobLogs,
                      List<HydrationIntake> hydrationIntakes,
                      List<NutrientEntity> nutrientData,
                      List<NutritionIntake> nutritionIntakes,
                      List<SleepEntity> sleep,
                      List<ScreenTime> screenTime,
                      List<HistoryApiTrack> history,
                      List<QuotesApiTrack> quotes,
                      List<SavedQuote> savedQuotes) {
        this.categories = categories;
        this.tags = tags;
        this.transactions = transactions;
        this.transactionTags = transactionTags;
        this.taskDefinitions = taskDefinitions;
        this.taskStates = taskStates;
        this.backgroundJobLogs = backgroundJobLogs;
        this.hydrationIntakes = hydrationIntakes;
        this.nutrientData = nutrientData;
        this.nutritionIntakes = nutritionIntakes;
        this.sleep = sleep;
        this.screenTime = screenTime;
        this.history = history;
        this.quotes = quotes;
        this.savedQuotes = savedQuotes;
    }
}
