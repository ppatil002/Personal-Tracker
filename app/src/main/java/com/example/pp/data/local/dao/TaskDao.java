package com.example.pp.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Relation;
import androidx.room.Transaction;
import androidx.room.Update;

import com.example.pp.core.enums.TaskType;
import com.example.pp.data.local.entity.TaskDefinition;
import com.example.pp.data.local.entity.TaskState;
import com.example.pp.core.enums.TaskStatus;

import androidx.lifecycle.LiveData;
import java.util.List;

@Dao
public interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertDefinition(TaskDefinition definition);

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
}
