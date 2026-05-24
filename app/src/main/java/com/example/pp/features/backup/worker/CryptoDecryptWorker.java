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
import java.util.zip.GZIPInputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoDecryptWorker extends BaseFeatureWorker {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;

    public CryptoDecryptWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String encryptedPath = getInputData().getString("ENCRYPTED_FILE_PATH");
        if (encryptedPath == null) {
            updateLog("FAILED", "DECRYPT_ERROR", "Encrypted file path missing.");
            return Result.failure();
        }

        updateLog("RUNNING", "DECRYPTING", "Decrypting and decompressing the backup stream.");

        try {
            File encryptedFile = new File(encryptedPath);
            File decryptedFile = new File(getApplicationContext().getCacheDir(), "restored_data.json");

            try (InputStream in = new FileInputStream(encryptedFile)) {
                // Read IV from header
                byte[] iv = new byte[IV_LENGTH_BYTE];
                if (in.read(iv) != IV_LENGTH_BYTE) {
                    throw new Exception("Invalid IV in file header.");
                }

                SecretKeySpec keySpec = new SecretKeySpec(BuildConfig.BACKUP_SECRET_KEY.getBytes(), "AES");
                Cipher cipher = Cipher.getInstance(ALGORITHM);
                cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

                try (CipherInputStream cipherIn = new CipherInputStream(in, cipher);
                     GZIPInputStream gzipIn = new GZIPInputStream(cipherIn);
                     OutputStream out = new FileOutputStream(decryptedFile)) {
                    
                    byte[] buffer = new byte[4096];
                    int nread;
                    while ((nread = gzipIn.read(buffer)) > 0) {
                        out.write(buffer, 0, nread);
                    }
                }
            }

            updateLog("SUCCESS", "DECRYPT_COMPLETE", "Data decrypted successfully to restored_data.json");
            
            Data outputData = new Data.Builder()
                    .putString("RESTORED_FILE_PATH", decryptedFile.getAbsolutePath())
                    .build();
            return Result.success(outputData);

        } catch (Exception e) {
            updateLog("FAILED", "DECRYPT_ERROR", "Decryption failed: " + e.getMessage());
            return Result.failure();
        }
    }
}
