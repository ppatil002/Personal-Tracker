package com.example.pp.features.settings;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pp.R;
import com.example.pp.features.category.CategoryActivity;
import com.example.pp.features.category.TagActivity;

public class MoreActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_more);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        findViewById(R.id.btnViewTables).setOnClickListener(v -> {
            startActivity(new Intent(this, TableListActivity.class));
        });

        findViewById(R.id.btnCategoryManager).setOnClickListener(v -> {
            startActivity(new Intent(this, CategoryActivity.class));
        });

        findViewById(R.id.btnTagManager).setOnClickListener(v -> {
            startActivity(new Intent(this, TagActivity.class));
        });
    }
}