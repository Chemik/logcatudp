package sk.madzik.android.logcatudp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import android.util.Log;

import sk.madzik.android.logcatudp.LogcatUdpService.Config;

public class LogcatThread extends Thread {
    private static final String TAG = "LogcatThread";
    private DatagramSocket mSocket = null;
    private Config mConfig = null;

    LogcatThread(DatagramSocket socket, Config config) {
        Log.d(TAG, "Thread constructed.");
        if (socket == null || config == null) {
            throw new NullPointerException();
        }
        mSocket = socket;
        mConfig = config;
    }

    public void run() {
        Log.d(TAG, "started");
        try {
            String procString = "logcat -v " + mConfig.mLogFormat;
            if (mConfig.mUseFilter && mConfig.mFilter.trim().length() > 0) {
                procString += " *:s " + mConfig.mFilter;
            }
            Process process = Runtime.getRuntime().exec(procString);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String logLine;
            boolean socketFailed = false;
            InetAddress destAddress = InetAddress.getByName(mConfig.mDestServer);
            while (true) {
                String sendingLine = "";
                // assume that log writes whole lines
                if (bufferedReader.ready()) {
                    logLine = bufferedReader.readLine();
                    if (mConfig.mSendIds) {
                        sendingLine = mConfig.mDevId + ": ";
                    }
                    sendingLine += logLine + System.getProperty("line.separator");
                    DatagramPacket packet = new DatagramPacket(sendingLine.getBytes(), sendingLine.length(),
                            destAddress, mConfig.mDestPort);
                    try {
                        mSocket.send(packet);
                        if (socketFailed) {
                            Log.d(TAG, "socket back online");
                            socketFailed = false;
                        }
                        sendingLine = "";
                    } catch (SocketException e) {
                        // it's OK, line was remembered
                        if (!socketFailed) {
                            Log.d(TAG, "socket send failed " + e);
                            socketFailed = true;
                        }
                    }
                }
                if (isInterrupted()) {
                    Log.d(TAG, "interupted.");
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Log.d(TAG, "stopped.");
        }
    }
}
