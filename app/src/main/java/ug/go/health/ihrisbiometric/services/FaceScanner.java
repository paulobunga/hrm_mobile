package ug.go.health.ihrisbiometric.services;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

import ug.go.health.ihrisbiometric.JniHelper;
import ug.go.health.ihrisbiometric.models.FaceInfo;
import ug.go.health.ihrisbiometric.models.FaceScannerResult;
import ug.go.health.ihrisbiometric.utils.Constants;
import ug.go.health.ihrisbiometric.utils.ImageConverter;

public class FaceScanner {
    private static final String TAG = "FaceScanner";
    private Context context;
    private ImageConverter imageConverter;
    public static FaceScannerResult result;

    static {
        System.loadLibrary("opencv_java4");
    }

    public int processImage(Mat mRgbFrame, FaceScannerResult result) {

        float[] faceBoxes = new float[15];

        JniHelper.getInstance().DetectFace(mRgbFrame.getNativeObjAddr(), faceBoxes);
        Log.d(TAG, "processImage: Resulting Boxes ===> " + Arrays.toString(faceBoxes));

        if (faceBoxes[0] != 1.0f) {
            return 0;
        }

        String faceRecognitionResult = JniHelper.getInstance().FaceRecognition(mRgbFrame.getNativeObjAddr(), faceBoxes);
        Log.d(TAG, "Face detected: " + faceRecognitionResult);

        try {
            JSONObject jsonObject = new JSONObject(faceRecognitionResult);
            String status = jsonObject.getString("status");
            FaceInfo faceInfo = result.faceInfo;

            switch (status) {
                case "NO_FACE_DETECTED":
                    faceInfo.faceDetected = false;
                    break;
                case "NOT_ENROLLED":
                    faceInfo.isEnrolled = false;
                    break;
                case "USER_ENROLLED":
                    faceInfo.faceDetected = true;
                    faceInfo.isEnrolled = true;
                    faceInfo.ihrisPID = jsonObject.optString("name", null);
                    break;
            }

            float livelinessScore = JniHelper.getInstance().CheckLiveliness(mRgbFrame.getNativeObjAddr(), faceBoxes);
            faceInfo.isLive = livelinessScore > 0.5f;
            Log.d(TAG, "processImage: We have received a liveness score of ==> " + livelinessScore);
            Log.d(TAG, faceInfo.isLive ? "processImage: This face is a real stream" : "processImage: This face is a still image");

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return 1;
    }


    public void initEngine(Context context) {
        this.context = context;
        this.imageConverter = new ImageConverter(context);
        AssetManager assetManager = context.getAssets();
        String modelDir = Constants.getModelDir(context);

        for (String modelName : Constants.getModelList()) {
            File modelFile = new File(modelDir, modelName);

            if (!modelFile.exists()) {
                try (InputStream inputStream = assetManager.open(modelName);
                     FileOutputStream outputStream = new FileOutputStream(modelFile)) {

                    byte[] buffer = new byte[4096];
                    int length;

                    while ((length = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length);
                    }

                    Log.d(TAG, "File copied successfully: " + modelFile.getAbsolutePath());
                } catch (IOException e) {
                    Log.e(TAG, "Error copying file: " + modelName, e);
                }
            } else {
                Log.d(TAG, "File already exists: " + modelFile.getAbsolutePath());
            }
        }

        String workDir = Constants.getWorkDir(context) + File.separator + "FACE_DB";
        File faceDbDir = new File(workDir);

        if (!faceDbDir.exists()) {
            faceDbDir.mkdir();
        }

        // Log the face database directory path
        Log.d(TAG, "Face database directory: " + faceDbDir.getAbsolutePath());

        JniHelper.getInstance().InitFaceEngine(modelDir, workDir);
    }

    public void saveImageToPictures(Context context, Bitmap bitmap, String imageName) {
        Uri uri = Build.VERSION.SDK_INT >= 29 ? MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        Uri imageUri = null;

        try (Cursor cursor = context.getContentResolver().query(uri, new String[]{"_id"}, "_display_name = ?", new String[]{imageName}, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                imageUri = ContentUris.withAppendedId(uri, cursor.getLong(cursor.getColumnIndexOrThrow("_id")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (imageUri == null) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, imageName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
            imageUri = context.getContentResolver().insert(uri, values);
        }

        if (imageUri != null) {
            try (OutputStream outputStream = context.getContentResolver().openOutputStream(imageUri)) {
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean registerFace(Mat mRgbFrame, String userId) {
        if (userId.isEmpty()) {
            return false;
        }

        Imgproc.cvtColor(mRgbFrame, mRgbFrame, Imgproc.COLOR_BGR2RGB);

        float[] faceBoxes = new float[15];
        JniHelper.getInstance().DetectFace(mRgbFrame.getNativeObjAddr(), faceBoxes);
        JniHelper.getInstance().FaceRegister(mRgbFrame.getNativeObjAddr(), userId, faceBoxes);

        return true;
    }

    public String saveEnrolledFaceImage(Mat mRgbFrame, String userId) {
        Bitmap bitmap = Bitmap.createBitmap(mRgbFrame.cols(), mRgbFrame.rows(), Bitmap.Config.ARGB_8888);
        org.opencv.android.Utils.matToBitmap(mRgbFrame, bitmap);

        String fileName = userId + ".jpg";
        File directory = new File(Environment.getExternalStorageDirectory(), "iHRIS Biometric/Staff Images");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File file = new File(directory, fileName);
        String filePath = file.getAbsolutePath();

        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            Log.d(TAG, "Face image saved successfully: " + filePath);
            return filePath;
        } catch (IOException e) {
            Log.e(TAG, "Error saving face image", e);
            return null;
        }
    }
}
