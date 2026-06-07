package com.example.pp.features.nutrition;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pp.PPApplication;
import com.example.pp.R;
import com.example.pp.data.local.entity.NutrientEntity;
import com.example.pp.data.repository.NutrientRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class NutrientDirectoryActivity extends AppCompatActivity implements NutrientAdapter.OnNutrientClickListener {

    private NutrientRepository nutrientRepository;
    private NutrientAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nutrient_directory);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        nutrientRepository = ((PPApplication) getApplication()).getNutrientRepository();

        RecyclerView rvNutrients = findViewById(R.id.rvNutrients);
        rvNutrients.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NutrientAdapter(this);
        rvNutrients.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAddNutrient);
        fab.setOnClickListener(v -> showAddEditDialog(null));

        loadNutrients();
    }

    private void loadNutrients() {
        nutrientRepository.getAllNutrients(nutrients -> adapter.setNutrients(nutrients));
    }

    private void showAddEditDialog(NutrientEntity existing) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_nutrient, null);
        EditText etFoodItem = view.findViewById(R.id.etFoodItem);
        EditText etPerUnit = view.findViewById(R.id.etPerUnit);
        EditText etUnit = view.findViewById(R.id.etUnit);
        EditText etCalories = view.findViewById(R.id.etCalories);
        EditText etProtein = view.findViewById(R.id.etProtein);
        EditText etCarbs = view.findViewById(R.id.etCarbs);
        EditText etFats = view.findViewById(R.id.etFats);

        if (existing != null) {
            etFoodItem.setText(existing.getFoodItem());
            etPerUnit.setText(String.valueOf(existing.getPerUnit()));
            etUnit.setText(existing.getUnit());
            etCalories.setText(String.valueOf(existing.getCalories()));
            etProtein.setText(String.valueOf(existing.getProtein()));
            etCarbs.setText(String.valueOf(existing.getCarbs()));
            etFats.setText(String.valueOf(existing.getFats()));
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(existing == null ? "Add Nutrient" : "Edit Nutrient")
                .setView(view)
                .setPositiveButton("Save", (d, which) -> {
                    String name = etFoodItem.getText().toString().trim();
                    String perUnitStr = etPerUnit.getText().toString().trim();
                    String unit = etUnit.getText().toString().trim();
                    String calStr = etCalories.getText().toString().trim();
                    String proStr = etProtein.getText().toString().trim();
                    String carbStr = etCarbs.getText().toString().trim();
                    String fatStr = etFats.getText().toString().trim();

                    if (name.isEmpty() || perUnitStr.isEmpty() || unit.isEmpty() || calStr.isEmpty()) {
                        Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double perUnit = Double.parseDouble(perUnitStr);
                    double cal = Double.parseDouble(calStr);
                    double pro = proStr.isEmpty() ? 0 : Double.parseDouble(proStr);
                    double carb = carbStr.isEmpty() ? 0 : Double.parseDouble(carbStr);
                    double fat = fatStr.isEmpty() ? 0 : Double.parseDouble(fatStr);

                    long now = System.currentTimeMillis();
                    if (existing == null) {
                        NutrientEntity nutrient = new NutrientEntity(name, cal, pro, carb, fat, perUnit, unit, now, now);
                        nutrientRepository.addNutrient(nutrient, this::loadNutrients);
                    } else {
                        existing.setFoodItem(name);
                        existing.setPerUnit(perUnit);
                        existing.setUnit(unit);
                        existing.setCalories(cal);
                        existing.setProtein(pro);
                        existing.setCarbs(carb);
                        existing.setFats(fat);
                        existing.setUpdatedDate(now);
                        nutrientRepository.updateNutrient(existing, this::loadNutrients);
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.color.card_background);
        }
    }

    @Override
    public void onEdit(NutrientEntity nutrient) {
        showAddEditDialog(nutrient);
    }

    @Override
    public void onDelete(NutrientEntity nutrient) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Nutrient")
                .setMessage("Are you sure you want to delete " + nutrient.getFoodItem() + "?")
                .setPositiveButton("Delete", (d, which) -> {
                    nutrientRepository.deleteNutrient(nutrient, this::loadNutrients);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
