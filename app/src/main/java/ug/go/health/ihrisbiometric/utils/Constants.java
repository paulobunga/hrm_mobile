package ug.go.health.ihrisbiometric.utils;

import android.content.Context;
import android.os.Environment;
import java.io.File;

public final class Constants {
    public static final String WORK_DIR_NAME = "ihris_biometric";
    public static final String WORK_DIR = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + WORK_DIR_NAME;

    private Constants() {
    }

    public static String getWorkDir(Context context) {
        File externalFilesDir = context.getExternalFilesDir(WORK_DIR_NAME);
        if (externalFilesDir == null) {
            return null;
        }
        if (!externalFilesDir.exists()) {
            externalFilesDir.mkdirs();
        }
        return externalFilesDir.getAbsolutePath();
    }

    public static String getModelDir(Context context) {
        String modelDirPath = getWorkDir(context) + File.separator + "model";
        File modelDir = new File(modelDirPath);
        if (!modelDir.exists()) {
            modelDir.mkdirs();
        }
        return modelDirPath;
    }

    public static String getImageDir(Context context) {
        String imageDirPath = getWorkDir(context) + File.separator + "library";
        File imageDir = new File(imageDirPath);
        if (!imageDir.exists()) {
            imageDir.mkdirs();
        }
        return imageDirPath;
    }

    public static String getImageListPath(Context context) {
        return getImageDir(context) + File.separator + "imagelist.dat";
    }

    public static String[] getModelList() {
        return new String[]{"det_500m_480.onnx", "mbf.onnx", "27_80.onnx", "40_80.onnx"};
    }
}
