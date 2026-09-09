package com.example.pp.features.tracker.edit;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pp.PPApplication;
import com.example.pp.R;
import com.example.pp.core.enums.AppEvent;
import com.example.pp.core.event.EventBus;
import com.example.pp.data.local.entity.TransactionEntity;
import com.example.pp.data.repository.CategoryRepository;
import com.example.pp.data.repository.RegistryRepository;
import com.example.pp.data.repository.TagRepository;
import com.example.pp.data.repository.TransactionRepository;
import com.example.pp.domain.model.CategoryTreeNode;
import com.example.pp.features.analytics.AnalyticsActivity;
import com.example.pp.features.category.CategoryBottomSheetFragment;
import com.example.pp.features.category.TagBottomSheetFragment;
import com.example.pp.features.category.model.Tag;
import com.example.pp.features.dashboard.DashboardActivity;
import com.example.pp.features.settings.MoreActivity;
import com.example.pp.utils.DateUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import com.google.gson.Gson;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    private TextView tvSyncStatus;
    private View btnAddTags;
    private TextView tvNoTagsSelected;
    private ChipGroup chipGroupSelectedTags;
    private ProgressBar progressBar;
    private TextView tvTypeExpense;
    private TextView tvTypeRefund;
    private View btnInfoTransactions;
    private View containerDayTransactions;
    private RecyclerView rvDayTransactions;
    private SmallTransactionAdapter dayTransactionsAdapter;
    private View btnSave;
    private View btnCancel;

    private TransactionRepository transactionRepository;
    private TagRepository tagRepository;
    private CategoryRepository categoryRepository;
    private RegistryRepository registryRepository;

    private CategoryTreeNode selectedCategory;
    private List<Tag> allTags = new ArrayList<>();
    private List<Tag> selectedTags = new ArrayList<>();
    private List<CategoryTreeNode> categories = new ArrayList<>();
    private String currentTransactionType = "EXPENSE";
    private TransactionEntity editingTransaction;
    private Map<String, List<String>> autoTagMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_tracker);

        PPApplication app = (PPApplication) getApplication();
        transactionRepository = app.getTransactionRepository();
        tagRepository = app.getTagRepository();
        categoryRepository = app.getCategoryRepository();
        registryRepository = app.getRegistryRepository();

        calendarView = findViewById(R.id.calendarView);
        progressBar = findViewById(R.id.progressBar);

        initAutoTagMap();
        setupNewFlow();
        setupCalendar();
        setupDropdowns();
        setupBottomNavigation();
        setupActions();
        observeData();
    }

    private void observeData() {
        categoryRepository.getCategoryTree().observe(this, newCategories -> {
            this.categories = newCategories;
        });

        tagRepository.getTags().observe(this, newTags -> {
            this.allTags = newTags;
        });

        EventBus.getInstance().observe().observe(this, event -> {
            if (event == AppEvent.CATEGORIES_LOADED_FROM_DB) {
                Toast.makeText(this, "Categories loaded from Database", Toast.LENGTH_SHORT).show();
            }
        });

        registryRepository.getRegistryItemLive("expense_sync_date").observe(this, entity -> {
            if (entity != null && tvSyncStatus != null) {
                try {
                    SyncStatus syncStatus = new Gson().fromJson(entity.value, SyncStatus.class);
                    String dateStr = syncStatus != null ? syncStatus.date : "--";
                    tvSyncStatus.setText("Sync: " + dateStr);
                } catch (Exception e) {
                    tvSyncStatus.setText("Sync: --");
                }
            }
        });
    }

    private void setupNewFlow() {
        etParticular = findViewById(R.id.etParticular);
        etAmount = findViewById(R.id.etAmount);
        etNote = findViewById(R.id.etNote);

        if (etParticular != null) {
            etParticular.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    applyAutoTags(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        btnOpenCategorySelector = findViewById(R.id.btnOpenCategorySelector);
        tvNoCategorySelected = findViewById(R.id.tvNoCategorySelected);
        containerSelectedCategory = findViewById(R.id.containerSelectedCategory);
        tvSelectedCategory = findViewById(R.id.tvSelectedCategory);
        tvSelectedCategoryIcon = findViewById(R.id.tvSelectedCategoryIcon);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvSelectedDay = findViewById(R.id.tvSelectedDay);
        tvSyncStatus = findViewById(R.id.tvSyncStatus);
        btnAddTags = findViewById(R.id.btnAddTags);
        tvNoTagsSelected = findViewById(R.id.tvNoTagsSelected);
        chipGroupSelectedTags = findViewById(R.id.chipGroupSelectedTags);

        btnInfoTransactions = findViewById(R.id.btnInfoTransactions);
        containerDayTransactions = findViewById(R.id.containerDayTransactions);
        rvDayTransactions = findViewById(R.id.rvDayTransactions);

        if (rvDayTransactions != null) {
            rvDayTransactions.setLayoutManager(new LinearLayoutManager(this));
            dayTransactionsAdapter = new SmallTransactionAdapter();
            dayTransactionsAdapter.setOnItemClickListener(this::editTransaction);
            rvDayTransactions.setAdapter(dayTransactionsAdapter);
        }

        if (btnInfoTransactions != null) {
            btnInfoTransactions.setOnClickListener(v -> toggleDayTransactions());
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

    private void editTransaction(TransactionEntity transaction) {
        this.editingTransaction = transaction;

        // Prepopulate fields
        if (etParticular != null) etParticular.setText(transaction.getItem());
        if (etAmount != null) etAmount.setText(String.valueOf(Math.abs(transaction.getAmount())));
        if (etNote != null) etNote.setText(transaction.getNotes());

        // Set category
        selectedCategory = findCategoryById(transaction.getCategoryId());
        updateSelectedCategoryUI();

        // Set date
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(transaction.getTransactionDate());
        CalendarDay date = CalendarDay.from(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));
        calendarView.setSelectedDate(date);
        updateDateDisplay(date);

        // Set transaction type
        setTransactionType(transaction.getTransactionType());

        // Set payment method
        selectedPayment = transaction.getPaymentMethod();
        View paymentView = findViewById(R.id.paymentDropdown);
        if (paymentView != null) {
            ((TextView) paymentView.findViewById(R.id.dropdownText)).setText(selectedPayment);
        }

        // Load and set tags
        transactionRepository.getTagIdsForTransaction(transaction.getTransactionId(), tagIds -> {
            selectedTags.clear();
            for (Tag tag : allTags) {
                if (tagIds.contains(tag.getId())) {
                    tag.setSelected(true);
                    selectedTags.add(tag);
                } else {
                    tag.setSelected(false);
                }
            }
            updateSelectedTagsUI(selectedTags);
        });

        // Update button texts
        if (btnSave instanceof TextView) ((TextView) btnSave).setText("Update");
        if (btnCancel instanceof TextView) ((TextView) btnCancel).setText("Delete");
    }

    private CategoryTreeNode findCategoryById(String categoryId) {
        for (CategoryTreeNode root : categories) {
            if (root.getCategoryId().equals(categoryId)) return root;
            for (CategoryTreeNode child : root.getChildren()) {
                if (child.getCategoryId().equals(categoryId)) return child;
            }
        }
        return null;
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
        long timestamp = getSelectedDateTimestamp();
        List<Tag> filteredTags = filterTagsByDate(allTags, timestamp);

        TagBottomSheetFragment bottomSheet = new TagBottomSheetFragment();
        bottomSheet.setTags(filteredTags);
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

        long timestamp = getSelectedDateTimestamp();
        List<Tag> validSelectedTags = filterTagsByDate(tags, timestamp);

        if (validSelectedTags.isEmpty()) {
            if (tvNoTagsSelected != null) tvNoTagsSelected.setVisibility(View.VISIBLE);
            chipGroupSelectedTags.setVisibility(View.GONE);
            return;
        }

        if (tvNoTagsSelected != null) tvNoTagsSelected.setVisibility(View.GONE);
        chipGroupSelectedTags.setVisibility(View.VISIBLE);

        float strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, getResources().getDisplayMetrics());

        for (Tag tag : validSelectedTags) {
            Chip chip = new Chip(this);
            chip.setText(tag.getName());
            
            chip.setChipBackgroundColorResource(R.color.tag_chip_background);
            chip.setChipStrokeColorResource(R.color.accent_purple);
            chip.setChipStrokeWidth(strokeWidth);
            chip.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            chip.setCloseIconVisible(true);
            chip.setCloseIconTint(ContextCompat.getColorStateList(this, R.color.text_muted));
            chip.setOnCloseIconClickListener(v -> {
                Log.d("EditTrackerActivity", "Removing tag: " + tag.getName());
                tag.setSelected(false);
                selectedTags.remove(tag);
                updateSelectedTagsUI(selectedTags);
            });

            chipGroupSelectedTags.addView(chip);
        }
    }

    private long getSelectedDateTimestamp() {
        CalendarDay date = calendarView.getSelectedDate();
        if (date == null) date = CalendarDay.today();
        Calendar cal = Calendar.getInstance();
        // date.getMonth() is 1-based, Calendar.set expects 0-based month
        cal.set(date.getYear(), date.getMonth() - 1, date.getDay(), 12, 0, 0);
        return cal.getTimeInMillis();
    }

    private List<Tag> filterTagsByDate(List<Tag> tagsToFilter, long timestamp) {
        List<Tag> filtered = new ArrayList<>();
        for (Tag tag : tagsToFilter) {
            boolean validFrom = tag.getValidFrom() == null || timestamp >= tag.getValidFrom();
            boolean validTill = tag.getValidTill() == null || timestamp <= tag.getValidTill();
            if (validFrom && validTill) {
                filtered.add(tag);
            }
        }
        return filtered;
    }

    private void setupCalendar() {
        if (calendarView == null) return;
        calendarView.setOnDateChangedListener((widget, date, selected) -> updateDateDisplay(date));
        
        Calendar today = Calendar.getInstance();
        CalendarDay todayDay = CalendarDay.from(today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1, today.get(Calendar.DAY_OF_MONTH));
        calendarView.setSelectedDate(todayDay);
        updateDateDisplay(todayDay);
    }

    private void updateDateDisplay(CalendarDay date) {
        if (date == null) return;
        Calendar calendar = Calendar.getInstance();
        calendar.set(date.getYear(), date.getMonth() - 1, date.getDay());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM, yyyy", Locale.getDefault());
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        if (tvSelectedDate != null) tvSelectedDate.setText(dateFormat.format(calendar.getTime()));
        if (tvSelectedDay != null) tvSelectedDay.setText(dayFormat.format(calendar.getTime()));
        
        // Refresh selected tags UI if date changes, as some tags might become invalid
        updateSelectedTagsUI(selectedTags);

        // Refresh day transactions if the container is visible
        if (containerDayTransactions != null && containerDayTransactions.getVisibility() == View.VISIBLE) {
            loadDayTransactions();
        }
    }

    private void toggleDayTransactions() {
        if (containerDayTransactions == null) return;
        if (containerDayTransactions.getVisibility() == View.VISIBLE) {
            containerDayTransactions.setVisibility(View.GONE);
        } else {
            containerDayTransactions.setVisibility(View.VISIBLE);
            loadDayTransactions();
        }
    }

    private void loadDayTransactions() {
        long startOfDay = DateUtils.getStartOfDay(getSelectedDateTimestamp());
        long endOfDay = startOfDay + (24 * 60 * 60 * 1000) - 1;

        transactionRepository.getTransactionsBetween(startOfDay, endOfDay, transactions -> {
            if (dayTransactionsAdapter != null) {
                dayTransactionsAdapter.setTransactions(transactions);
            }
        });
    }

    private static class SmallTransactionAdapter extends RecyclerView.Adapter<SmallTransactionAdapter.ViewHolder> {
        private List<TransactionEntity> transactions = new ArrayList<>();
        private OnItemClickListener listener;

        public interface OnItemClickListener {
            void onItemClick(TransactionEntity transaction);
        }

        public void setOnItemClickListener(OnItemClickListener listener) {
            this.listener = listener;
        }

        public void setTransactions(List<TransactionEntity> transactions) {
            this.transactions = transactions;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction_small, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TransactionEntity transaction = transactions.get(position);
            holder.tvParticular.setText(transaction.getItem());
            holder.tvAmount.setText(String.format(Locale.getDefault(), "₹%.2f", transaction.getAmount()));
            
            if ("REFUND".equalsIgnoreCase(transaction.getTransactionType())) {
                holder.tvAmount.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.emerald_green));
            } else {
                holder.tvAmount.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.accent_pink));
            }

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(transaction);
                }
            });
        }

        @Override
        public int getItemCount() {
            return transactions.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvParticular, tvAmount;

            ViewHolder(View itemView) {
                super(itemView);
                tvParticular = itemView.findViewById(R.id.tvSmallParticular);
                tvAmount = itemView.findViewById(R.id.tvSmallAmount);
            }
        }
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
        View pluxee = dialog.findViewById(R.id.optionPLUXEE);
        View icici = dialog.findViewById(R.id.optionICICI);
        View ama = dialog.findViewById(R.id.optionICICIAMA);
        View axis = dialog.findViewById(R.id.optionAXIS);
        if (upi != null) upi.setOnClickListener(optionClick);
        if (cash != null) cash.setOnClickListener(optionClick);
        if (pluxee != null) pluxee.setOnClickListener(optionClick);
        if (icici != null) icici.setOnClickListener(optionClick);
        if (ama != null) ama.setOnClickListener(optionClick);
        if (axis != null) axis.setOnClickListener(optionClick);
        dialog.show();
    }

    private void setupBottomNavigation() {
        View fab = findViewById(R.id.btnCenterFab);
        if (fab != null) fab.setOnClickListener(v -> clearUI());

        View navDashboard = findViewById(R.id.navDashboard);
        if (navDashboard != null) {
            navDashboard.setOnClickListener(v -> {
                Intent intent = new Intent(this, DashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            });
        }

        View navAnalytics = findViewById(R.id.navAnalytics);
        if (navAnalytics != null) {
            navAnalytics.setOnClickListener(v -> {
                startActivity(new Intent(this, AnalyticsActivity.class));
            });
        }

        View navMore = findViewById(R.id.navMore);
        if (navMore != null) {
            navMore.setOnClickListener(v -> {
                startActivity(new Intent(this, MoreActivity.class));
            });
        }
    }

    private void setupActions() {
        View done = findViewById(R.id.btnDoneForDay);
        btnCancel = findViewById(R.id.btnCancel);
        btnSave = findViewById(R.id.btnSave);
        if (done != null) done.setOnClickListener(v -> markDoneForDay());
        if (btnCancel != null) btnCancel.setOnClickListener(v -> {
            if (editingTransaction != null) {
                deleteCurrentTransaction();
            } else {
                clearUI();
            }
        });
        if (btnSave != null) btnSave.setOnClickListener(v -> validateAndSave());

        tvTypeExpense = findViewById(R.id.tvTypeExpense);
        tvTypeRefund = findViewById(R.id.tvTypeRefund);

        if (tvTypeExpense != null && tvTypeRefund != null) {
            tvTypeExpense.setOnClickListener(v -> setTransactionType("EXPENSE"));
            tvTypeRefund.setOnClickListener(v -> setTransactionType("REFUND"));
        }
    }

    private void setTransactionType(String type) {
        currentTransactionType = type;
        if (type.equals("EXPENSE")) {
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

    private void markDoneForDay() {
        CalendarDay date = calendarView.getSelectedDate();
        if (date == null) date = CalendarDay.today();
        
        Calendar cal = Calendar.getInstance();
        cal.set(date.getYear(), date.getMonth() - 1, date.getDay());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM, yyyy", Locale.getDefault());
        String dateStr = dateFormat.format(cal.getTime());

        try {
            SyncStatus syncStatus = new SyncStatus();
            syncStatus.date = dateStr;
            syncStatus.timestamp = System.currentTimeMillis();
            
            String json = new Gson().toJson(syncStatus);
            
            registryRepository.saveRegistryItem("expense_sync_date", json, () -> {
                Toast.makeText(this, "Marked done for " + dateStr, Toast.LENGTH_SHORT).show();
            });
        } catch (Exception e) {
            Toast.makeText(this, "Failed to mark sync", Toast.LENGTH_SHORT).show();
        }
    }

    private static class SyncStatus {
        String date;
        long timestamp;
    }

    private void deleteCurrentTransaction() {
        if (editingTransaction == null) return;
        progressBar.setVisibility(View.VISIBLE);
        transactionRepository.deleteTransaction(editingTransaction, () -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Entry deleted successfully", Toast.LENGTH_SHORT).show();
            clearUI();
        });
    }

    private void validateAndSave() {
        if (etParticular == null || etAmount == null || etNote == null) return;

        String item = etParticular.getText().toString().trim();
        if (!item.isEmpty()) {
            item = item.substring(0, 1).toUpperCase() + item.substring(1);
        }
        String amountStr = etAmount.getText().toString().trim();
        String notes = etNote.getText().toString().trim();
        
        if (TextUtils.isEmpty(item)) { etParticular.setError("Required"); return; }
        if (TextUtils.isEmpty(amountStr)) { etAmount.setError("Required"); return; }
        if (selectedCategory == null) { Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show(); return; }

        double amount;
        try { 
            amount = Double.parseDouble(amountStr);
            // Ensure refund is always negative
            if ("REFUND".equals(currentTransactionType)) {
                amount = -Math.abs(amount);
            }
        } catch (NumberFormatException e) { 
            etAmount.setError("Invalid amount"); 
            return; 
        }

        progressBar.setVisibility(View.VISIBLE);
        
        CalendarDay date = calendarView.getSelectedDate();
        if (date == null) date = CalendarDay.today();
        
        Calendar cal = Calendar.getInstance();
        cal.set(date.getYear(), date.getMonth() - 1, date.getDay(), 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long transactionTimestamp = cal.getTimeInMillis();
        
        long now = System.currentTimeMillis();

        TransactionEntity transaction = new TransactionEntity(
            editingTransaction != null ? editingTransaction.getTransactionId() : UUID.randomUUID().toString(),
            selectedCategory.getCategoryId(),
            item,
            amount,
            TextUtils.isEmpty(notes) ? null : notes,
            transactionTimestamp,
            editingTransaction != null ? editingTransaction.getCreatedTimestamp() : now,
            now,
            selectedPayment,
            currentTransactionType
        );

        List<String> tagIds = new ArrayList<>();
        for (Tag tag : selectedTags) tagIds.add(tag.getId());

        transactionRepository.saveTransaction(transaction, tagIds, () -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, editingTransaction != null ? "Entry updated successfully" : "Entry saved successfully", Toast.LENGTH_SHORT).show();
            clearUI();
        }, () -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Failed to save entry", Toast.LENGTH_SHORT).show();
        });
    }

    private void clearUI() {
        editingTransaction = null;
        if (etParticular != null) etParticular.setText("");
        if (etAmount != null) etAmount.setText("");
        if (etNote != null) etNote.setText("");
        selectedCategory = null;
        updateSelectedCategoryUI();
        selectedTags.clear();
        for (Tag t : allTags) t.setSelected(false);
        updateSelectedTagsUI(new ArrayList<>());
        setTransactionType("EXPENSE");

        // Reset button texts
        if (btnSave instanceof TextView) ((TextView) btnSave).setText("Save Entry");
        if (btnCancel instanceof TextView) ((TextView) btnCancel).setText("Clear");

        if (containerDayTransactions != null && containerDayTransactions.getVisibility() == View.VISIBLE) {
            loadDayTransactions();
        }

        // Reset payment method
        selectedPayment = "UPI";
        View paymentView = findViewById(R.id.paymentDropdown);
        if (paymentView != null) {
            TextView tvPayment = paymentView.findViewById(R.id.dropdownText);
            if (tvPayment != null) tvPayment.setText(selectedPayment);
        }
    }

    private void initAutoTagMap() {
        autoTagMap.put("egg", Arrays.asList("Eggs", "Gym"));
        autoTagMap.put("banana", Arrays.asList("Fruits", "Gym"));
        autoTagMap.put("sweet potato", Arrays.asList("Gym"));
        autoTagMap.put("curd", Arrays.asList("Gym"));
        autoTagMap.put("geek", Arrays.asList("Country Delight", "Gym"));
        autoTagMap.put("bus", Arrays.asList("Bus"));
        autoTagMap.put("auto", Arrays.asList("Auto"));
        autoTagMap.put("metro", Arrays.asList("Train"));
        autoTagMap.put("train", Arrays.asList("Train"));
        autoTagMap.put("local", Arrays.asList("Train"));
        autoTagMap.put("choix", Arrays.asList("Office"));
        autoTagMap.put("thali", Arrays.asList("Office"));
        autoTagMap.put("desi deli", Arrays.asList("Office"));
        autoTagMap.put("xplore", Arrays.asList("Office"));
        autoTagMap.put("smoothie", Arrays.asList("Office"));
        autoTagMap.put("salad bowl", Arrays.asList("Office"));
        autoTagMap.put("dosa", Arrays.asList("Office"));
    }

    private void applyAutoTags(String text) {
        if (TextUtils.isEmpty(text) || allTags == null || allTags.isEmpty()) return;

        String lowerText = text.toLowerCase();
        boolean changed = false;

        for (Map.Entry<String, List<String>> entry : autoTagMap.entrySet()) {
            if (lowerText.contains(entry.getKey())) {
                for (String tagName : entry.getValue()) {
                    Tag matchingTag = findTagByName(tagName);
                    if (matchingTag != null && !selectedTags.contains(matchingTag)) {
                        matchingTag.setSelected(true);
                        selectedTags.add(matchingTag);
                        changed = true;
                    }
                }
            }
        }

        if (changed) {
            updateSelectedTagsUI(selectedTags);

            // If "Office" tag is selected, set payment to PLUXEE
            for (Tag tag : selectedTags) {
                if ("Office".equalsIgnoreCase(tag.getName())) {
                    selectedPayment = "PLUXEE";
                    View paymentView = findViewById(R.id.paymentDropdown);
                    if (paymentView != null) {
                        TextView tvPayment = paymentView.findViewById(R.id.dropdownText);
                        if (tvPayment != null) tvPayment.setText(selectedPayment);
                    }
                    break;
                }
            }
        }
    }

    private Tag findTagByName(String name) {
        for (Tag tag : allTags) {
            if (tag.getName().equalsIgnoreCase(name)) {
                return tag;
            }
        }
        return null;
    }
}