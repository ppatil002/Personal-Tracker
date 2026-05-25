package com.example.pp.data.remote;

import android.content.Context;
import android.util.Log;

import com.example.pp.BuildConfig;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

public class GoogleDriveClient {

    private static final String TAG = "GoogleDriveClient";
    private static volatile GoogleDriveClient instance;
    private Drive driveService;

    private GoogleDriveClient(Context context) {
        init(context);
    }

    private void init(Context context) {
        Context appContext = context.getApplicationContext();
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(appContext);
        if (account != null) {
            setupDriveService(appContext, account);
        }
    }

    private void setupDriveService(Context context, GoogleSignInAccount account) {
        Context appContext = context.getApplicationContext();
        try {
            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                    appContext, Collections.singleton(DriveScopes.DRIVE_FILE));
            credential.setSelectedAccount(account.getAccount());

            driveService = new Drive.Builder(
                    AndroidHttp.newCompatibleTransport(),
                    new GsonFactory(),
                    credential)
                    .setApplicationName("Personal Tracker")
                    .build();
        } catch (Exception e) {
            Log.e(TAG, "Failed to setup Drive service: " + e.getMessage());
        }
    }

    public void refresh(Context context) {
        init(context.getApplicationContext());
    }

    public void signInSilently(Context context, Runnable onComplete) {
        Context appContext = context.getApplicationContext();
        getFullGoogleSignInClient(appContext).silentSignIn()
                .addOnSuccessListener(account -> {
                    setupDriveService(appContext, account);
                    if (onComplete != null) onComplete.run();
                })
                .addOnFailureListener(e -> {
                    if (onComplete != null) onComplete.run();
                });
    }

    public boolean authorizeSync(Context context) {
        if (isAuthorized()) return true;

        Context appContext = context.getApplicationContext();
        try {
            GoogleSignInAccount account = Tasks.await(
                    getFullGoogleSignInClient(appContext).silentSignIn());
            if (account != null) {
                setupDriveService(appContext, account);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Blocking silent sign-in failed: " + e.getMessage());
        }
        return false;
    }

    public static GoogleDriveClient getInstance(Context context) {
        if (instance == null) {
            synchronized (GoogleDriveClient.class) {
                if (instance == null) {
                    instance = new GoogleDriveClient(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public static com.google.android.gms.common.api.Scope getDriveScope() {
        return new com.google.android.gms.common.api.Scope(DriveScopes.DRIVE_FILE);
    }

    public static GoogleSignInClient getFullGoogleSignInClient(Context context) {
        Context appContext = context.getApplicationContext();
        GoogleSignInOptions.Builder builder = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .requestScopes(getDriveScope());

        String webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID;
        if (webClientId != null && !webClientId.isEmpty()) {
            builder.requestIdToken(webClientId);
        }

        return GoogleSignIn.getClient(appContext, builder.build());
    }

    @Deprecated
    public static GoogleSignInClient getGoogleSignInClient(Context context) {
        return getFullGoogleSignInClient(context);
    }

    public boolean isAuthorized() {
        return driveService != null;
    }

    public String uploadFile(java.io.File localFile, String remoteName) throws IOException {
        if (driveService == null) throw new IOException("Drive service not initialized");

        String folderId = getOrCreateFolder(BuildConfig.BACKUP_PATH);
        String existingFileId = findFileIdByName(remoteName, folderId);
        FileContent content = new FileContent("application/octet-stream", localFile);

        if (existingFileId != null) {
            File googleFile = driveService.files().update(existingFileId, new File(), content).execute();
            return googleFile.getId();
        } else {
            File metadata = new File()
                    .setName(remoteName)
                    .setParents(Collections.singletonList(folderId));

            File googleFile = driveService.files().create(metadata, content).execute();
            if (googleFile == null) {
                throw new IOException("Null response from Drive when creating file");
            }
            return googleFile.getId();
        }
    }

    public void downloadFile(String remoteName, java.io.File localFile) throws IOException {
        if (driveService == null) throw new IOException("Drive service not initialized");

        String folderId = getOrCreateFolder(BuildConfig.BACKUP_PATH);
        String fileId = findFileIdByName(remoteName, folderId);
        if (fileId == null) throw new IOException("File not found on Drive: " + remoteName);

        OutputStream outputStream = new FileOutputStream(localFile);
        driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream);
        outputStream.close();
    }

    private String getOrCreateFolder(String path) throws IOException {
        if (path == null || path.isEmpty()) return "root";
        
        String[] folders = path.split("/");
        String parentId = "root";
        for (String folderName : folders) {
            if (folderName.isEmpty()) continue;
            parentId = getOrCreateSingleFolder(folderName, parentId);
        }
        return parentId;
    }

    private String getOrCreateSingleFolder(String name, String parentId) throws IOException {
        String query = "name = '" + name + "' and mimeType = 'application/vnd.google-apps.folder' and '" + parentId + "' in parents and trashed = false";
        FileList result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id)")
                .execute();

        List<File> files = result.getFiles();
        if (files != null && !files.isEmpty()) {
            return files.get(0).getId();
        }

        File metadata = new File()
                .setName(name)
                .setMimeType("application/vnd.google-apps.folder")
                .setParents(Collections.singletonList(parentId));

        File folder = driveService.files().create(metadata).setFields("id").execute();
        return folder.getId();
    }

    private String findFileIdByName(String name, String parentId) throws IOException {
        String query = "name = '" + name + "' and '" + parentId + "' in parents and trashed = false";
        FileList result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute();

        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            return null;
        }
        return files.get(0).getId();
    }
}
