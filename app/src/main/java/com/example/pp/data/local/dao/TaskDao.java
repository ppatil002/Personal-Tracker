package com.example.pp.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Relation;
import androidx.room.Transaction;
import androidx.room.Update;

import com.example.pp.core.enums.TaskStatus;
import com.example.pp.core.enums.TaskType;
import com.example.pp.data.local.entity.TaskDefinition;
import com.example.pp.data.local.entity.TaskState;
import com.example.pp.data.local.model.SyncStats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Dao
public interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertDefinition(TaskDefinition definition);

    @Update
    void updateDefinition(TaskDefinition definition);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertState(TaskState state);

    @Update
    void updateState(TaskState state);

    @Query("SELECT * FROM task_states WHERE task_id = (SELECT task_id FROM task_definitions WHERE task_type = :type LIMIT 1)")
    TaskState getStateByType(TaskType type);

    @Query("SELECT * FROM task_states WHERE task_id = :taskId")
    TaskState getStateForTask(String taskId);

    @Query("SELECT * FROM task_definitions WHERE task_type = :type LIMIT 1")
    TaskDefinition getDefinitionByType(TaskType type);

    @Query("SELECT * FROM task_definitions WHERE task_id = :taskId")
    TaskDefinition getDefinitionById(String taskId);

    @Transaction
    default void updateTaskState(String taskId, TaskStatus newState) {
        TaskState state = getStateForTask(taskId);
        if (state != null) {
            state.setCurrentState(newState);
            updateState(state);
        }
    }

    /**
     * returns both the definition and its runtime state for all enabled tasks.
     * Use this in the Orchestrator to decide what to run.
     */
    @Transaction
    @Query("SELECT * FROM task_definitions WHERE enabled = 1")
    List<TaskWithState> getEnabledTasksWithState();

    @Transaction
    @Query("SELECT * FROM task_definitions")
    List<TaskWithState> getAllTasksWithState();

    @Transaction
    @Query("SELECT * FROM task_definitions")
    LiveData<List<TaskWithState>> getAllTasksWithStateLiveData();

    class TaskWithState {
        @androidx.room.Embedded
        public TaskDefinition definition;
        
        @Relation(
                parentColumn = "task_id",
                entityColumn = "task_id"
        )
        public TaskState state;
    }

    @Query("SELECT * FROM task_definitions")
    List<TaskDefinition> getAllDefinitions();

    @Query("SELECT * FROM task_states")
    List<TaskState> getAllStates();

    @Query("SELECT COUNT(*) FROM task_definitions")
    int getDefinitionsCount();

    @Query("SELECT COUNT(*) FROM task_states")
    int getStatesCount();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertDefinitionsBatch(List<TaskDefinition> definitions);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertStatesBatch(List<TaskState> states);

    @Update
    void updateDefinitionsBatch(List<TaskDefinition> definitions);

    @Update
    void updateStatesBatch(List<TaskState> states);

    @Transaction
    default SyncStats upsertAllDefinitions(List<TaskDefinition> cloudItems) {
        SyncStats stats = new SyncStats("Task Definitions");
        stats.previousCount = getDefinitionsCount();
        if (cloudItems == null || cloudItems.isEmpty()) {
            stats.finalCount = stats.previousCount;
            return stats;
        }

        List<TaskDefinition> localItems = getAllDefinitions();
        Map<String, TaskDefinition> localMap = new HashMap<>();
        for (TaskDefinition local : localItems) {
            localMap.put(local.getTaskId(), local);
        }

        List<TaskDefinition> toInsert = new ArrayList<>();
        List<TaskDefinition> toUpdate = new ArrayList<>();

        for (TaskDefinition cloud : cloudItems) {
            TaskDefinition local = localMap.get(cloud.getTaskId());
            if (local == null) {
                toInsert.add(cloud);
            } else if (cloud.getUpdatedAt() > local.getUpdatedAt()) {
                toUpdate.add(cloud);
            }
        }

        if (!toInsert.isEmpty()) insertDefinitionsBatch(toInsert);
        if (!toUpdate.isEmpty()) updateDefinitionsBatch(toUpdate);

        stats.inserted = toInsert.size();
        stats.updated = toUpdate.size();
        stats.finalCount = getDefinitionsCount();
        return stats;
    }

    @Transaction
    default SyncStats upsertAllStates(List<TaskState> cloudItems) {
        SyncStats stats = new SyncStats("Task States");
        stats.previousCount = getStatesCount();
        if (cloudItems == null || cloudItems.isEmpty()) {
            stats.finalCount = stats.previousCount;
            return stats;
        }

        List<TaskState> localItems = getAllStates();
        Map<String, TaskState> localMap = new HashMap<>();
        for (TaskState local : localItems) {
            localMap.put(local.getTaskId(), local);
        }

        List<TaskState> toInsert = new ArrayList<>();
        List<TaskState> toUpdate = new ArrayList<>();

        for (TaskState cloud : cloudItems) {
            TaskState local = localMap.get(cloud.getTaskId());
            if (local == null) {
                toInsert.add(cloud);
            } else if (cloud.getLastRunTime() > local.getLastRunTime() 
                    || cloud.getCurrentState() != local.getCurrentState()
                    || cloud.getPausedAt() != local.getPausedAt()
                    || cloud.getSnoozedUntil() != local.getSnoozedUntil()
                    || cloud.getProgress() != local.getProgress()
                    || cloud.isRunning() != local.isRunning()) {
                toUpdate.add(cloud);
            }
        }

        if (!toInsert.isEmpty()) insertStatesBatch(toInsert);
        if (!toUpdate.isEmpty()) updateStatesBatch(toUpdate);

        stats.inserted = toInsert.size();
        stats.updated = toUpdate.size();
        stats.finalCount = getStatesCount();
        return stats;
    }
}
