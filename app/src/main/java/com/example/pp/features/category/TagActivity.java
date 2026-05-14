package com.example.pp.features.category;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.pp.PPApplication;
import com.example.pp.R;
import com.example.pp.data.local.entity.TagEntity;
import com.example.pp.data.repository.TagRepository;
import com.example.pp.features.category.adapter.TagManagerAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.UUID;

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

        if (tag != null) {
            etName.setText(tag.getName());
            builder.setTitle("Edit Tag");
        } else {
            builder.setTitle("Add Tag");
        }

        builder.setView(view)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (TextUtils.isEmpty(name)) {
                        Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (tag != null) {
                        tag.setName(name);
                        tagRepository.updateTag(tag, () -> Toast.makeText(this, "Tag updated", Toast.LENGTH_SHORT).show());
                    } else {
                        tagRepository.addTag(name, () -> Toast.makeText(this, "Tag added", Toast.LENGTH_SHORT).show());
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
