package sk.madzik.android.logcatudp;

import java.net.DatagramSocket;
import java.net.SocketException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.Log;

public class LogcatUdpService extends Service {
    public static final String TAG = "LogcatUdpService";
    public static boolean isRunning = false;

    class Config {
        boolean mSendIds;
        String mDevId;
        String mDestServer;
        int mDestPort;
        boolean mUseFilter;
        String mFilter;
        String mLogFormat;
    }

    private Config mConfig = null;

    private DatagramSocket mSocket = null;
    private LogcatThread mLogcatThread = null;
    private NotificationManager mNotificationManager = null;
    private int SERVICE_NOTIFICATION_ID = 1;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, TAG + " started");

        // get configuration
        mConfig = new Config();
        SharedPreferences settings = getSharedPreferences(LogcatUdpCfg.Preferences.PREFS_NAME, Context.MODE_PRIVATE);
        mConfig.mSendIds = settings.getBoolean(LogcatUdpCfg.Preferences.SEND_IDS, false);
        String android_ID = Secure.getString(this.getContentResolver(), Secure.ANDROID_ID);
        if (TextUtils.isEmpty(android_ID))
            android_ID = "emulator";
        mConfig.mDevId = settings.getString(LogcatUdpCfg.Preferences.DEV_ID, android_ID);
        mConfig.mDestServer = settings.getString(LogcatUdpCfg.Preferences.DEST_SERVER, LogcatUdpCfg.DEF_SERVER);
        mConfig.mDestPort = settings.getInt(LogcatUdpCfg.Preferences.DEST_PORT, LogcatUdpCfg.DEF_PORT);
        mConfig.mUseFilter = settings.getBoolean(LogcatUdpCfg.Preferences.USE_FILTER, false);
        mConfig.mFilter = settings.getString(LogcatUdpCfg.Preferences.FILTER_TEXT, "");
        mConfig.mLogFormat = settings.getString(LogcatUdpCfg.Preferences.LOG_FORMAT, "");

        // status bar notification icon manager
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        int icon = R.drawable.ic_stat_notif;
        Notification notif = new Notification(icon, this.getString(R.string.notif_text), System.currentTimeMillis());
        notif.flags |= Notification.FLAG_ONGOING_EVENT;
        notif.flags |= Notification.FLAG_NO_CLEAR;
        notif.setLatestEventInfo(
                getApplicationContext(),
                this.getString(R.string.notif_text),
                this.getString(R.string.notif_message),
                PendingIntent.getActivity(this, 0, new Intent(this, LogcatUdpCfg.class), 0));
        mNotificationManager.notify(SERVICE_NOTIFICATION_ID, notif);

        try {
            mSocket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
            Log.e(TAG, "Socket creation failed!");
            stopSelf();
        }
        mLogcatThread = new LogcatThread(mSocket, mConfig);
        mLogcatThread.start();

        isRunning = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, TAG + " stopping.");
        if (mLogcatThread != null) {
            mLogcatThread.interrupt();
            try {
                mLogcatThread.join(1000);
                if (mLogcatThread.isAlive()) {
                    // TODO: Display "force close/wait" dialog
                    mLogcatThread.join();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.w(TAG, "Joining logcat thread exception.");
            }
        }
        mNotificationManager.cancelAll();
        isRunning = false;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

}
