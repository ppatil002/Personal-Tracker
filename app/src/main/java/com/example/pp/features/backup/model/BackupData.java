package com.example.pp.features.backup.model;

import com.example.pp.data.local.entity.BackgroundJobLog;
import com.example.pp.data.local.entity.CategoryEntity;
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

    public BackupData(List<CategoryEntity> categories, List<TagEntity> tags, 
                      List<TransactionEntity> transactions, List<TransactionTagCrossRef> transactionTags,
                      List<TaskDefinition> taskDefinitions, List<TaskState> taskStates,
                      List<BackgroundJobLog> backgroundJobLogs) {
        this.categories = categories;
        this.tags = tags;
        this.transactions = transactions;
        this.transactionTags = transactionTags;
        this.taskDefinitions = taskDefinitions;
        this.taskStates = taskStates;
        this.backgroundJobLogs = backgroundJobLogs;
    }
}