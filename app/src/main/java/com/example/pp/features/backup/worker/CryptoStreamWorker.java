package com.example.pp.features.backup.worker;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;

import com.example.pp.BuildConfig;
import com.example.pp.core.orchestrator.BaseFeatureWorker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.zip.GZIPOutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoStreamWorker extends BaseFeatureWorker {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;

    public CryptoStreamWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String rawPath = getInputData().getString("RAW_FILE_PATH");
        if (rawPath == null) {
            updateLog("FAILED", "CRYPTO_ERROR", "Raw path missing.");
            failTask("Raw path missing in encryption stage.");
            return Result.failure();
        }

        updateLog("RUNNING", "ENCRYPTING", "Applying AES-GCM encryption.");
        updateTaskProgress(66);

        try {
            File rawFile = new File(rawPath);
            int slotIndex = LocalDate.now().get(WeekFields.of(Locale.getDefault()).weekOfYear()) % 4;
            String fileName = "backup_slot_" + slotIndex + ".enc";

            File backupDir = new File(getApplicationContext().getExternalFilesDir(null), BuildConfig.BACKUP_PATH);
            if (!backupDir.exists()) backupDir.mkdirs();

            File encryptedFile = new File(backupDir, fileName);

            byte[] iv = new byte[IV_LENGTH_BYTE];
            new SecureRandom().nextBytes(iv);

            SecretKeySpec keySpec = new SecretKeySpec(BuildConfig.BACKUP_SECRET_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

            try (InputStream in = new FileInputStream(rawFile);
                 OutputStream out = new FileOutputStream(encryptedFile)) {
                out.write(iv);
                try (CipherOutputStream cipherOut = new CipherOutputStream(out, cipher);
                     GZIPOutputStream gzipOut = new GZIPOutputStream(cipherOut)) {
                    byte[] buffer = new byte[4096];
                    int nread;
                    while ((nread = in.read(buffer)) > 0) {
                        gzipOut.write(buffer, 0, nread);
                    }
                }
            }

            updateLog("SUCCESS", "CRYPTO_COMPLETE", "Encryption complete.");

            Data outputData = new Data.Builder()
                    .putString("ENCRYPTED_FILE_PATH", encryptedFile.getAbsolutePath())
                    .putString("REMOTE_FILE_NAME", fileName)
                    .putString("TASK_ID", taskId)
                    .build();

            return Result.success(outputData);

        } catch (Exception e) {
            updateLog("FAILED", "CRYPTO_ERROR", e.getMessage());
            failTask(e.getMessage());
            return Result.failure();
        }
    }
}
