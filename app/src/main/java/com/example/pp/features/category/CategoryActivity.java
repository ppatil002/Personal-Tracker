package com.example.pp.features.category;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pp.PPApplication;
import com.example.pp.R;
import com.example.pp.data.local.entity.CategoryEntity;
import com.example.pp.data.repository.CategoryRepository;
import com.example.pp.features.category.adapter.CategoryManagerAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class CategoryActivity extends AppCompatActivity {

    private CategoryRepository categoryRepository;
    private CategoryManagerAdapter adapter;
    private List<CategoryEntity> allCategories = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_manager);

        categoryRepository = ((PPApplication) getApplication()).getCategoryRepository();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        RecyclerView rvCategories = findViewById(R.id.rvCategories);
        rvCategories.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CategoryManagerAdapter();
        rvCategories.setAdapter(adapter);

        adapter.setOnCategoryActionListener(new CategoryManagerAdapter.OnCategoryActionListener() {
            @Override
            public void onEdit(CategoryEntity category) {
                showCategoryDialog(category);
            }

            @Override
            public void onDelete(CategoryEntity category) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(CategoryActivity.this);
                builder.setTitle("Delete Category")
                        .setMessage("Are you sure you want to delete '" + category.getName() + "'? This might affect existing transactions.")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            categoryRepository.deleteCategory(category, () -> {
                                Toast.makeText(CategoryActivity.this, "Category deleted", Toast.LENGTH_SHORT).show();
                            });
                        })
                        .setNegativeButton("Cancel", null);

                AlertDialog dialog = builder.create();
                dialog.show();
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawableResource(R.color.card_background);
                }
            }
        });

        findViewById(R.id.fabAddCategory).setOnClickListener(v -> showCategoryDialog(null));

        categoryRepository.getCategoryTree().observe(this, tree -> {
            new Thread(() -> {
                List<CategoryEntity> entities = com.example.pp.data.local.AppDatabase.getInstance(this).categoryDao().getAllCategories();
                runOnUiThread(() -> {
                    this.allCategories = entities;
                    adapter.submitList(entities);
                });
            }).start();
        });
    }

    private void showCategoryDialog(CategoryEntity category) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_category, null);
        EditText etName = view.findViewById(R.id.etCategoryName);
        EditText etIcon = view.findViewById(R.id.etCategoryIcon);
        EditText etColor = view.findViewById(R.id.etCategoryColor);
        View vColorPreview = view.findViewById(R.id.vColorPreview);
        Spinner spinner = view.findViewById(R.id.spinnerParentCategory);

        etColor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String hex = s.toString().trim();
                if (hex.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$")) {
                    try {
                        vColorPreview.setBackgroundColor(Color.parseColor(hex));
                    } catch (Exception e) {
                        vColorPreview.setBackgroundColor(Color.TRANSPARENT);
                    }
                } else {
                    vColorPreview.setBackgroundColor(Color.TRANSPARENT);
                }
            }
        });

        List<String> parentOptions = new ArrayList<>();
        parentNames = new ArrayList<>();
        parentOptions.add("None (Root)");
        parentNames.add(null);

        for (CategoryEntity cat : allCategories) {
            if (cat.getParentCategoryId() == null) {
                if (category != null && cat.getCategoryId().equals(category.getCategoryId())) continue;
                parentOptions.add(cat.getName());
                parentNames.add(cat.getCategoryId());
            }
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, parentOptions);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);

        if (category != null) {
            etName.setText(category.getName());
            etIcon.setText(category.getIcon());
            etColor.setText(category.getColour());
            builder.setTitle("Edit Category");
            if (category.getParentCategoryId() != null) {
                int idx = parentNames.indexOf(category.getParentCategoryId());
                if (idx >= 0) spinner.setSelection(idx);
            }
        } else {
            etColor.setText("#FFFFFF");
            builder.setTitle("Add Category");
        }

        builder.setView(view)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String icon = etIcon.getText().toString().trim();
                    String color = etColor.getText().toString().trim();
                    String parentId = parentNames.get(spinner.getSelectedItemPosition());

                    if (TextUtils.isEmpty(name)) {
                        Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!color.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$")) {
                        Toast.makeText(this, "Invalid color format", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (category != null) {
                        category.setName(name);
                        category.setIcon(icon.isEmpty() ? "📁" : icon);
                        category.setParentCategoryId(parentId);
                        category.setColour(color);
                        categoryRepository.updateCategory(category, () -> Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show());
                    } else {
                        categoryRepository.addCategory(parentId, name, icon.isEmpty() ? "📁" : icon, color, () -> Toast.makeText(this, "Added", Toast.LENGTH_SHORT).show());
                    }
                })
                .setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.color.card_background);
        }
    }

    private List<String> parentNames; // Helper for spinner mapping
}
