package com.example.pp.features.backup;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pp.R;
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
import java.util.List;
import java.util.Locale;

public class BackupActivity extends AppCompatActivity {

    private BackupViewModel viewModel;
    private GenericTableAdapter adapter;
    private TextView tvStatus;
    private View btnSignIn;

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
                viewModel.runRestore("backup_slot_" + slot + ".enc");
            } else {
                Toast.makeText(this, "Please sign in to Google Drive first", Toast.LENGTH_SHORT).show();
            }
        });

        setupLogTable();
        GoogleDriveClient.getInstance(this).signInSilently(this, this::updateSignInStatus);
        viewModel.getLogs().observe(this, this::updateLogs);
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
        
        TableColumn timeCol = new TableColumn("time", "Time", TableColumn.ColumnType.TEXT);
        timeCol.setWidthWeight(2);
        
        TableColumn stepCol = new TableColumn("step", "Step", TableColumn.ColumnType.TEXT);
        stepCol.setWidthWeight(3);
        
        TableColumn statusCol = new TableColumn("status", "Status", TableColumn.ColumnType.TEXT);
        statusCol.setWidthWeight(2);

        List<TableColumn> columns = Arrays.asList(timeCol, stepCol, statusCol);
        adapter.setColumns(columns);
        rvLogs.setAdapter(adapter);
    }

    private void updateLogs(List<BackgroundJobLog> logs) {
        List<TableRow> rows = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

        for (BackgroundJobLog log : logs) {
            List<TableCell> cells = new ArrayList<>();
            cells.add(new TableCell("time", sdf.format(new Date(log.getLastUpdatedTimestamp())), null, TableColumn.ColumnType.TEXT));
            cells.add(new TableCell("step", log.getCurrentStep(), null, TableColumn.ColumnType.TEXT));
            
            String statusEmoji = "⚙️";
            if ("SUCCESS".equals(log.getStatus())) statusEmoji = "✅";
            else if ("FAILED".equals(log.getStatus())) statusEmoji = "❌";
            else if ("RUNNING".equals(log.getStatus())) statusEmoji = "🕒";

            cells.add(new TableCell("status", statusEmoji + " " + log.getStatus(), null, TableColumn.ColumnType.TEXT));
            
            rows.add(new TableRow(log.getJobId() + log.getLastUpdatedTimestamp(), cells, log));
        }
        adapter.setRows(rows);
    }
}
