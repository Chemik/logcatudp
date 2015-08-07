package sk.madzik.android.logcatudp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class LogcatUdpReceiver extends BroadcastReceiver {
    private static final String TAG = "LogcaUdpReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Boot completed; starting service ...");
        SharedPreferences settings = context.getSharedPreferences(LogcatUdpCfg.Preferences.PREFS_NAME, Context.MODE_PRIVATE);

        if (settings.getBoolean(LogcatUdpCfg.Preferences.AUTO_START, true)) {
            context.startService(new Intent(context, LogcatUdpService.class));
            Log.i(TAG, "Starting Service");
        }
    }
}
