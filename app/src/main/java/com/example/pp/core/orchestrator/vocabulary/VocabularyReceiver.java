package com.example.pp.core.orchestrator.vocabulary;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.pp.PPApplication;
import com.example.pp.core.constants.AppConstants;
import com.example.pp.core.task.AppExecutors;
import com.example.pp.data.local.AppDatabase;
import com.example.pp.data.local.entity.TaskState;
import com.example.pp.data.local.entity.Vocabulary;
import com.example.pp.data.local.entity.VocabularyRevisionLog;
import com.example.pp.data.repository.TaskRepository;

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
                updateTaskLastRun(context, taskId);
            } else {
                Log.i(TAG, "No vocabulary available for revision. Skipping notification.");
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
                    long now = System.currentTimeMillis();
                    double halfLifeBefore = vocab.halfLife;
                    double halfLifeAfter = halfLifeBefore; // Half-Life Regression model update in next step

                    // 1. Insert append-only revision log
                    VocabularyRevisionLog revisionLog = new VocabularyRevisionLog(
                            vocab.id,
                            now,
                            remembered ? "REMEMBER" : "FORGET",
                            hintUsed,
                            rating,
                            halfLifeBefore,
                            halfLifeAfter
                    );
                    db.vocabularyRevisionLogDao().insert(revisionLog);

                    // 2. Update vocabulary record
                    vocab.lastReviewedTimestamp = now;
                    if (remembered) {
                        vocab.correctAttempts++;
                    } else {
                        vocab.incorrectAttempts++;
                    }
                    if (hintUsed) {
                        vocab.hintsUsedCount++;
                    }
                    vocab.nextRevisionTimestamp = VocabularyScheduler.calculateNextTriggerTime(now);

                    db.vocabularyDao().insert(vocab);
                    Log.i(TAG, "Recorded revision log and updated vocabulary for word: " + vocab.word);
                }

                // 3. Dismiss notification and schedule next reminder
                VocabularyNotificationHelper.cancelNotification(context);
                VocabularyScheduler.schedule(context, null);
            }
        });
    }

    private void updateTaskLastRun(Context context, String taskId) {
        if (taskId == null) return;
        TaskRepository repo = ((PPApplication) context.getApplicationContext()).getTaskRepository();
        TaskState state = repo.getStateForTask(taskId);
        if (state != null) {
            state.setLastRunTime(System.currentTimeMillis());
            repo.updateState(state);
        }
    }
}
