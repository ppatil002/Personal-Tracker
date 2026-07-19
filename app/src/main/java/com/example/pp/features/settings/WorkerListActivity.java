package com.example.pp.features.settings;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.pp.PPApplication;
import com.example.pp.R;
import com.example.pp.core.orchestrator.SystemTaskScheduler;
import com.example.pp.core.task.AppExecutors;
import com.example.pp.data.local.dao.TaskDao;
import com.example.pp.data.repository.TaskRepository;

import java.util.List;

public class WorkerListActivity extends AppCompatActivity {

    private TaskRepository taskRepository;
    private WorkerAdapter adapter;
    private final AppExecutors executors = AppExecutors.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_list);

        taskRepository = ((PPApplication) getApplication()).getTaskRepository();

        boolean fromNotification = getIntent().getBooleanExtra("FROM_NOTIFICATION", false);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            if (fromNotification) {
                startActivity(new Intent(this, MoreActivity.class));
            }
            finish();
        });

        findViewById(R.id.btnRefresh).setOnClickListener(v -> {
            executors.diskIO().execute(() -> {
                SystemTaskScheduler.scheduleAllTasksSync(this);
                executors.mainThread().execute(() -> {
                    Toast.makeText(this, "All tasks re-scheduled and updated.", Toast.LENGTH_SHORT).show();
                });
            });
        });

        RecyclerView rvWorkers = findViewById(R.id.rvWorkers);
        rvWorkers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WorkerAdapter();
        adapter.setOnTaskActionListener(new WorkerAdapter.OnTaskActionListener() {
            @Override
            public void onRunNow(String taskId) {
                taskRepository.runTaskNow(taskId);
                Toast.makeText(WorkerListActivity.this, taskId + " triggered manually", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPauseTask(String taskId) {
                taskRepository.pauseTask(taskId);
                Toast.makeText(WorkerListActivity.this, taskId + " paused", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResumeTask(String taskId) {
                taskRepository.resumeTask(taskId);
                Toast.makeText(WorkerListActivity.this, taskId + " resumed", Toast.LENGTH_SHORT).show();
            }
        });
        rvWorkers.setAdapter(adapter);

        taskRepository.getAllTasksWithStateLiveData().observe(this, tasks -> {
            if (tasks != null) {
                adapter.setTasks(tasks);
            }
        });

        syncWithWorkManager();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        boolean fromNotification = getIntent().getBooleanExtra("FROM_NOTIFICATION", false);
        if (fromNotification) {
            startActivity(new Intent(this, MoreActivity.class));
        }
        finish();
    }

    private void syncWithWorkManager() {
        executors.diskIO().execute(() -> {
            try {
                List<TaskDao.TaskWithState> tasks = taskRepository.getAllTasksWithState();
                
                // Sync status with WorkManager for each task
                WorkManager wm = WorkManager.getInstance(this);
                for (TaskDao.TaskWithState task : tasks) {
                    try {
                        String tag = task.definition.getTaskType().name();
                        List<androidx.work.WorkInfo> workInfos = wm.getWorkInfosByTag(tag).get();
                        if (!workInfos.isEmpty()) {
                            androidx.work.WorkInfo info = workInfos.get(0);
                            Log.d("WorkerList", "Task " + tag + " WorkManager State: " + info.getState());
                        }
                    } catch (Exception e) {
                        Log.e("WorkerList", "Failed to sync WorkManager status for task: " + task.definition.getTaskId(), e);
                    }
                }
            } catch (Exception e) {
                Log.e("WorkerList", "Failed to sync WorkManager status", e);
            }
        });
    }
}
