package sk.madzik.android.logcatudp;

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

	private boolean mSendIds;
	private String mDevId;
	private String mDestServer;
	private int mDestPort;

	@Override
	public void onCreate() {
		super.onCreate();

		Log.d(TAG, TAG+" started");

		// get configuration
		SharedPreferences settings = getSharedPreferences(LogcatUdpCfg.Preferences.PREFS_NAME, Context.MODE_PRIVATE);
		mSendIds = settings.getBoolean(LogcatUdpCfg.Preferences.SEND_IDS, false);
		String android_ID = Secure.getString(this.getContentResolver(), Secure.ANDROID_ID);
		if (TextUtils.isEmpty(android_ID))
			android_ID = "emulator";
		mDevId = settings.getString(LogcatUdpCfg.Preferences.DEV_ID, android_ID);
		mDestServer = settings.getString(LogcatUdpCfg.Preferences.DEST_SERVER, LogcatUdpCfg.DEF_SERVER);
		mDestPort = settings.getInt(LogcatUdpCfg.Preferences.DEST_PORT, LogcatUdpCfg.DEF_PORT);

		// TODO: notification on statusbar
		// TODO: read logcat, open udp port, send lines (thread)

		isRunning = true;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, TAG+" stopping.");
		isRunning = false;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

}
