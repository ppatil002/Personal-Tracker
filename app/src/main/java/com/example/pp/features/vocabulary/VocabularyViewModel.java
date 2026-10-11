package com.example.pp.features.vocabulary;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.pp.PPApplication;
import com.example.pp.data.local.entity.Vocabulary;
import com.example.pp.data.repository.vocabulary.VocabularyRepository;
import com.example.pp.data.repository.vocabulary.callback.BatchWordCompletionProgressCallback;
import com.example.pp.domain.model.VocabularyAnalyticsState;
import com.example.pp.domain.model.VocabularyCompletionState;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VocabularyViewModel extends AndroidViewModel {
    private static final int MANUAL_VOCABULARY_BATCH_SIZE = 50;

    private final VocabularyRepository repository;
    private final MutableLiveData<List<VocabularyAdapter.ListItem>> vocabItems = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<VocabularyAnalyticsState> vocabularyAnalyticsState = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<VocabularyCompletionState> vocabularyEnrichmentProgress = new MutableLiveData<>(VocabularyCompletionState.idle());

    private int currentOffset = 0;
    private final int PAGE_SIZE = 20;
    private boolean isLastPage = false;
    private int totalLoadedWordsCount = 0;
    private String lastDate = "";

    public VocabularyViewModel(@NonNull Application application) {
        super(application);
        this.repository = ((PPApplication) application).getVocabularyRepository();
        loadNextPage();
    }

    public LiveData<List<VocabularyAdapter.ListItem>> getVocabItems() {
        return vocabItems;
    }

    public LiveData<VocabularyAnalyticsState> getVocabularyAnalyticsState() {
        return vocabularyAnalyticsState;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<VocabularyCompletionState> getVocabularyEnrichmentProgress() {
        return vocabularyEnrichmentProgress;
    }

    public void loadNextPage() {
        if (Boolean.TRUE.equals(isLoading.getValue()) || isLastPage) return;

        isLoading.setValue(true);
        repository.loadVocabularyChunk(PAGE_SIZE, currentOffset, (vocabList, totalWords) -> {
            if (vocabList == null || vocabList.isEmpty()) {
                isLastPage = true;
                isLoading.setValue(false);
                vocabularyAnalyticsState.setValue(new VocabularyAnalyticsState(totalLoadedWordsCount, totalWords));
                return;
            }

            totalLoadedWordsCount += vocabList.size();
            List<VocabularyAdapter.ListItem> newItems = processVocabList(vocabList);
            List<VocabularyAdapter.ListItem> currentItems = vocabItems.getValue();
            List<VocabularyAdapter.ListItem> updated = new ArrayList<>(currentItems != null ? currentItems : new ArrayList<>());
            updated.addAll(newItems);
            vocabItems.setValue(updated);

            currentOffset += PAGE_SIZE;
            if (vocabList.size() < PAGE_SIZE) {
                isLastPage = true;
            }

            vocabularyAnalyticsState.setValue(new VocabularyAnalyticsState(totalLoadedWordsCount, totalWords));
            isLoading.setValue(false);
        });
    }

    private List<VocabularyAdapter.ListItem> processVocabList(List<Vocabulary> rawList) {
        List<VocabularyAdapter.ListItem> items = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, yyyy", Locale.getDefault());
        String today = sdf.format(new Date());
        String yesterday = sdf.format(new Date(System.currentTimeMillis() - 86400000));

        for (Vocabulary v : rawList) {
            String dateStr = sdf.format(new Date(v.createdTimestamp));
            if (dateStr.equals(today)) dateStr = "Today";
            else if (dateStr.equals(yesterday)) dateStr = "Yesterday";

            if (!dateStr.equals(lastDate)) {
                items.add(new VocabularyAdapter.HeaderItem(dateStr));
                lastDate = dateStr;
            }
            items.add(new VocabularyAdapter.WordItem(v));
        }
        return items;
    }

    public void addVocabulary(String word, String meaning, String example, String partOfSpeech) {
        repository.addVocabulary(word, meaning, example, partOfSpeech, System.currentTimeMillis());
        refresh();
    }

    public void updateVocabulary(Vocabulary vocabulary) {
        repository.updateVocabulary(vocabulary);
        refresh();
    }

    public void deleteVocabulary(Vocabulary vocabulary) {
        repository.deleteVocabulary(vocabulary);
        refresh();
    }

    public void enrichIncompleteVocabularies() {
        vocabularyEnrichmentProgress.setValue(VocabularyCompletionState.started(MANUAL_VOCABULARY_BATCH_SIZE));

        repository.completeIncompleteVocabulariesWithProgress(MANUAL_VOCABULARY_BATCH_SIZE,
            new BatchWordCompletionProgressCallback() {
                @Override
                public void onProgress(int current, int total, String wordName, int successCount, int failureCount, List<String> failedWords) {
                    vocabularyEnrichmentProgress.setValue(VocabularyCompletionState.processing(current, total, wordName, successCount, failureCount, failedWords));
                }

                @Override
                public void onComplete(int successCount, int failureCount, List<String> failedWords) {
                    vocabularyEnrichmentProgress.setValue(VocabularyCompletionState.completed(successCount, failureCount, failedWords));
                    refresh();
                }

                @Override
                public void onError(Throwable throwable) {
                    vocabularyEnrichmentProgress.setValue(VocabularyCompletionState.completed(0, 0, new ArrayList<>()));
                }

                @Override
                public void onEmpty() {
                    vocabularyEnrichmentProgress.setValue(VocabularyCompletionState.onEmpty());
                }

            });
    }

    public void refresh() {
        currentOffset = 0;
        isLastPage = false;
        totalLoadedWordsCount = 0;
        lastDate = "";
        vocabItems.setValue(new ArrayList<>());
        loadNextPage();
    }
}
