package ug.go.health.hrmattend.services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;
import androidx.work.ListenableWorker.Result;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class StaffPictureUploadService extends ListenableWorker {
    /**
     * @param appContext   The application {@link Context}
     * @param workerParams Parameters to setup the internal state of this worker
     */
    public StaffPictureUploadService(@NonNull Context appContext, @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
    }

    private static final String TAG = "StaffPictureUploadService";
    private static final String IMAGE_DIR = "path_to_staff_images_directory"; // Replace with actual path
    private static final String SERVER_URL = "http://yourserver.com/upload"; // Replace with actual server URL

    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        return Futures.immediateFuture(uploadImages());
    }

    private Result uploadImages() {
        File imageDir = new File(getApplicationContext().getFilesDir(), IMAGE_DIR);
        if (!imageDir.exists() || !imageDir.isDirectory()) {
            Log.e(TAG, "Image directory does not exist or is not a directory");
            return Result.failure();
        }

        File[] imageFiles = imageDir.listFiles();
        if (imageFiles == null || imageFiles.length == 0) {
            Log.d(TAG, "No images to upload");
            return Result.success();
        }

        OkHttpClient client = new OkHttpClient();
        List<File> uploadedFiles = new ArrayList<>();

        for (File imageFile : imageFiles) {
            if (imageFile.isFile() && imageFile.getName().endsWith(".jpg")) {
                try {
                    RequestBody requestBody = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("file", imageFile.getName(),
                                    RequestBody.create(MediaType.parse("image/jpeg"), imageFile))
                            .build();

                    Request request = new Request.Builder()
                            .url(SERVER_URL)
                            .post(requestBody)
                            .build();

                    Response response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        uploadedFiles.add(imageFile);
                        Log.d(TAG, "Uploaded: " + imageFile.getName());
                    } else {
                        Log.e(TAG, "Failed to upload: " + imageFile.getName());
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error uploading image: " + imageFile.getName(), e);
                }
            }
        }

        // Optionally, delete uploaded files
        for (File uploadedFile : uploadedFiles) {
            if (!uploadedFile.delete()) {
                Log.w(TAG, "Failed to delete: " + uploadedFile.getName());
            }
        }

        return Result.success();
    }
}
