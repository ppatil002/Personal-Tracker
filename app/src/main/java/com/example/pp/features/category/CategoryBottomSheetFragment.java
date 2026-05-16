package com.example.pp.features.category;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pp.R;
import com.example.pp.domain.model.CategoryTreeNode;
import com.example.pp.features.category.adapter.CategoryBottomSheetAdapter;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

public class CategoryBottomSheetFragment extends BottomSheetDialogFragment {

    private OnCategorySelectedListener listener;
    private List<CategoryTreeNode> categories;

    public interface OnCategorySelectedListener {
        void onCategorySelected(CategoryTreeNode category);
    }

    public void setOnCategorySelectedListener(OnCategorySelectedListener listener) {
        this.listener = listener;
    }

    public void setCategories(List<CategoryTreeNode> categories) {
        this.categories = categories;
    }

    @Override
    public int getTheme() {
        return R.style.RoundedBottomSheetDialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_category_selector, container, false);

        RecyclerView rvCategories = view.findViewById(R.id.rvCategories);
        EditText etSearch = view.findViewById(R.id.etSearchCategory);

        CategoryBottomSheetAdapter adapter = new CategoryBottomSheetAdapter(categories);
        adapter.setOnCategorySelectedListener(category -> {
            if (listener != null) {
                listener.onCategorySelected(category);
            }
            dismiss();
        });

        rvCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCategories.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        return view;
    }
}