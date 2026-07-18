package com.example.pp.data.repository;

import android.content.Context;

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

    public interface LoadCallback {
        void onLoaded(List<Vocabulary> vocabList);
    }

    public void getLatestVocab(int limit, int offset, LoadCallback callback) {
        executors.diskIO().execute(() -> {
            List<Vocabulary> list = vocabularyDao.getLatestVocab(limit, offset);
            executors.mainThread().execute(() -> callback.onLoaded(list));
        });
    }
}
