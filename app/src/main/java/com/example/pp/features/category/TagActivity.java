package com.example.pp.features.category;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pp.PPApplication;
import com.example.pp.R;
import com.example.pp.data.local.entity.TagEntity;
import com.example.pp.data.repository.TagRepository;
import com.example.pp.features.category.adapter.TagManagerAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TagActivity extends AppCompatActivity {

    private TagRepository tagRepository;
    private TagManagerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_manager);

        tagRepository = ((PPApplication) getApplication()).getTagRepository();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        RecyclerView rvTags = findViewById(R.id.rvTags);
        rvTags.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TagManagerAdapter();
        rvTags.setAdapter(adapter);

        adapter.setOnTagActionListener(new TagManagerAdapter.OnTagActionListener() {
            @Override
            public void onEdit(TagEntity tag) {
                showTagDialog(tag);
            }

            @Override
            public void onDelete(TagEntity tag) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(TagActivity.this);
                builder.setTitle("Delete Tag")
                        .setMessage("Are you sure you want to delete tag '" + tag.getName() + "'?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            tagRepository.deleteTag(tag, () -> {
                                Toast.makeText(TagActivity.this, "Tag deleted", Toast.LENGTH_SHORT).show();
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

        findViewById(R.id.fabAddTag).setOnClickListener(v -> showTagDialog(null));

        tagRepository.getTags().observe(this, tags -> {
            new Thread(() -> {
                java.util.List<TagEntity> entities = com.example.pp.data.local.AppDatabase.getInstance(this).tagDao().getAllTags();
                runOnUiThread(() -> adapter.submitList(entities));
            }).start();
        });
    }

    private void showTagDialog(TagEntity tag) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_tag, null);
        EditText etName = view.findViewById(R.id.etTagName);
        TextView tvValidFrom = view.findViewById(R.id.tvValidFrom);
        TextView tvValidTill = view.findViewById(R.id.tvValidTill);
        View btnClearDates = view.findViewById(R.id.btnClearDates);

        final Calendar calFrom = Calendar.getInstance();
        final Calendar calTill = Calendar.getInstance();
        final long[] selectedFrom = {tag != null && tag.getValidFrom() != null ? tag.getValidFrom() : -1};
        final long[] selectedTill = {tag != null && tag.getValidTill() != null ? tag.getValidTill() : -1};

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, yyyy", Locale.getDefault());

        if (tag != null) {
            etName.setText(tag.getName());
            builder.setTitle("Edit Tag");
            if (selectedFrom[0] != -1) tvValidFrom.setText("From: " + sdf.format(new Date(selectedFrom[0])));
            if (selectedTill[0] != -1) tvValidTill.setText("Till: " + sdf.format(new Date(selectedTill[0])));
        } else {
            builder.setTitle("Add Tag");
        }

        tvValidFrom.setOnClickListener(v -> {
            if (selectedFrom[0] != -1) calFrom.setTimeInMillis(selectedFrom[0]);
            new DatePickerDialog(this, (view1, year, month, dayOfMonth) -> {
                calFrom.set(year, month, dayOfMonth, 0, 0, 0);
                calFrom.set(Calendar.MILLISECOND, 0);
                selectedFrom[0] = calFrom.getTimeInMillis();
                tvValidFrom.setText("From: " + sdf.format(calFrom.getTime()));
            }, calFrom.get(Calendar.YEAR), calFrom.get(Calendar.MONTH), calFrom.get(Calendar.DAY_OF_MONTH)).show();
        });

        tvValidTill.setOnClickListener(v -> {
            if (selectedTill[0] != -1) calTill.setTimeInMillis(selectedTill[0]);
            new DatePickerDialog(this, (view1, year, month, dayOfMonth) -> {
                calTill.set(year, month, dayOfMonth, 23, 59, 59);
                calTill.set(Calendar.MILLISECOND, 999);
                selectedTill[0] = calTill.getTimeInMillis();
                tvValidTill.setText("Till: " + sdf.format(calTill.getTime()));
            }, calTill.get(Calendar.YEAR), calTill.get(Calendar.MONTH), calTill.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnClearDates.setOnClickListener(v -> {
            selectedFrom[0] = -1;
            selectedTill[0] = -1;
            tvValidFrom.setText("");
            tvValidTill.setText("");
            tvValidFrom.setHint("Valid From (Optional)");
            tvValidTill.setHint("Valid Till (Optional)");
        });

        builder.setView(view)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (TextUtils.isEmpty(name)) {
                        Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Long vFrom = selectedFrom[0] == -1 ? null : selectedFrom[0];
                    Long vTill = selectedTill[0] == -1 ? null : selectedTill[0];

                    if (tag != null) {
                        tag.setName(name);
                        tag.setValidFrom(vFrom);
                        tag.setValidTill(vTill);
                        tagRepository.updateTag(tag, () -> Toast.makeText(this, "Tag updated", Toast.LENGTH_SHORT).show());
                    } else {
                        tagRepository.addTag(name, vFrom, vTill, () -> Toast.makeText(this, "Tag added", Toast.LENGTH_SHORT).show());
                    }
                })
                .setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.color.card_background);
        }
    }
}
