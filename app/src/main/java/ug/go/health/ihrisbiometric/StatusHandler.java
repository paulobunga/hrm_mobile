package ug.go.health.hrmattend;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class StatusHandler extends Handler {

    private static final String TAG = "STATUS_HANDLER";

    public StatusHandler() {}

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        // Log.d(TAG, "HANDLE MESSAGE: " + msg.what);
    }

}
