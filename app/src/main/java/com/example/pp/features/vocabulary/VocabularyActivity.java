package com.example.pp.features.vocabulary;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.pp.domain.model.VocabularyCompletionState;

import com.example.pp.R;
import com.example.pp.data.local.entity.Vocabulary;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

public class VocabularyActivity extends AppCompatActivity {

    private VocabularyViewModel viewModel;
    private VocabularyAdapter adapter;
    private TextView tvAnalytics;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vocabulary);

        viewModel = new ViewModelProvider(this).get(VocabularyViewModel.class);

        initViews();
        observeViewModel();
    }

    private void initViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        tvAnalytics = findViewById(R.id.tvAnalytics);

        RecyclerView rvVocabulary = findViewById(R.id.rvVocabulary);
        rvVocabulary.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VocabularyAdapter();
        rvVocabulary.setAdapter(adapter);
        adapter.setOnItemClickListener(this::showAddVocabDialog);

        rvVocabulary.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    if (layoutManager != null) {
                        int visibleItemCount = layoutManager.getChildCount();
                        int totalItemCount = layoutManager.getItemCount();
                        int pastVisibleItems = layoutManager.findFirstVisibleItemPosition();

                        if ((visibleItemCount + pastVisibleItems) >= totalItemCount - 3) {
                            viewModel.loadNextPage();
                        }
                    }
                }
            }
        });

        findViewById(R.id.fabAddVocab).setOnClickListener(v -> showAddVocabDialog(null));
        findViewById(R.id.btnAIEnrich).setOnClickListener(v -> viewModel.enrichIncompleteVocabularies());
    }

    private void observeViewModel() {
        viewModel.getVocabItems().observe(this, items -> {
            adapter.setItems(items);
        });

        viewModel.getVocabularyAnalyticsState().observe(this, state -> {
            if (state == null) return;
            tvAnalytics.setText(String.format(Locale.getDefault(), "Words: %d/%d", state.loadedWords, state.totalWords));
        });

        viewModel.getVocabularyEnrichmentProgress().observe(this, progress -> {
            if (progress == null) return;

            if (progress.isCompleting) {
                if (progressDialog == null) {
                    progressDialog = new ProgressDialog(this);
                    progressDialog.setCancelable(false);
                    progressDialog.setTitle("Enriching Vocabulary");
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progressDialog.show();
                }

                int percentage = progress.totalWords > 0 ? (progress.currentWord * 100) / progress.totalWords : 0;
                progressDialog.setMessage("Processing: " + progress.currentWordName);
                progressDialog.setProgress(percentage);
                progressDialog.setMax(100);
            } else {
                // Enrichment completed
                if (progressDialog != null) {
                    progressDialog.dismiss();
                    progressDialog = null;
                }

                // Show completion summary
                if (progress.successCount > 0 || progress.failureCount > 0) {
                    String message = "Enrichment completed:\n" +
                            "✓ Success: " + progress.successCount + "\n" +
                            "✗ Failed: " + progress.failureCount;

                    if (!progress.failedWords.isEmpty()) {
                        message += "\n\nFailed words:\n" + String.join(", ", progress.failedWords);
                    }
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                } else if (progress.totalWords == 0) {
                    Toast.makeText(this, "All Words are Already Updated", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void showAddVocabDialog(Vocabulary existingVocab) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_vocab, null);
        EditText etWord = dialogView.findViewById(R.id.etWord);
        AutoCompleteTextView actvPartOfSpeech = dialogView.findViewById(R.id.actvPartOfSpeech);
        EditText etMeaning = dialogView.findViewById(R.id.etMeaning);
        EditText etExample = dialogView.findViewById(R.id.etExample);

        if (existingVocab != null) {
            etWord.setText(existingVocab.word);
            actvPartOfSpeech.setText(existingVocab.partOfSpeech);
            etMeaning.setText(existingVocab.meaning);
            etExample.setText(existingVocab.example);
        }

        String[] partsOfSpeech = {"Noun", "Verb", "Adjective", "Adverb"};
        ArrayAdapter<String> posAdapter = new ArrayAdapter<>(this, R.layout.item_dropdown_divider, R.id.tvDropdownItem, partsOfSpeech);
        actvPartOfSpeech.setAdapter(posAdapter);
        actvPartOfSpeech.setOnClickListener(v -> {
            hideKeyboard(v);
            actvPartOfSpeech.showDropDown();
        });
        actvPartOfSpeech.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                hideKeyboard(v);
                actvPartOfSpeech.showDropDown();
            }
        });

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(existingVocab == null ? "Add New Word" : "Edit Word")
                .setView(dialogView)
                .setPositiveButton("Save", (d, which) -> {
                    String rawWord = etWord.getText().toString().trim();
                    if (rawWord.isEmpty()) {
                        Toast.makeText(this, "Word is required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String word = rawWord.substring(0, 1).toUpperCase() + rawWord.substring(1).toLowerCase();
                    String pos = actvPartOfSpeech.getText().toString().trim();
                    String meaning = etMeaning.getText().toString().trim();
                    String example = etExample.getText().toString().trim();

                    if (existingVocab == null) {
                        viewModel.addVocabulary(word, meaning, example, pos);
                        Toast.makeText(this, "Word added", Toast.LENGTH_SHORT).show();
                    } else {
                        existingVocab.setWord(word);
                        existingVocab.setPartOfSpeech(pos);
                        existingVocab.setMeaning(meaning);
                        existingVocab.setExample(example);
                        viewModel.updateVocabulary(existingVocab);
                        Toast.makeText(this, "Word updated", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null);

        if (existingVocab != null) {
            builder.setNeutralButton("Delete", (d, which) -> {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Delete Word")
                        .setMessage("Are you sure you want to delete '" + existingVocab.word + "'?")
                        .setPositiveButton("Delete", (dialog, which1) -> {
                            viewModel.deleteVocabulary(existingVocab);
                            Toast.makeText(this, "Word deleted", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        AlertDialog dialog = builder.create();

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.color.card_background);
        }
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
