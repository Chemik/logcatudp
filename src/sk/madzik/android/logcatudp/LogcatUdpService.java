package sk.madzik.android.logcatudp;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class LogcatUdpService extends Service {
	public static final String TAG = "LogcatUdpService";
	public static boolean isRunning = false;

	@Override
	public void onCreate() {
		super.onCreate();

		Log.d(TAG, TAG+" started");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, TAG+" stopping.");
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

}
