package com.example.pp.features.backup.model;

import com.example.pp.data.local.entity.BackgroundJobLog;
import com.example.pp.data.local.entity.CategoryEntity;
import com.example.pp.data.local.entity.HydrationIntake;
import com.example.pp.data.local.entity.NutritionIntake;
import com.example.pp.data.local.entity.ScreenTime;
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
    public List<NutritionIntake> nutritionIntakes;
    public List<SleepEntity> sleep;

    public BackupData(List<CategoryEntity> categories, List<TagEntity> tags, 
                      List<TransactionEntity> transactions, List<TransactionTagCrossRef> transactionTags,
                      List<TaskDefinition> taskDefinitions, List<TaskState> taskStates,
                      List<BackgroundJobLog> backgroundJobLogs,
                      List<HydrationIntake> hydrationIntakes,
                      List<NutritionIntake> nutritionIntakes) {
                      List<NutritionIntake> nutritionIntakes,
                      List<ScreenTime> screenTime) {
        this.categories = categories;
        this.tags = tags;
        this.transactions = transactions;
        this.transactionTags = transactionTags;
        this.taskDefinitions = taskDefinitions;
        this.taskStates = taskStates;
        this.backgroundJobLogs = backgroundJobLogs;
        this.hydrationIntakes = hydrationIntakes;
        this.nutritionIntakes = nutritionIntakes;
        this.screenTime = screenTime;
    }
}
