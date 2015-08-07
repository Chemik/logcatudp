package sk.madzik.android.logcatudp;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class LogcatUdpCfg extends Activity {
    public static final String TAG = "LogcatUdpCfg";

    private static final int MENU_SAVE = Menu.FIRST + 1;
    private static final int MENU_CANCEL = Menu.FIRST + 2;
    private static final int MENU_CLR_LOG = Menu.FIRST + 3;
    private static final int MENU_SHARE = Menu.FIRST + 4;

    static final String DEF_SERVER = "192.168.1.10";
    static final int DEF_PORT = 10009;

    static final String DEF_FORMAT = "brief";

    private boolean cancelSave = false;

    private SharedPreferences mSettings;

    private CheckBox chkSendIds;
    private EditText txtDevId;
    private EditText txtServer;
    private EditText txtPort;
    private CheckBox chkUseFilter;
    private EditText txtFilter;
    private CheckBox chkAutoStart;

    private Button btnActivateService;
    private Button btnDeactivateService;
    private Button btnGrantLogs;

    private Spinner spinLogFormat;

    ProgressDialog prgDialog;

    public class Preferences {
        public static final String PREFS_NAME = "LogcatUdp";
        public static final String SEND_IDS = "SendIds";
        public static final String DEV_ID = "DeviceID";
        public static final String DEST_SERVER = "DestServer";
        public static final String DEST_PORT = "DestPort";
        public static final String AUTO_START = "AutoStart";
        public static final String USE_FILTER = "UseFilter";
        public static final String FILTER_TEXT = "FilterText";
        public static final String LOG_FORMAT = "LogFormat";
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "started");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        chkSendIds = (CheckBox) findViewById(R.id.chkSendIds);
        txtDevId = (EditText) findViewById(R.id.txtID);
        txtServer = (EditText) findViewById(R.id.txtServer);
        txtPort = (EditText) findViewById(R.id.txtPort);
        spinLogFormat = (Spinner) findViewById(R.id.spinLogFormat);
        chkUseFilter = (CheckBox) findViewById(R.id.chkUseFilter);
        txtFilter = (EditText) findViewById(R.id.txtFilter);
        chkAutoStart = (CheckBox) findViewById(R.id.chkAutoStart);

        mSettings = getSharedPreferences(Preferences.PREFS_NAME, MODE_PRIVATE);

        // set send ID (un)checked
        chkSendIds.setChecked(mSettings.getBoolean(Preferences.SEND_IDS, false));
        // enable/disable ID editbox
        if (!chkSendIds.isChecked()) {
            findViewById(R.id.lblID).setVisibility(View.GONE);
            txtDevId.setVisibility(View.GONE);
        }
        chkSendIds.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                findViewById(R.id.lblID).setVisibility((isChecked ? View.VISIBLE : View.GONE));
                txtDevId.setVisibility((isChecked ? View.VISIBLE : View.GONE));
            }
        });

        // set text in ID editbox
        String android_ID = Secure.getString(this.getContentResolver(), Secure.ANDROID_ID);
        if (TextUtils.isEmpty(android_ID))
            android_ID = "emulator";
        txtDevId.setText("" + mSettings.getString(Preferences.DEV_ID, android_ID));

        txtServer.setText("" + mSettings.getString(Preferences.DEST_SERVER, DEF_SERVER));
        txtPort.setText("" + mSettings.getInt(Preferences.DEST_PORT, DEF_PORT));
        chkAutoStart.setChecked(mSettings.getBoolean(Preferences.AUTO_START, true));

        // set log format
        spinLogFormat.setSelection(
                getLogFormatIndex(mSettings.getString(Preferences.LOG_FORMAT, DEF_FORMAT)));

        // set Filter log (un)checked
        chkUseFilter.setChecked(mSettings.getBoolean(Preferences.USE_FILTER, false));
        // enable/disable Filter editbox
        if (!chkUseFilter.isChecked()) {
            findViewById(R.id.lblFilter).setVisibility(View.GONE);
            txtFilter.setVisibility(View.GONE);
        }
        chkUseFilter.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                findViewById(R.id.lblFilter).setVisibility((isChecked ? View.VISIBLE : View.GONE));
                txtFilter.setVisibility((isChecked ? View.VISIBLE : View.GONE));
            }
        });

        // set text in filter editbox
        txtFilter.setText("" + mSettings.getString(Preferences.FILTER_TEXT, ""));

        btnActivateService = (Button) findViewById(R.id.activateServiceBtn);
        btnActivateService.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService();
                saveSettings();
                startService();
                Toast.makeText(LogcatUdpCfg.this, "LogcatUdp service (re)started", Toast.LENGTH_SHORT).show();
            }
        });

        btnDeactivateService = (Button) findViewById(R.id.deactivateServiceBtn);
        btnDeactivateService.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (stopService()) {
                    Toast.makeText(LogcatUdpCfg.this, "LogcatUdp service stopped", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnGrantLogs = (Button) findViewById(R.id.grantPermissionBtn);
        btnGrantLogs.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = getApplicationContext();
                RootUtils.setReadLogsPermission(context);
                if (RootUtils.haveReadLogsPermission(context)) {
                    btnGrantLogs.setEnabled(false);
                }
            }
        });
        if (!RootUtils.haveReadLogsPermission(getApplicationContext()) && RootUtils.isDeviceRooted()) {
            btnGrantLogs.setEnabled(true);
        }

        Log.d(TAG, "created");
    }

    @Override
    public void onPause() {
        Log.d(TAG, "Pause cfg dialog");
        super.onPause();
        if (!cancelSave)
            saveSettings();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem item = menu.add(0, MENU_SAVE, 0, getString(R.string.mnuSave));
        item.setIcon(android.R.drawable.ic_menu_save);

        MenuItem mnuClose = menu.add(0, MENU_CANCEL, 0, getString(R.string.mnuClose));
        mnuClose.setIcon(android.R.drawable.ic_menu_close_clear_cancel);

        MenuItem mnuClear = menu.add(0, MENU_CLR_LOG, 0, getString(R.string.mnuClear));
        mnuClear.setIcon(android.R.drawable.ic_menu_delete);

        MenuItem mnuShare = menu.add(0, MENU_SHARE, 0, getString(R.string.mnuShare));
        mnuShare.setIcon(android.R.drawable.ic_menu_share);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_SAVE:
                saveSettings();
                break;
            case MENU_CANCEL:
            /*boolean stopped = */
                stopService();
                cancelSave = true;
                finish();
                break;
            case MENU_CLR_LOG:
                try {
                    Runtime.getRuntime().exec("logcat -c");
                    Log.i(TAG, "Log cleared!");
                } catch (IOException e) {
                    Log.e(TAG, "Clearing log failed!");
                    e.printStackTrace();
                }
                break;
            case MENU_SHARE:
                prgDialog = ProgressDialog.show(this, "", "Loading log. Please wait...", true);
                Thread checkUpdate = new Thread() {
                    public void run() {
                        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                        intent.setType("text/plain");
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                        String androidID = Secure.getString(LogcatUdpCfg.this.getContentResolver(), Secure.ANDROID_ID);
                        intent.putExtra(Intent.EXTRA_SUBJECT, "Logcat from phone: " + androidID);
                        try {
                            Process process = Runtime.getRuntime().exec("logcat -d");
                            DataInputStream reader = new DataInputStream(process.getInputStream());
                            String extraText = "";
                            String ln_str;
                            while ((ln_str = reader.readLine()) != null) {
                                extraText += ln_str + System.getProperty("line.separator");
                            }
                            intent.putExtra(Intent.EXTRA_TEXT, extraText);
                            startActivity(Intent.createChooser(intent, "How do you want to share?"));
                        } catch (IOException e) {
                            Log.e(TAG, "Sharing log failed!");
                            e.printStackTrace();
                        }
                        handler.sendEmptyMessage(0);
                    }
                };
                checkUpdate.start();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            prgDialog.dismiss();
        }
    };

    private void startService() {
        Intent serviceIntent = new Intent(LogcatUdpCfg.this, LogcatUdpService.class);
        Log.d(TAG, "Start service");
        startService(serviceIntent);
    }

    private boolean stopService() {
        Intent serviceIntent = new Intent(LogcatUdpCfg.this, LogcatUdpService.class);
        Log.d(TAG, "Stop service");
        boolean stopres = stopService(serviceIntent);
        if (stopres) Log.d(TAG, "Service Stopped!");
        return stopres;
    }

    private boolean saveSettings() {
        Log.d(TAG, "saving settings");
        SharedPreferences.Editor editor = mSettings.edit();

        boolean sendIds = chkSendIds.isChecked();

        String devId = "";
        if (sendIds)
            devId = txtDevId.getText().toString();
        String destServer = txtServer.getText().toString();
        int destPort = 0;
        boolean error = false;

        try {
            destPort = Integer.parseInt(txtPort.getText().toString());
        } catch (NumberFormatException ex) {
            Toast.makeText(this, "Port is not a valid integer!", Toast.LENGTH_SHORT).show();
            error = true;
        }

        String logFormat = spinLogFormat.getSelectedItem().toString();

        boolean useFilter = chkUseFilter.isChecked();
        String filterText = "";
        if (useFilter)
            filterText = txtFilter.getText().toString();

        boolean autoStart = chkAutoStart.isChecked();

        if (!error) {
            /*boolean startserv = false;
			if (LogcatUdpService.isRunning) {
				stopService();
				startserv = true;
			}*/
            editor.putBoolean(Preferences.SEND_IDS, sendIds);
            if (sendIds)
                editor.putString(Preferences.DEV_ID, devId);
            editor.putString(Preferences.DEST_SERVER, destServer);
            editor.putInt(Preferences.DEST_PORT, destPort);
            editor.putBoolean(Preferences.USE_FILTER, useFilter);
            if (useFilter)
                editor.putString(Preferences.FILTER_TEXT, filterText);
            editor.putBoolean(Preferences.AUTO_START, autoStart);
            editor.putString(Preferences.LOG_FORMAT, logFormat);
            editor.commit();
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
			/*if (startserv) {
				startService();
			}*/
        } else {
            Toast.makeText(this, "Settings not saved!!!", Toast.LENGTH_LONG).show();
        }
        return !error;
    }

    private int getLogFormatIndex( String format ) {
        String[] format_array = getResources().getStringArray(R.array.log_format_array);
        int ret = Arrays.asList(format_array).indexOf(format);
        return ret >= 0 ? ret : 0;
    }
}
