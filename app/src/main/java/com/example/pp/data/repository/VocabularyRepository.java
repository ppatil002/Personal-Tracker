package com.example.pp.data.repository;

import android.content.Context;

import com.example.pp.core.constants.AppConstants;
import com.example.pp.core.task.AppExecutors;
import com.example.pp.data.local.AppDatabase;
import com.example.pp.data.local.dao.VocabularyDao;
import com.example.pp.data.local.entity.Vocabulary;

import java.util.List;

public class VocabularyRepository {
    private final VocabularyDao vocabularyDao;
    private final AppExecutors executors;

    public VocabularyRepository(Context context) {
        this.vocabularyDao = AppDatabase.getInstance(context).vocabularyDao();
        this.executors = AppExecutors.getInstance();
    }

    public void addVocabulary(String word, String meaning, String example, String partOfSpeech, long timestamp) {
        executors.diskIO().execute(() -> {
            vocabularyDao.insert(new Vocabulary(word, meaning, example, partOfSpeech, timestamp));
        });
    }

    public void updateVocabulary(Vocabulary vocabulary) {
        executors.diskIO().execute(() -> {
            vocabularyDao.insert(vocabulary);
        });
    }

    public void deleteVocabulary(Vocabulary vocabulary) {
        executors.diskIO().execute(() -> {
            vocabularyDao.delete(vocabulary);
        });
    }

    public interface WordCheckCallback {
        void onResult(int status, String word);
    }

    public void addWordIfNotExist(String word, WordCheckCallback callback) {
        executors.diskIO().execute(() -> {
            Vocabulary existing = vocabularyDao.getVocabByWord(word);
            if (existing == null) {
                vocabularyDao.insert(new Vocabulary(word, "", "", "", System.currentTimeMillis()));
                executors.mainThread().execute(() -> callback.onResult(AppConstants.VOCABULARY_STATUS_ADDED, word));
            } else {
                executors.mainThread().execute(() -> callback.onResult(AppConstants.VOCABULARY_STATUS_ALREADY_EXISTS, word));
            }
        });
    }

    public void addMeaningToLastWord(String rawMeaning, WordCheckCallback callback) {
        executors.diskIO().execute(() -> {
            Vocabulary last = vocabularyDao.getLastAddedVocab();
            if (last == null) {
                executors.mainThread().execute(() -> callback.onResult(AppConstants.VOCABULARY_STATUS_NO_LAST_WORD, ""));
                return;
            }

            if (last.meaning != null && !last.meaning.trim().isEmpty()) {
                executors.mainThread().execute(() -> callback.onResult(AppConstants.VOCABULARY_STATUS_MEANING_SKIPPED, last.word));
                return;
            }

            String meaning = rawMeaning.trim();
            // Remove ending full stop
            if (meaning.endsWith(".")) {
                meaning = meaning.substring(0, meaning.length() - 1).trim();
            }
            // Remove quotes
            meaning = meaning.replaceAll("^['\"]|['\"]$", "").trim();

            last.meaning = meaning;
            vocabularyDao.insert(last);
            executors.mainThread().execute(() -> callback.onResult(AppConstants.VOCABULARY_STATUS_MEANING_ADDED, last.word));
        });
    }

    public void addPartOfSpeechToLastWord(String rawPos, WordCheckCallback callback) {
        executors.diskIO().execute(() -> {
            Vocabulary last = vocabularyDao.getLastAddedVocab();
            if (last == null) {
                executors.mainThread().execute(() -> callback.onResult(AppConstants.VOCABULARY_STATUS_NO_LAST_WORD, ""));
                return;
            }

            if (last.partOfSpeech != null && !last.partOfSpeech.trim().isEmpty()) {
                executors.mainThread().execute(() -> callback.onResult(AppConstants.VOCABULARY_STATUS_POS_SKIPPED, last.word));
                return;
            }

            String pos = rawPos.trim().substring(0, 1).toUpperCase() + rawPos.trim().substring(1).toLowerCase();
            last.partOfSpeech = pos;
            vocabularyDao.insert(last);
            executors.mainThread().execute(() -> callback.onResult(AppConstants.VOCABULARY_STATUS_POS_ADDED, last.word));
        });
    }

    public interface LoadCallback {
        void onLoaded(List<Vocabulary> vocabList);
    }

    public interface ChunkLoadCallback {
        void onLoaded(List<Vocabulary> vocabList, int totalWords);
    }

    public void getVocabChunk(int limit, int offset, ChunkLoadCallback callback) {
        executors.diskIO().execute(() -> {
            List<Vocabulary> list = vocabularyDao.getLatestVocab(limit, offset);
            int totalWords = vocabularyDao.getCount();
            executors.mainThread().execute(() -> callback.onLoaded(list, totalWords));
        });
    }
}
