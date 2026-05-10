package com.example.pp.features.tracker.edit;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.example.pp.PPApplication;
import com.example.pp.R;
import com.example.pp.core.event.EventBus;
import com.example.pp.data.local.entity.TransactionEntity;
import com.example.pp.domain.model.CategoryTreeNode;
import com.example.pp.features.category.CategoryBottomSheetFragment;
import com.example.pp.features.category.TagBottomSheetFragment;
import com.example.pp.features.category.model.Tag;
import com.example.pp.features.settings.MoreActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class EditTrackerActivity extends AppCompatActivity {

    private MaterialCalendarView calendarView;
    private String selectedPayment = "UPI";

    private EditText etParticular;
    private EditText etAmount;
    private EditText etNote;
    private ImageView btnOpenCategorySelector;
    private TextView tvNoCategorySelected;
    private View containerSelectedCategory;
    private TextView tvSelectedCategory;
    private TextView tvSelectedCategoryIcon;
    private TextView tvSelectedDate;
    private TextView tvSelectedDay;
    private View btnAddTags;
    private TextView tvNoTagsSelected;
    private ChipGroup chipGroupSelectedTags;
    private ProgressBar progressBar;
    private TextView tvTypeExpense;
    private TextView tvTypeRefund;

    private CategoryTreeNode selectedCategory;
    private List<Tag> allTags = new ArrayList<>();
    private List<Tag> selectedTags = new ArrayList<>();
    private List<CategoryTreeNode> categories = new ArrayList<>();
    private String currentTransactionType = "Expense";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_tracker);

        calendarView = findViewById(R.id.calendarView);
        progressBar = findViewById(R.id.progressBar);

        setupCalendar();
        setupDropdowns();
        setupBottomNavigation();
        setupActions();
        setupNewFlow();
        observeData();
    }

    private void observeData() {
        PPApplication app = (PPApplication) getApplication();
        
        app.getCategoryRepository().getCategoryTree().observe(this, newCategories -> {
            this.categories = newCategories;
        });

        app.getTagRepository().getTags().observe(this, newTags -> {
            this.allTags = newTags;
        });

        EventBus.getInstance().observe().observe(this, event -> {
            if (event == EventBus.AppEvent.CATEGORIES_LOADED_FROM_DB) {
                Toast.makeText(this, "Categories loaded from Database", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupNewFlow() {
        etParticular = findViewById(R.id.etParticular);
        etAmount = findViewById(R.id.etAmount);
        etNote = findViewById(R.id.etNote);
        btnOpenCategorySelector = findViewById(R.id.btnOpenCategorySelector);
        tvNoCategorySelected = findViewById(R.id.tvNoCategorySelected);
        containerSelectedCategory = findViewById(R.id.containerSelectedCategory);
        tvSelectedCategory = findViewById(R.id.tvSelectedCategory);
        tvSelectedCategoryIcon = findViewById(R.id.tvSelectedCategoryIcon);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvSelectedDay = findViewById(R.id.tvSelectedDay);
        btnAddTags = findViewById(R.id.btnAddTags);
        tvNoTagsSelected = findViewById(R.id.tvNoTagsSelected);
        chipGroupSelectedTags = findViewById(R.id.chipGroupSelectedTags);

        ImageView btnClearParticular = findViewById(R.id.btnClearParticular);
        if (btnClearParticular != null && etParticular != null) {
            btnClearParticular.setOnClickListener(v -> etParticular.setText(""));
            etParticular.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    btnClearParticular.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });
        }

        if (btnOpenCategorySelector != null) btnOpenCategorySelector.setOnClickListener(v -> showCategoryBottomSheet());
        if (btnAddTags != null) btnAddTags.setOnClickListener(v -> showTagBottomSheet());
        if (containerSelectedCategory != null) containerSelectedCategory.setOnClickListener(v -> showCategoryBottomSheet());
    }

    private void showCategoryBottomSheet() {
        CategoryBottomSheetFragment bottomSheet = new CategoryBottomSheetFragment();
        bottomSheet.setCategories(categories);
        bottomSheet.setOnCategorySelectedListener(category -> {
            selectedCategory = category;
            updateSelectedCategoryUI();
        });
        bottomSheet.show(getSupportFragmentManager(), "CategoryBottomSheet");
    }

    private void updateSelectedCategoryUI() {
        if (selectedCategory == null) {
            if (tvNoCategorySelected != null) tvNoCategorySelected.setVisibility(View.VISIBLE);
            if (containerSelectedCategory != null) containerSelectedCategory.setVisibility(View.GONE);
            return;
        }
        if (tvNoCategorySelected != null) tvNoCategorySelected.setVisibility(View.GONE);
        if (containerSelectedCategory != null) containerSelectedCategory.setVisibility(View.VISIBLE);

        StringBuilder path = new StringBuilder(selectedCategory.getName());
        CategoryTreeNode parent = findParentNode(selectedCategory);
        if (parent != null) path.insert(0, parent.getName() + "  \u2192  ");

        if (tvSelectedCategory != null) tvSelectedCategory.setText(path.toString());
        if (tvSelectedCategoryIcon != null) tvSelectedCategoryIcon.setText(selectedCategory.getIcon());
    }

    private CategoryTreeNode findParentNode(CategoryTreeNode child) {
        for (CategoryTreeNode root : categories) {
            for (CategoryTreeNode c : root.getChildren()) {
                if (c.getCategoryId().equals(child.getCategoryId())) return root;
            }
        }
        return null;
    }

    private void showTagBottomSheet() {
        TagBottomSheetFragment bottomSheet = new TagBottomSheetFragment();
        bottomSheet.setTags(allTags);
        bottomSheet.setOnTagsAppliedListener(tags -> {
            this.selectedTags = tags;
            updateSelectedTagsUI(tags);
        });
        bottomSheet.show(getSupportFragmentManager(), "TagBottomSheet");
    }

    private void updateSelectedTagsUI(List<Tag> tags) {
        if (chipGroupSelectedTags == null) return;
        chipGroupSelectedTags.removeAllViews();
        if (tags == null || tags.isEmpty()) {
            if (tvNoTagsSelected != null) tvNoTagsSelected.setVisibility(View.VISIBLE);
            chipGroupSelectedTags.setVisibility(View.GONE);
            return;
        }
        if (tvNoTagsSelected != null) tvNoTagsSelected.setVisibility(View.GONE);
        chipGroupSelectedTags.setVisibility(View.VISIBLE);

        for (Tag tag : tags) {
            Chip chip = new Chip(this);
            chip.setText(tag.getName());
            chip.setChipBackgroundColor(ContextCompat.getColorStateList(this, R.color.tag_chip_background));
            chip.setChipStrokeColor(ContextCompat.getColorStateList(this, R.color.accent_purple));
            chip.setChipStrokeWidth(1f);
            chip.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            chip.setCloseIconVisible(true);
            chip.setCloseIconTint(ContextCompat.getColorStateList(this, R.color.text_muted));
            chip.setOnCloseIconClickListener(v -> {
                tag.setSelected(false);
                selectedTags.remove(tag);
                updateSelectedTagsUI(selectedTags);
            });
            chipGroupSelectedTags.addView(chip);
        }
    }

    private void setupCalendar() {
        if (calendarView == null) return;
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(date.getYear(), date.getMonth() - 1, date.getDay());
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM, yyyy", Locale.getDefault());
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
            if (tvSelectedDate != null) tvSelectedDate.setText(dateFormat.format(calendar.getTime()));
            if (tvSelectedDay != null) tvSelectedDay.setText(dayFormat.format(calendar.getTime()));
        });
        Calendar today = Calendar.getInstance();
        calendarView.setSelectedDate(CalendarDay.from(today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1, today.get(Calendar.DAY_OF_MONTH)));
    }

    private void setupDropdowns() {
        View paymentView = findViewById(R.id.paymentDropdown);
        if (paymentView != null) {
            ((TextView) paymentView.findViewById(R.id.dropdownText)).setText(selectedPayment);
            ((ImageView) paymentView.findViewById(R.id.dropdownIcon)).setImageResource(R.drawable.ic_credit_card);
            paymentView.setOnClickListener(v -> showPaymentBottomSheet(paymentView));
        }
    }

    private void showPaymentBottomSheet(View paymentView) {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.RoundedBottomSheetDialog);
        dialog.setContentView(R.layout.bottom_sheet_payment_options);
        View.OnClickListener optionClick = v -> {
            selectedPayment = ((TextView) v).getText().toString();
            ((TextView) paymentView.findViewById(R.id.dropdownText)).setText(selectedPayment);
            dialog.dismiss();
        };
        View upi = dialog.findViewById(R.id.optionUPI);
        View cash = dialog.findViewById(R.id.optionCASH);
        View icici = dialog.findViewById(R.id.optionICICI);
        View ama = dialog.findViewById(R.id.optionICICIAMA);
        View axis = dialog.findViewById(R.id.optionAXIS);
        if (upi != null) upi.setOnClickListener(optionClick);
        if (cash != null) cash.setOnClickListener(optionClick);
        if (icici != null) icici.setOnClickListener(optionClick);
        if (ama != null) ama.setOnClickListener(optionClick);
        if (axis != null) axis.setOnClickListener(optionClick);
        dialog.show();
    }

    private void setupBottomNavigation() {
        View back = findViewById(R.id.btnBack);
        View refresh = findViewById(R.id.btnRefresh);
        View fab = findViewById(R.id.btnCenterFab);
        if (back != null) back.setOnClickListener(v -> finish());
        if (refresh != null) refresh.setOnClickListener(v -> Toast.makeText(this, "Refreshing...", Toast.LENGTH_SHORT).show());
        if (fab != null) fab.setOnClickListener(v -> clearUI());

        View navMore = findViewById(R.id.navMore);
        if (navMore != null) {
            navMore.setOnClickListener(v -> {
                startActivity(new Intent(this, MoreActivity.class));
            });
        }
    }

    private void setupActions() {
        View done = findViewById(R.id.btnDoneForDay);
        View cancel = findViewById(R.id.btnCancel);
        View save = findViewById(R.id.btnSave);
        if (done != null) done.setOnClickListener(v -> Toast.makeText(this, "Marked done for the day", Toast.LENGTH_SHORT).show());
        if (cancel != null) cancel.setOnClickListener(v -> clearUI());
        if (save != null) save.setOnClickListener(v -> validateAndSave());

        tvTypeExpense = findViewById(R.id.tvTypeExpense);
        tvTypeRefund = findViewById(R.id.tvTypeRefund);

        if (tvTypeExpense != null && tvTypeRefund != null) {
            tvTypeExpense.setOnClickListener(v -> setTransactionType("Expense"));
            tvTypeRefund.setOnClickListener(v -> setTransactionType("Refund"));
        }
    }

    private void setTransactionType(String type) {
        currentTransactionType = type;
        if (type.equals("Expense")) {
            tvTypeExpense.setBackgroundResource(R.drawable.bg_type_selected);
            tvTypeExpense.setTextColor(ContextCompat.getColor(this, R.color.white));
            tvTypeRefund.setBackground(null);
            tvTypeRefund.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
        } else {
            tvTypeRefund.setBackgroundResource(R.drawable.bg_type_selected);
            tvTypeRefund.setTextColor(ContextCompat.getColor(this, R.color.white));
            tvTypeExpense.setBackground(null);
            tvTypeExpense.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
        }
    }

    private void validateAndSave() {
        if (etParticular == null || etAmount == null || etNote == null) return;

        String item = etParticular.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();
        String notes = etNote.getText().toString().trim();
        
        if (TextUtils.isEmpty(item)) { etParticular.setError("Required"); return; }
        if (TextUtils.isEmpty(amountStr)) { etAmount.setError("Required"); return; }
        if (selectedCategory == null) { Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show(); return; }

        double amount;
        try { amount = Double.parseDouble(amountStr); } catch (NumberFormatException e) { etAmount.setError("Invalid amount"); return; }

        progressBar.setVisibility(View.VISIBLE);
        
        CalendarDay date = calendarView.getSelectedDate();
        if (date == null) date = CalendarDay.today();
        
        Calendar cal = Calendar.getInstance();
        cal.set(date.getYear(), date.getMonth() - 1, date.getDay(), 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long transactionTimestamp = cal.getTimeInMillis();
        
        long now = System.currentTimeMillis();

        TransactionEntity transaction = new TransactionEntity(
            UUID.randomUUID().toString(),
            selectedCategory.getCategoryId(),
            item,
            amount,
            TextUtils.isEmpty(notes) ? null : notes,
            transactionTimestamp,
            now,
            selectedPayment,
            currentTransactionType
        );

        List<String> tagIds = new ArrayList<>();
        for (Tag tag : selectedTags) tagIds.add(tag.getId());

        PPApplication app = (PPApplication) getApplication();
        app.getTransactionRepository().saveTransaction(transaction, tagIds, () -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Entry saved successfully", Toast.LENGTH_SHORT).show();
            clearUI();
        }, () -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Failed to save entry", Toast.LENGTH_SHORT).show();
        });
    }

    private void clearUI() {
        if (etParticular != null) etParticular.setText("");
        if (etAmount != null) etAmount.setText("");
        if (etNote != null) etNote.setText("");
        selectedCategory = null;
        updateSelectedCategoryUI();
        selectedTags.clear();
        for (Tag t : allTags) t.setSelected(false);
        updateSelectedTagsUI(new ArrayList<>());
        setTransactionType("Expense");
        Calendar today = Calendar.getInstance();
        if (calendarView != null) calendarView.setSelectedDate(CalendarDay.from(today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1, today.get(Calendar.DAY_OF_MONTH)));
    }
}