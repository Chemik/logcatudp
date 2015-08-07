package sk.madzik.android.logcatudp;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * Class provides additional functionality on rooted android
 */
public final class RootUtils {
    public static final String TAG = "LogcatUdpRootUtils";

    /**
     * static class
     */
    private RootUtils() {
    }

    public static boolean haveReadLogsPermission(Context context) {
        PackageManager pm = context.getPackageManager();
        return pm.checkPermission(android.Manifest.permission.READ_LOGS, context.getPackageName()) == 0;
    }

    public static void setReadLogsPermission(Context context) {
        if (haveReadLogsPermission(context)) {
            Log.d(TAG, "we have the READ_LOGS permission already!");
        } else {
            String pname = context.getPackageName();
            String[] CMDLINE_GRANTPERMS = {"su", "-c", null};
            Log.d(TAG, "we do not have the READ_LOGS permission. Trying to acquire...");
            if (android.os.Build.VERSION.SDK_INT >= 16) {
                Log.d(TAG, "Working around JellyBeans 'feature'...");
                try {
                    // format the commandline parameter
                    CMDLINE_GRANTPERMS[2] = String.format("pm grant %s android.permission.READ_LOGS", pname);
                    java.lang.Process p = Runtime.getRuntime().exec(CMDLINE_GRANTPERMS);
                    int res = p.waitFor();
                    Log.d(TAG, "exec returned: " + res);
                    if (res != 0)
                        throw new Exception("failed to become root");
                } catch (Exception e) {
                    Log.d(TAG, "exec(): " + e);
                    Toast.makeText(context, "Failed to obtain READ_LOGS permission", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public static boolean isDeviceRooted() {
        return checkRootMethod1() || checkRootMethod2() || checkRootMethod3();
    }

    private static boolean checkRootMethod1() {
        String buildTags = android.os.Build.TAGS;
        return buildTags != null && buildTags.contains("test-keys");
    }

    private static boolean checkRootMethod2() {
        String[] paths = {"/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
                "/system/bin/failsafe/su", "/data/local/su"};
        for (String path : paths) {
            if (new File(path).exists()) return true;
        }
        return false;
    }

    private static boolean checkRootMethod3() {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[]{"/system/xbin/which", "su"});
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            return in.readLine() != null;
        } catch (Throwable t) {
            return false;
        } finally {
            if (process != null) process.destroy();
        }
    }
}
