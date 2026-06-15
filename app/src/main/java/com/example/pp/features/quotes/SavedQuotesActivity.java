package com.example.pp.features.quotes;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pp.PPApplication;
import com.example.pp.R;
import com.example.pp.data.local.entity.SavedQuote;
import com.example.pp.data.repository.QuoteRepository;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class SavedQuotesActivity extends AppCompatActivity {
    private QuoteRepository quoteRepository;
    private SavedQuotesAdapter adapter;
    private RecyclerView rvQuotes;
    private FlexboxLayout flexCategories;
    private TextView tvPagination;
    private ImageButton btnPrev, btnNext;

    private int currentPage = 1;
    private final int pageSize = 10;
    private int totalPages = 1;
    private List<String> selectedCategoriesList = new ArrayList<>();
    private LiveData<List<SavedQuote>> quotesLiveData;
    private LiveData<Integer> countLiveData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_quotes);

        quoteRepository = ((PPApplication) getApplication()).getQuoteRepository();
        initViews();
        loadCategories();
        loadQuotes();
    }

    private void initViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        rvQuotes = findViewById(R.id.rvSavedQuotes);
        flexCategories = findViewById(R.id.flexCategories);
        tvPagination = findViewById(R.id.tvPaginationInfo);
        btnPrev = findViewById(R.id.btnPrevPage);
        btnNext = findViewById(R.id.btnNextPage);

        rvQuotes.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SavedQuotesAdapter();
        rvQuotes.setAdapter(adapter);

        // Add slide-up animation for items
        rvQuotes.setItemAnimator(new androidx.recyclerview.widget.DefaultItemAnimator());

        adapter.setListener(new SavedQuotesAdapter.OnQuoteActionListener() {
            @Override
            public void onEdit(SavedQuote quote) {
                showEditDialog(quote);
            }

            @Override
            public void onDelete(SavedQuote quote) {
                new MaterialAlertDialogBuilder(SavedQuotesActivity.this)
                        .setTitle("Delete Quote")
                        .setMessage("Are you sure you want to delete this quote?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            quoteRepository.deleteQuote(quote.quote);
                            loadQuotes();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        btnPrev.setOnClickListener(v -> {
            if (currentPage > 1) {
                currentPage--;
                loadQuotes();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (currentPage < totalPages) {
                currentPage++;
                loadQuotes();
            }
        });
    }

    private void loadCategories() {
        quoteRepository.getAllDistinctCategories().observe(this, categories -> {
            flexCategories.removeAllViews();
            if (categories == null) return;

            for (String cat : categories) {
                Chip chip = new Chip(new ContextThemeWrapper(this, R.style.savedQuotesFilterChips));
                chip.setText(cat.toUpperCase());
                boolean isChecked = selectedCategoriesList.contains(cat);

                if (isChecked) {
                    chip.setChipBackgroundColor(ColorStateList.valueOf(getColor(R.color.accent_purple)));
                    chip.setTextColor(Color.WHITE);
                } else {
                    chip.setChipBackgroundColor(ColorStateList.valueOf(getColor(R.color.elevated_surface)));
                    chip.setTextColor(getColor(R.color.text_secondary));
                }
                
                chip.setOnClickListener(v -> {
                    if (selectedCategoriesList.contains(cat)) {
                        selectedCategoriesList.remove(cat);
                    } else {
                        selectedCategoriesList.add(cat);
                    }
                    currentPage = 1;
                    loadCategories(); // Refresh chip UI
                    loadQuotes();
                });
                
                // Spacing
                FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                int marginHorizontal = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
                int marginVertical = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
                params.setMargins(marginHorizontal, marginVertical, marginHorizontal, marginVertical);
                chip.setLayoutParams(params);

                flexCategories.addView(chip);
            }
        });
    }

    private void showEditDialog(SavedQuote savedQuote) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_save_quote, null);
        TextView tvQuotePreview = dialogView.findViewById(R.id.tvQuotePreview);
        ChipGroup cgSelectedCategories = dialogView.findViewById(R.id.cgSelectedCategories);
        AutoCompleteTextView actvCategory = dialogView.findViewById(R.id.actvCategoryFilter);
        EditText etNotes = dialogView.findViewById(R.id.etQuoteNotes);

        tvQuotePreview.setText("“ " + savedQuote.quote + " ”\n\n— " + savedQuote.author);
        etNotes.setText(savedQuote.notes);

        List<String> selectedCategories = new ArrayList<>();
        if (savedQuote.category != null && !savedQuote.category.isEmpty()) {
            String[] cats = savedQuote.category.split(", ");
            for (String cat : cats) {
                addCategoryChipForEdit(cat, cgSelectedCategories, selectedCategories);
            }
        }

        quoteRepository.getAllDistinctCategories().observe(this, categories -> {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_dropdown_divider, R.id.tvDropdownItem, categories);
            actvCategory.setAdapter(adapter);
            actvCategory.setOnClickListener(v -> actvCategory.showDropDown());
            actvCategory.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus && actvCategory.getAdapter() != null) {
                    actvCategory.postDelayed(actvCategory::showDropDown, 200);
                }
            });
            actvCategory.setOnItemClickListener((parent, view, position, id) -> {
                String cat = (String) parent.getItemAtPosition(position);
                addCategoryChipForEdit(cat, cgSelectedCategories, selectedCategories);
                actvCategory.setText("");
            });
        });

        actvCategory.setOnEditorActionListener((v, actionId, event) -> {
            String cat = actvCategory.getText().toString().trim();
            if (!cat.isEmpty()) {
                addCategoryChipForEdit(cat, cgSelectedCategories, selectedCategories);
                actvCategory.setText("");
                return true;
            }
            return false;
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Edit Quote")
                .setView(dialogView)
                .setPositiveButton("Update", (d, which) -> {
                    String notes = etNotes.getText().toString().trim();
                    quoteRepository.updateQuote(savedQuote.quote, savedQuote.quote, savedQuote.author, selectedCategories, notes);
                    Toast.makeText(this, "Quote updated!", Toast.LENGTH_SHORT).show();
                    loadQuotes();
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.color.card_background);
        }
    }

    private void addCategoryChipForEdit(String category, ChipGroup chipGroup, List<String> selectedList) {
        String catClean = category.trim();
        if (catClean.isEmpty() || selectedList.contains(catClean)) return;

        selectedList.add(catClean);
        Chip chip = new Chip(new ContextThemeWrapper(this, R.style.QuotesCategoryChip), null, 0);
        chip.setText(catClean.toUpperCase());
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            chipGroup.removeView(chip);
            selectedList.remove(catClean);
        });
        chipGroup.addView(chip);
    }

    private void loadQuotes() {
        if (quotesLiveData != null) quotesLiveData.removeObservers(this);
        if (countLiveData != null) countLiveData.removeObservers(this);

        quotesLiveData = quoteRepository.getSavedQuotes(currentPage, pageSize, selectedCategoriesList);
        countLiveData = quoteRepository.getSavedQuotesCount(selectedCategoriesList);

        quotesLiveData.observe(this, quotes -> adapter.setQuotes(quotes));
        countLiveData.observe(this, totalCount -> {
            totalPages = (int) Math.ceil((double) totalCount / pageSize);
            if (totalPages == 0) totalPages = 1;
            tvPagination.setText(currentPage + " / " + totalPages);
            updatePaginationButtons();
        });
    }

    private void updatePaginationButtons() {
        btnPrev.setEnabled(currentPage > 1);
        btnPrev.setAlpha(currentPage > 1 ? 1.0f : 0.5f);
        btnNext.setEnabled(currentPage < totalPages);
        btnNext.setAlpha(currentPage < totalPages ? 1.0f : 0.5f);
    }
}
