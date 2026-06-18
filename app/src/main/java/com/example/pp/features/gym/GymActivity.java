package com.example.pp.features.gym;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pp.R;
import com.example.pp.core.task.AppExecutors;
import com.example.pp.data.local.entity.BodyWeight;
import com.example.pp.data.local.entity.GymTracking;
import com.example.pp.utils.DateUtils;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import android.widget.LinearLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GymActivity extends AppCompatActivity {

    private GymViewModel viewModel;
    private LineChart weightChart;
    private RecyclerView rvGymLogs;
    private GymLogAdapter adapter;
    private TextView tvLogPeriodLabel;
    private View btnPrevLogs, btnNextLogs;
    private long lastSelectedDate = DateUtils.getStartOfDay(System.currentTimeMillis());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gym);

        viewModel = new ViewModelProvider(this).get(GymViewModel.class);
        initViews();
        setupChart();
        observeViewModel();
    }

    private void initViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        weightChart = findViewById(R.id.weightChart);
        rvGymLogs = findViewById(R.id.rvGymLogs);

        btnPrevLogs = findViewById(R.id.btnPrevLogs);
        btnNextLogs = findViewById(R.id.btnNextLogs);
        
        btnPrevLogs.setOnClickListener(v -> viewModel.navigateLogPeriod(-1));
        btnNextLogs.setOnClickListener(v -> viewModel.navigateLogPeriod(1));
        tvLogPeriodLabel = findViewById(R.id.tvLogPeriodLabel);

        adapter = new GymLogAdapter();
        adapter.setOnLogClickListener(this::showGymEntryDialog);
        rvGymLogs.setLayoutManager(new LinearLayoutManager(this));
        rvGymLogs.setAdapter(adapter);

        ChipGroup cgFilter = findViewById(R.id.cgWeightFilter);
        cgFilter.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip10) viewModel.setWeightLimit(10);
            else if (checkedId == R.id.chip30) viewModel.setWeightLimit(30);
            else if (checkedId == R.id.chipAll) viewModel.setWeightLimit(-1);
        });
        cgFilter.check(R.id.chip10);

        findViewById(R.id.fabAddGym).setOnClickListener(v -> showGymEntryDialog());
    }

    private void setupChart() {
        weightChart.getDescription().setEnabled(false);
        weightChart.setDrawGridBackground(false);
        weightChart.getLegend().setEnabled(false);
        weightChart.setTouchEnabled(true);
        weightChart.setDragEnabled(true);
        weightChart.setScaleEnabled(true);
        weightChart.setPinchZoom(true);

        XAxis xAxis = weightChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(5); // Sparse labels

        YAxis leftAxis = weightChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(ContextCompat.getColor(this, R.color.stroke_color));
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisMinimum(0f);

        weightChart.getAxisRight().setEnabled(false);

        weightChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                // Marker will show automatically
            }

            @Override
            public void onNothingSelected() {}
        });
    }

    private void observeViewModel() {
        viewModel.getBodyWeightHistory().observe(this, weights -> {
            if (weights != null && !weights.isEmpty()) {
                renderWeightChart(weights);
            }
        });

        viewModel.getGymLogsForSelectedPeriod().observe(this, logs -> {
            if (logs != null) {
                adapter.setLogs(logs);
            }
        });

        viewModel.getLogPeriodLabel().observe(this, label -> tvLogPeriodLabel.setText(label));
        
        viewModel.getSelectedLogDate().observe(this, date -> {
            if (date != null) lastSelectedDate = date;
        });

        viewModel.getAvailableWorkoutDates().observe(this, dates -> {
            updateNavButtons(dates);
        });
    }

    private void updateNavButtons(List<Long> dates) {
        if (dates == null || dates.isEmpty()) {
            btnPrevLogs.setEnabled(false);
            btnNextLogs.setEnabled(false);
            btnPrevLogs.setAlpha(0.3f);
            btnNextLogs.setAlpha(0.3f);
            return;
        }

        long current = DateUtils.getStartOfDay(Calendar.getInstance().getTimeInMillis()); // Default if none selected
        // Better: use the actual selected date from VM if possible, but we don't have direct access here easily without another observer or logic.
        // Let's observe the selected date in the VM too.
    }

    private void renderWeightChart(List<BodyWeight> weights) {
        List<BodyWeight> sortedWeights = new ArrayList<>(weights);
        Collections.sort(sortedWeights, (a, b) -> Long.compare(a.dateMeasured, b.dateMeasured));

        List<Entry> entries = new ArrayList<>();
        List<Long> timestamps = new ArrayList<>();
        float minWeight = Float.MAX_VALUE;

        for (int i = 0; i < sortedWeights.size(); i++) {
            BodyWeight bw = sortedWeights.get(i);
            float w = (float) bw.weight;
            entries.add(new Entry(i, w));
            timestamps.add(bw.dateMeasured);
            if (w < minWeight) minWeight = w;
        }

        LineDataSet set = new LineDataSet(entries, "Weight");
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawFilled(true);
        set.setDrawCircles(true);
        set.setCircleColor(ContextCompat.getColor(this, R.color.accent_purple));
        set.setColor(ContextCompat.getColor(this, R.color.accent_purple));
        set.setLineWidth(2f);
        set.setCircleRadius(4f);
        set.setDrawCircleHole(false);
        set.setDrawValues(false);
        set.setHighlightEnabled(true);
        set.setDrawHighlightIndicators(false);

        // Gradient Fill
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{
                        ContextCompat.getColor(this, R.color.accent_purple_low_alpha),
                        Color.TRANSPARENT
                }
        );
        set.setFillDrawable(gradient);

        LineData data = new LineData(set);
        weightChart.setData(data);

        // X-Axis Value Formatter
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM", Locale.getDefault());
        weightChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < timestamps.size()) {
                    return sdf.format(new Date(timestamps.get(index)));
                }
                return "";
            }
        });

        // Marker
        WeightMarkerView marker = new WeightMarkerView(this, timestamps);
        weightChart.setMarker(marker);

        // Adjust Y Axis
        YAxis leftAxis = weightChart.getAxisLeft();
        if (minWeight != Float.MAX_VALUE) {
            leftAxis.setAxisMinimum(Math.max(0, minWeight - 10));
        }

        weightChart.invalidate();
    }

    private void showGymEntryDialog() {
        showGymEntryDialog(null);
    }

    private void showGymEntryDialog(GymTracking existingLog) {
        Calendar selectedDate = Calendar.getInstance();
        if (existingLog != null) {
            selectedDate.setTimeInMillis(existingLog.exerciseDate);
        } else {
            selectedDate.setTimeInMillis(lastSelectedDate);
        }

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_gym_entry, null);
        
        TextView tvDate = view.findViewById(R.id.tvGymDate);
        EditText etWeight = view.findViewById(R.id.etBodyWeight);
        AutoCompleteTextView actvMuscle = view.findViewById(R.id.actvMuscle);
        AutoCompleteTextView actvExercise = view.findViewById(R.id.actvExercise);
        LinearLayout containerSets = view.findViewById(R.id.containerSets);
        MaterialButton btnAddSet = view.findViewById(R.id.btnAddSet);

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, yyyy", Locale.getDefault());
        tvDate.setText(sdf.format(selectedDate.getTime()));

        // Populate Pickers
        String[] muscles = {"Back", "Biceps", "Chest", "Shoulders", "Triceps","Legs", "Cardio", "Abs"};
        actvMuscle.setAdapter(new ArrayAdapter<>(this, R.layout.item_dropdown_divider, R.id.tvDropdownItem, muscles));
        actvMuscle.setOnClickListener(v -> actvMuscle.showDropDown());

        if (existingLog != null) {
            actvMuscle.setText(existingLog.muscle, false);
            actvExercise.setText(existingLog.exercise);
            
            // Load sets
            try {
                if (existingLog.setsJson != null && existingLog.setsJson.startsWith("[{")) {
                    List<Map<String, String>> sets = new Gson().fromJson(existingLog.setsJson, new TypeToken<List<Map<String, String>>>(){}.getType());
                    for (Map<String, String> setData : sets) {
                        addSetRow(containerSets, setData);
                    }
                } else {
                    // Fallback for old format
                    addSetRow(containerSets, null);
                }
            } catch (Exception e) {
                // In case startsWith("[") but is not valid JSON array of maps
                addSetRow(containerSets, null);
            }
        } else {
            addSetRow(containerSets, null);
        }

        btnAddSet.setOnClickListener(v -> addSetRow(containerSets, null));

        tvDate.setOnClickListener(v -> {
            new android.app.DatePickerDialog(this, (d, y, m, day) -> {
                selectedDate.set(y, m, day);
                lastSelectedDate = DateUtils.getStartOfDay(selectedDate.getTimeInMillis());
                tvDate.setText(sdf.format(selectedDate.getTime()));
                
                AppExecutors.getInstance().diskIO().execute(() -> {
                    BodyWeight w = viewModel.getWeightForDateSync(DateUtils.getStartOfDay(selectedDate.getTimeInMillis()));
                    AppExecutors.getInstance().mainThread().execute(() -> {
                        if (w != null) etWeight.setText(String.valueOf(w.weight));
                        else etWeight.setText("");
                    });
                });
            }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show();
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(existingLog == null ? "Log Session" : "Edit Session")
                .setView(view)
                .setPositiveButton("Save", (d, which) -> {
                    String weightStr = etWeight.getText().toString();
                    if (!weightStr.isEmpty()) {
                        viewModel.addBodyWeight(Double.parseDouble(weightStr), DateUtils.getStartOfDay(selectedDate.getTimeInMillis()));
                    }

                    String muscle = actvMuscle.getText().toString();
                    String exercise = capitalizeWords(actvExercise.getText().toString().trim());

                    List<Map<String, String>> setsList = new ArrayList<>();
                    for (int i = 0; i < containerSets.getChildCount(); i++) {
                        View row = containerSets.getChildAt(i);
                        String r = ((EditText) row.findViewById(R.id.etReps)).getText().toString().trim();
                        String w = ((EditText) row.findViewById(R.id.etWeight)).getText().toString().trim();
                        String df = ((AutoCompleteTextView) row.findViewById(R.id.actvDifficulty)).getText().toString().trim();

                        if (!r.isEmpty()) {
                            Map<String, String> setMap = new HashMap<>();
                            setMap.put("reps", r);
                            if (!w.isEmpty()) setMap.put("weight", w);
                            if (!df.isEmpty()) setMap.put("diff", df);
                            setsList.add(setMap);
                        }
                    }

                    if (existingLog != null && setsList.isEmpty()) {
                        // Deletes entry if all rows are removed on edit
                        viewModel.deleteGymLog(existingLog);
                        Toast.makeText(this, "Session removed", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!muscle.isEmpty() && !exercise.isEmpty() && !setsList.isEmpty()) {
                        String json = new Gson().toJson(setsList);
                        if (existingLog != null) {
                            existingLog.muscle = muscle;
                            existingLog.exercise = exercise;
                            existingLog.setsJson = json;
                            existingLog.exerciseDate = selectedDate.getTimeInMillis();
                            viewModel.updateGymLog(existingLog);
                        } else {
                            viewModel.addGymLog(muscle, exercise, json, "", selectedDate.getTimeInMillis());
                        }
                        Toast.makeText(this, "Workout saved", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.color.card_background);
        }
    }

    private void addSetRow(LinearLayout container, Map<String, String> data) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_gym_set_row, container, false);
        EditText etReps = row.findViewById(R.id.etReps);
        EditText etWeight = row.findViewById(R.id.etWeight);
        AutoCompleteTextView actvDiff = row.findViewById(R.id.actvDifficulty);
        View btnRemove = row.findViewById(R.id.btnRemoveSet);

        String[] difficulties = {"Simple", "Challenging", "Hard"};
        actvDiff.setAdapter(new ArrayAdapter<>(this, R.layout.item_dropdown_divider, R.id.tvDropdownItem, difficulties));
        actvDiff.setOnClickListener(v -> {
            actvDiff.setDropDownVerticalOffset(-actvDiff.getHeight() * 5);
            actvDiff.showDropDown();
        });

        if (data != null) {
            etReps.setText(data.get("reps"));
            etWeight.setText(data.get("weight"));
            actvDiff.setText(data.get("diff"), false);
        }

        btnRemove.setOnClickListener(v -> container.removeView(row));
        container.addView(row);
    }

    private String capitalizeWords(String str) {
        if (str == null || str.isEmpty()) return str;
        String[] words = str.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1).toLowerCase())
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }
}
