package ug.go.health.hrmattend.services;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;
import androidx.work.ListenableWorker.Result;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.gotev.uploadservice.HttpUploadRequest;
import net.gotev.uploadservice.HttpUploadTask;
import net.gotev.uploadservice.UploadService;
import net.gotev.uploadservice.UploadTask;
import net.gotev.uploadservice.UploadTaskListener;

import ug.go.health.hrmattend.services.SessionService;
import ug.go.health.hrmattend.models.DeviceSettings;
import ug.go.health.hrmattend.utils.Constants;

public class StaffPictureUploadService extends ListenableWorker {
    /**
     * @param appContext   The application {@link Context}
     * @param workerParams Parameters to setup the internal state of this worker
     */
    public StaffPictureUploadService(@NonNull Context appContext, @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
    }

    private static final String TAG = "StaffPictureUploadService";
    private static final String IMAGE_DIR = Environment.DIRECTORY_PICTURES + File.separator + "iHRIS Biometric/Staff Images";

    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        return Futures.immediateFuture(uploadImages());
    }

    private Result uploadImages() {
        SessionService session = new SessionService(getApplicationContext());
        DeviceSettings deviceSettings = session.getDeviceSettings();
        String baseUrl = deviceSettings.getServerUrl();

        File imageDir;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            imageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "iHRIS Biometric/Staff Images");
        } else {
            imageDir = new File(Constants.getImageDir(getApplicationContext()) + File.separator + "Staff Images");
        }
        if (!imageDir.exists() || !imageDir.isDirectory()) {
            Log.e(TAG, "Image directory does not exist or is not a directory");
            return Result.failure();
        }

        File[] imageFiles = imageDir.listFiles();
        if (imageFiles == null || imageFiles.length == 0) {
            Log.d(TAG, "No images to upload");
            return Result.success();
        }

        UploadService.NAMESPACE = "ug.go.health.hrmattend";
        UploadService.HTTP_STACK = new HttpUploadTask();

        for (File imageFile : imageFiles) {
            if (imageFile.isFile() && imageFile.getName().endsWith(".jpg")) {
                try {
                    HttpUploadRequest request = new HttpUploadRequest(getApplicationContext(), baseUrl, imageFile.getName())
                            .addFileToUpload(imageFile.getAbsolutePath(), "image/jpeg", "file")
                            .setNotificationConfig((context, uploadId) -> new UploadService.NotificationConfig(R.string.app_name))
                            .setAutoDeleteFilesAfterSuccessfulUpload(true)
                            .setUsesFixedLengthStreamingMode(true);

                    request.startUpload();
                    Log.d(TAG, "Upload started: " + imageFile.getName());
                } catch (Exception e) {
                    Log.e(TAG, "Error starting upload for image: " + imageFile.getName(), e);
                }
            }
        }

        return Result.success();
    }
}
