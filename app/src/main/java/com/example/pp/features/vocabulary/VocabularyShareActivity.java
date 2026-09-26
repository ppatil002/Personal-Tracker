package com.example.pp.features.vocabulary;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.pp.PPApplication;
import com.example.pp.core.constants.AppConstants;
import com.example.pp.data.repository.VocabularyRepository;


public class VocabularyShareActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && "text/plain".equals(type)) {
            handleSharedText(intent);
        } else {
            finish();
        }
    }

    private void handleSharedText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            String trimmed = sharedText.trim();
            if (trimmed.isEmpty()) {
                finish();
                return;
            }

            VocabularyRepository repository = ((PPApplication) getApplication()).getVocabularyRepository();
            String[] words = trimmed.split("\\s+");

            if (isPartOfSpeech(trimmed)) {
                repository.addPartOfSpeechToLastWord(trimmed, (status, lastWord) -> showResultToast(status, lastWord));
            } else if (words.length > 3) {
                repository.addMeaningToLastWord(trimmed, (status, lastWord) -> showResultToast(status, lastWord));
            } else if (trimmed.length() > 20){
                showResultToast(AppConstants.VOCABULARY_STATUS_WORD_LENGTH_EXCEED, trimmed);
            }
            else {
                String word = trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1).toLowerCase();
                repository.addWordIfNotExist(word, (status, w) -> showResultToast(status, w));
            }
        } else {
            finish();
        }
    }

    private boolean isPartOfSpeech(String text) {
        String t = text.trim().toLowerCase();
        return t.equals("noun") || t.equals("verb") || t.equals("adjective") || t.equals("adverb");
    }

    private void showResultToast(int status, String word) {
        String message;
        switch (status) {
            case AppConstants.VOCABULARY_STATUS_ADDED:
                message = "✅ \"" + word + "\" added to vocabulary";
                break;
            case AppConstants.VOCABULARY_STATUS_ALREADY_EXISTS:
                message = "⚠️ \"" + word + "\" already in vocabulary";
                break;
            case AppConstants.VOCABULARY_STATUS_MEANING_ADDED:
                message = "📝 Meaning added to \"" + word + "\"";
                break;
            case AppConstants.VOCABULARY_STATUS_MEANING_SKIPPED:
                message = "ℹ️ Meaning already exists for \"" + word + "\"";
                break;
            case AppConstants.VOCABULARY_STATUS_POS_ADDED:
                message = "🏷️ Part of speech added to \"" + word + "\"";
                break;
            case AppConstants.VOCABULARY_STATUS_POS_SKIPPED:
                message = "ℹ️ Part of speech already exists for \"" + word + "\"";
                break;
            case AppConstants.VOCABULARY_STATUS_NO_LAST_WORD:
                message = "❌ No word found to add details to";
                break;
            case AppConstants.VOCABULARY_STATUS_WORD_LENGTH_EXCEED:
                message = "⚠️ \"" + word + "\" limit exceeded";
                break;
            default:
                message = "Done";
        }
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        finish();
    }
}
