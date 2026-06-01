package com.example.pp.features.backup;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.WorkInfo;

import com.example.pp.R;
import com.example.pp.core.enums.TableColumnType;
import com.example.pp.data.local.entity.BackgroundJobLog;
import com.example.pp.data.remote.GoogleDriveClient;
import com.example.pp.ui.components.table.adapter.GenericTableAdapter;
import com.example.pp.ui.components.table.model.TableCell;
import com.example.pp.ui.components.table.model.TableColumn;
import com.example.pp.ui.components.table.model.TableConfig;
import com.example.pp.ui.components.table.model.TableRow;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class BackupActivity extends AppCompatActivity {

    private BackupViewModel viewModel;
    private GenericTableAdapter adapter;
    private TextView tvStatus;
    private View btnSignIn;
    
    private TextView tvPageIndicator;
    private View btnPrevPage, btnNextPage;
    private int currentPage = 0;
    private static final int DATES_PER_PAGE = 3;
    private List<BackgroundJobLog> allLogs = new ArrayList<>();

    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    task.getResult(ApiException.class);
                    GoogleDriveClient.getInstance(this).refresh(this);
                    updateSignInStatus();
                } catch (ApiException e) {
                    Toast.makeText(this, "Sign in failed: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);

        viewModel = new ViewModelProvider(this).get(BackupViewModel.class);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        tvStatus = findViewById(R.id.tvDriveStatus);
        btnSignIn = findViewById(R.id.btnGoogleSignIn);
        
        btnSignIn.setOnClickListener(v -> {
            Intent intent = GoogleDriveClient.getFullGoogleSignInClient(this).getSignInIntent();
            signInLauncher.launch(intent);
        });

        findViewById(R.id.btnRunBackup).setOnClickListener(v -> {
            if (GoogleDriveClient.getInstance(this).isAuthorized()) {
                viewModel.runManualBackup();
            } else {
                Toast.makeText(this, "Please sign in to Google Drive first", Toast.LENGTH_SHORT).show();
            }
        });
        
        findViewById(R.id.btnRunRestore).setOnClickListener(v -> {
            if (GoogleDriveClient.getInstance(this).isAuthorized()) {
                LocalDate now = LocalDate.now();
                int slot = now.get(WeekFields.of(Locale.getDefault()).weekOfYear()) % 4;
                UUID workId = viewModel.runRestore("backup_slot_" + slot + ".enc");
                observeRestoreWork(workId);
            } else {
                Toast.makeText(this, "Please sign in to Google Drive first", Toast.LENGTH_SHORT).show();
            }
        });

        tvPageIndicator = findViewById(R.id.tvPageIndicator);
        btnPrevPage = findViewById(R.id.btnPrevPage);
        btnNextPage = findViewById(R.id.btnNextPage);

        btnPrevPage.setOnClickListener(v -> {
            if (currentPage > 0) {
                currentPage--;
                updateLogsDisplay();
            }
        });

        btnNextPage.setOnClickListener(v -> {
            currentPage++;
            updateLogsDisplay();
        });

        setupLogTable();
        GoogleDriveClient.getInstance(this).signInSilently(this, this::updateSignInStatus);
        viewModel.getLogs().observe(this, logs -> {
            if (logs != null) {
                this.allLogs = logs;
                updateLogsDisplay();
            }
        });
    }

    private void observeRestoreWork(UUID workId) {
        viewModel.getWorkManager().getWorkInfoByIdLiveData(workId).observe(this, workInfo -> {
            if (workInfo != null && workInfo.getState().isFinished()) {
                if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                    String summary = workInfo.getOutputData().getString("IMPORT_SUMMARY");
                    showRestoreSummaryDialog(summary);
                } else {
                    Toast.makeText(this, "Restore failed. Check logs.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void showRestoreSummaryDialog(String summary) {
        new AlertDialog.Builder(this)
                .setTitle("Restore Complete")
                .setMessage(summary != null ? summary : "Database updated successfully.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void updateSignInStatus() {
        if (GoogleDriveClient.getInstance(this).isAuthorized()) {
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
            String displayName = (account != null ? account.getDisplayName() : null);
            if (displayName == null || displayName.isEmpty()) {
                displayName = (account != null ? account.getEmail() : "Authorized");
            }
            tvStatus.setText("Signed in as: " + displayName);
            btnSignIn.setVisibility(View.GONE);
        } else {
            tvStatus.setText("Google Drive not authorized");
            btnSignIn.setVisibility(View.VISIBLE);
        }
    }

    private void setupLogTable() {
        RecyclerView rvLogs = findViewById(R.id.rvLogs);
        rvLogs.setLayoutManager(new LinearLayoutManager(this));

        TableConfig config = new TableConfig();
        adapter = new GenericTableAdapter(config);
        
        TableColumn timeCol = new TableColumn("time", "Time", TableColumnType.TEXT);
        timeCol.setWidthWeight(1.5F);
        
        TableColumn stepCol = new TableColumn("step", "Step", TableColumnType.TEXT);
        stepCol.setWidthWeight(3);
        
        TableColumn statusCol = new TableColumn("status", "Status", TableColumnType.TEXT);
        statusCol.setWidthWeight(2);

        List<TableColumn> columns = Arrays.asList(timeCol, stepCol, statusCol);
        adapter.setColumns(columns);
        rvLogs.setAdapter(adapter);
    }

    private void updateLogsDisplay() {
        if (allLogs.isEmpty()) {
            adapter.setRows(new ArrayList<>());
            tvPageIndicator.setText("Page 0/0");
            return;
        }

        SimpleDateFormat dayFormat = new SimpleDateFormat("dd-MMM", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

        // Group by Date
        Map<String, List<BackgroundJobLog>> groupedLogs = new LinkedHashMap<>();
        for (BackgroundJobLog log : allLogs) {
            String dateKey = dayFormat.format(new Date(log.getLastUpdatedTimestamp()));
            if (!groupedLogs.containsKey(dateKey)) {
                groupedLogs.put(dateKey, new ArrayList<>());
            }
            groupedLogs.get(dateKey).add(log);
        }

        List<String> sortedDates = new ArrayList<>(groupedLogs.keySet());
        int totalPages = (int) Math.ceil((double) sortedDates.size() / DATES_PER_PAGE);
        
        if (currentPage >= totalPages) currentPage = Math.max(0, totalPages - 1);
        
        tvPageIndicator.setText(String.format(Locale.getDefault(), "Page %d/%d", currentPage + 1, totalPages));
        btnPrevPage.setEnabled(currentPage > 0);
        btnNextPage.setEnabled(currentPage < totalPages - 1);
        btnPrevPage.setAlpha(currentPage > 0 ? 1.0f : 0.4f);
        btnNextPage.setAlpha(currentPage < totalPages - 1 ? 1.0f : 0.4f);

        List<TableRow> rows = new ArrayList<>();
        int start = currentPage * DATES_PER_PAGE;
        int end = Math.min(start + DATES_PER_PAGE, sortedDates.size());

        for (int i = start; i < end; i++) {
            String dateStr = sortedDates.get(i);
            
            // Add Header Row for Date
            List<TableCell> headerCells = new ArrayList<>();
            headerCells.add(new TableCell("time", dateStr, null, TableColumnType.TEXT, 3));
            rows.add(new TableRow("header_" + dateStr, headerCells, null));

            // Add Log Rows
            List<BackgroundJobLog> dayLogs = groupedLogs.get(dateStr);
            if (dayLogs != null) {
                for (BackgroundJobLog log : dayLogs) {
                    List<TableCell> cells = new ArrayList<>();
                    cells.add(new TableCell("time", timeFormat.format(new Date(log.getLastUpdatedTimestamp())), null, TableColumnType.TEXT));
                    cells.add(new TableCell("step", log.getCurrentStep(), null, TableColumnType.TEXT));
                    
                    String statusEmoji = "⚙️";
                    if ("SUCCESS".equals(log.getStatus())) statusEmoji = "✅";
                    else if ("FAILED".equals(log.getStatus())) statusEmoji = "❌";
                    else if ("RUNNING".equals(log.getStatus())) statusEmoji = "🕒";

                    cells.add(new TableCell("status", statusEmoji + " " + log.getStatus(), null, TableColumnType.TEXT));
                    rows.add(new TableRow(log.getJobId() + log.getLastUpdatedTimestamp(), cells, log));
                }
            }
        }
        adapter.setRows(rows);
    }
}
