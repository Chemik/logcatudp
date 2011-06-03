package sk.madzik.android.logcatudp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import android.util.Log;
import sk.madzik.android.logcatudp.LogcatUdpService.Config;

public class LogcatThread extends Thread {
	private static final String TAG = "LogcatThread";
	private DatagramSocket mSocket = null;
	private Config mConfig = null;

	LogcatThread( DatagramSocket socket, Config config ) {
		Log.d( TAG, "Thread constructed." );
		if ( socket == null || config == null ) {
			throw new NullPointerException();
		}
		mSocket = socket;
		mConfig = config;
	}

	public void run() {
		Log.d(TAG, "started");
		try {
			String procString = "logcat";
			if ( mConfig.mUseFilter && mConfig.mFilter.trim().length()>0 ) {
				procString += " *:s " + mConfig.mFilter;
			}
			Process process = Runtime.getRuntime().exec( procString );
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String logLine;
			while ( (logLine = bufferedReader.readLine()) != null ) {
				String sendingLine = "";
				if ( mConfig.mSendIds ) {
					sendingLine = mConfig.mDevId + ": ";
				}
				sendingLine += logLine + System.getProperty("line.separator");
				DatagramPacket packet = new DatagramPacket(sendingLine.getBytes(), sendingLine.length(),
						InetAddress.getByName(mConfig.mDestServer), mConfig.mDestPort);
				mSocket.send(packet);
				if ( isInterrupted() ) {
					Log.d( TAG, "interupted." );
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			Log.d( TAG, "stopped." );
		}
	}
}
