package com.example.pp.features.category;

import android.app.Dialog;
import android.content.DialogInterface;
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
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pp.R;
import com.example.pp.features.category.adapter.TagBottomSheetAdapter;
import com.example.pp.features.category.model.Tag;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

public class TagBottomSheetFragment extends BottomSheetDialogFragment {

    private OnTagsAppliedListener listener;
    private List<Tag> tags;

    private TagBottomSheetAdapter adapter;
    private boolean isApplied = false;

    public interface OnTagsAppliedListener {
        void onTagsApplied(List<Tag> selectedTags);
    }

    public void setOnTagsAppliedListener(OnTagsAppliedListener listener) {
        this.listener = listener;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (!isApplied && listener != null && adapter != null) {
            listener.onTagsApplied(adapter.getSelectedTags());
        }
    }

    @Override
    public int getTheme() {
        return R.style.RoundedBottomSheetDialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_tag_selector, container, false);

        RecyclerView rvTags = view.findViewById(R.id.rvTags);
        EditText etSearch = view.findViewById(R.id.etSearchTag);
        AppCompatButton btnApply = view.findViewById(R.id.btnApplyTags);

        adapter = new TagBottomSheetAdapter(tags);
        rvTags.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTags.setAdapter(adapter);

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

        btnApply.setOnClickListener(v -> {
            isApplied = true;
            List<Tag> selected = adapter.getSelectedTags();
            if (listener != null) {
                listener.onTagsApplied(selected);
            }
            dismiss();
        });

        return view;
    }
}
