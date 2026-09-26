package com.example.pp.features.vocabulary;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pp.R;
import com.example.pp.data.local.entity.Vocabulary;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class VocabularyActivity extends AppCompatActivity {

    private VocabularyViewModel viewModel;
    private VocabularyAdapter adapter;

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
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int pastVisibleItems = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                        viewModel.loadNextPage();
                    }
                }
            }
        });

        findViewById(R.id.fabAddVocab).setOnClickListener(v -> showAddVocabDialog(null));
    }

    private void observeViewModel() {
        viewModel.getVocabItems().observe(this, items -> {
            adapter.setItems(items);
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

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
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
                .setNegativeButton("Cancel", null)
                .create();

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
}
