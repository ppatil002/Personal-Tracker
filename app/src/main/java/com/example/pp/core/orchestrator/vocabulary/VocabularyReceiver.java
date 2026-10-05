package com.example.pp.core.orchestrator.vocabulary;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.pp.PPApplication;
import com.example.pp.core.constants.AppConstants;
import com.example.pp.core.enums.TaskStatus;
import com.example.pp.core.enums.TaskType;
import com.example.pp.core.task.AppExecutors;
import com.example.pp.data.local.AppDatabase;
import com.example.pp.data.local.entity.TaskDefinition;
import com.example.pp.data.local.entity.TaskState;
import com.example.pp.data.local.entity.Vocabulary;
import com.example.pp.data.repository.TaskRepository;
import com.example.pp.data.repository.VocabularyRepository;

public class VocabularyReceiver extends BroadcastReceiver {
    private static final String TAG = "VocabularyReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String taskId = intent.getStringExtra(VocabularyNotificationHelper.EXTRA_TASK_ID);
        String action = intent.getAction();
        long vocabId = intent.getLongExtra(VocabularyNotificationHelper.EXTRA_VOCAB_ID, -1);

        Log.i(TAG, "Vocabulary receiver triggered. Action: " + action + ", TaskId: " + taskId + ", VocabId: " + vocabId);

        if (AppConstants.ACTION_VOCABULARY_ALARM.equals(action)) {
            processAlarm(context, taskId);
        } else {
            handleUserAction(context, action, vocabId, taskId, intent);
        }
    }

    private void processAlarm(Context context, String taskId) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            Vocabulary word = VocabularyScheduler.getNextWordToRevise(context);
            if (word != null) {
                VocabularyNotificationHelper.showInitialNotification(context, word, taskId);
                updateTaskRunningState(context, taskId);
                VocabularyScheduler.logOperation(context, "RUNNING", "Vocab Revise", "Notification triggered for word: " + word.word);
            } else {
                Log.i(TAG, "No vocabulary available for revision. Skipping notification.");
                VocabularyScheduler.logOperation(context, "IDLE", "Vocab Revise", "No vocabulary available for revision.");
            }
        });
    }

    private void handleUserAction(Context context, String action, long vocabId, String taskId, Intent intent) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            Vocabulary vocab = (vocabId > 0) ? db.vocabularyDao().getVocabById(vocabId) : null;

            if (VocabularyNotificationHelper.ACTION_HINT.equals(action)) {
                if (vocab != null) {
                    VocabularyNotificationHelper.showHintNotification(context, vocab, taskId);
                }
            } else if (VocabularyNotificationHelper.ACTION_REMEMBER.equals(action)) {
                boolean hintUsed = intent.getBooleanExtra(VocabularyNotificationHelper.EXTRA_HINT_USED, false);
                if (vocab != null) {
                    VocabularyNotificationHelper.showRatingNotification(context, vocab, true, hintUsed, taskId);
                }
            } else if (VocabularyNotificationHelper.ACTION_FORGET.equals(action)) {
                boolean hintUsed = intent.getBooleanExtra(VocabularyNotificationHelper.EXTRA_HINT_USED, false);
                if (vocab != null) {
                    VocabularyNotificationHelper.showRatingNotification(context, vocab, false, hintUsed, taskId);
                }
            } else if (VocabularyNotificationHelper.ACTION_RATE.equals(action)) {
                boolean remembered = intent.getBooleanExtra(VocabularyNotificationHelper.EXTRA_REMEMBERED, false);
                boolean hintUsed = intent.getBooleanExtra(VocabularyNotificationHelper.EXTRA_HINT_USED, false);
                int rating = intent.getIntExtra(VocabularyNotificationHelper.EXTRA_RATING, 3);

                if (vocab != null) {
                    // Process review via VocabularyRepository (updates half-life & saves to DB)
                    new VocabularyRepository(context).processReview(vocab.id, rating, hintUsed, 0);

                    // Log SUCCESS operation and update task state
                    VocabularyScheduler.logOperation(context, "SUCCESS", "Vocab Revise",
                            "Feedback recorded for word: " + vocab.word + " (Rating: " + rating + "★, " + (remembered ? "Remembered" : "Forgotten") + ")");
                    updateTaskSuccessState(context, taskId);
                }

                // Dismiss notification and schedule next reminder
                VocabularyNotificationHelper.cancelNotification(context);
                VocabularyScheduler.schedule(context, null);
            }
        });
    }

    private void updateTaskRunningState(Context context, String taskId) {
        TaskRepository repo = ((PPApplication) context.getApplicationContext()).getTaskRepository();
        String resolvedTaskId = resolveTaskId(repo, taskId);
        if (resolvedTaskId != null) {
            TaskState state = repo.getStateForTask(resolvedTaskId);
            if (state != null) {
                state.setLastRunTime(System.currentTimeMillis());
                state.setCurrentState(TaskStatus.RUNNING);
                repo.updateState(state);
            }
        }
    }

    private void updateTaskSuccessState(Context context, String taskId) {
        TaskRepository repo = ((PPApplication) context.getApplicationContext()).getTaskRepository();
        String resolvedTaskId = resolveTaskId(repo, taskId);
        if (resolvedTaskId != null) {
            TaskState state = repo.getStateForTask(resolvedTaskId);
            if (state != null) {
                state.setLastRunTime(System.currentTimeMillis());
                state.setCurrentState(TaskStatus.COMPLETED);
                repo.updateState(state);
            }
        }
    }

    private String resolveTaskId(TaskRepository repo, String taskId) {
        if (taskId != null) return taskId;
        TaskDefinition def = repo.getDefinitionByType(TaskType.VOCABULARY_REVISION);
        return def != null ? def.getTaskId() : null;
    }
}
