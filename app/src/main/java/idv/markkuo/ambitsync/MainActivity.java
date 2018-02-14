package idv.markkuo.ambitsync;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantLock;

import idv.markkuo.ambitlog.AmbitRecord;
import idv.markkuo.ambitlog.LogEntry;
import idv.markkuo.ambitlog.LogHeader;

public class MainActivity extends Activity {
    private static String TAG = "AmbitSync";

    //for wakelock
    PowerManager.WakeLock wakeLock;

    //for USB permission and access
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private PendingIntent mPermissionIntent;
    private UsbManager usbManager;
    private HashMap<Integer, Integer> connectedDevices = new HashMap<Integer, Integer>(); /* device ID: file descriptor */

    /* UI widgets handles */
    private TextView mBatteryText;
    private TextView mAmbitStatusText;
    private ProgressBar mBatteryProgress;

    private TextView mLogCountText;
    private TextView mOutputPathText;
    private ListView mEntryListView;
    private TextView mInfoText;

    // for battery updater thread
    private Runnable batteryUpdater;
    private Handler uiUpdaterHandler;
    private int batteryPercentage = 0;

    // the ListView adapter to display "Moves"
    private BaseAdapter entryAdapter;

    /* VID PID for Suunto Ambit watches */
    private static int VID;
    private static int PID[];

    /* C pointer which holds ambit device in native world, do not manipulate it in Java! */
    private long ambit_device = 0;

    /*
     * structure for storing all Ambit moves/logs. Static because at any given point of time
     * only one Ambit device can be connected. Also making it static will prevent the system
     * from deleting it upon orientation/config change, which is very expensive in this app
     */
    private static AmbitRecord record = new AmbitRecord();

    // GPX file output directory
    private File gpxDir = null;

    // for ambit_device synchronization (calling JNI libambit)
    private ReentrantLock lock;

    // for requesting external storage permission
    final static int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;

    // for storing/saving objects to app storage, not used now just reserved for future use
    //private SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        uiUpdaterHandler = new Handler();
        lock = new ReentrantLock();
        usbManager = (UsbManager) getSystemService(USB_SERVICE);
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,TAG);
        //mPrefs = getPreferences(MODE_PRIVATE);

        // get supported ambit VID/PID from resources
        VID = getResources().getInteger(R.integer.vid);
        PID = getResources().getIntArray(R.array.pid);

        // restore from saved state
        if (savedInstanceState != null) {
            Log.d(TAG, "restoring from saved state");
            ambit_device = savedInstanceState.getLong("ambit_device");
            connectedDevices = (HashMap<Integer, Integer>) savedInstanceState.getSerializable("connected_devices");
            batteryPercentage = savedInstanceState.getInt("bat_percent");
            if (connectedDevices.size() > 0 && isAmbitDisconnected()) {
                Log.d(TAG, "ambit device already disconnected");
                ambit_device = 0;
                connectedDevices.clear();
            }
        } else {
            ambit_device = 0;
        }

        setContentView(R.layout.activity_main);

        // UI widget references
        mBatteryProgress = (ProgressBar) findViewById(R.id.batteryProgressBar);
        mBatteryText = (TextView) findViewById(R.id.batteryTextView);
        mAmbitStatusText = (TextView) findViewById(R.id.ambitStatusTextView);
        mLogCountText = (TextView) findViewById(R.id.LogCountTextView);
        mOutputPathText = (TextView) findViewById(R.id.gpxOutputPathText);
        mEntryListView = (ListView)findViewById(R.id.listView);
        mInfoText = (TextView) findViewById(R.id.infoText);

        // the main ListView initialization
        entryAdapter = new MoveListAdapter(getApplicationContext(), record.getEntries());
        mEntryListView.setAdapter(entryAdapter);

        // used for restore UI's state (visibility and text)
        if (savedInstanceState != null && ambit_device != 0) {
            mInfoText.setVisibility(savedInstanceState.getInt("info_text_vis"));
            mEntryListView.setVisibility(savedInstanceState.getInt("listview_vis"));
            mBatteryProgress.setVisibility(savedInstanceState.getInt("bat_progress_vis"));
            mBatteryText.setVisibility(savedInstanceState.getInt("bat_vis"));
            mBatteryText.setText(savedInstanceState.getString("bat_text"));

            mAmbitStatusText.setText(savedInstanceState.getString("status_text"));
            mLogCountText.setVisibility(savedInstanceState.getInt("log_count_vis"));
            mLogCountText.setText(savedInstanceState.getString("log_count_text"));
            mOutputPathText.setVisibility(savedInstanceState.getInt("output_path_vis"));
        }

        // getting USB permission and register Intent for USB device attach/detach events
        registerReceiver(usbManagerBroadcastReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED));
        registerReceiver(usbManagerBroadcastReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));
        registerReceiver(usbManagerBroadcastReceiver, new IntentFilter(ACTION_USB_PERMISSION));
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);

        // Initial check for connected USB devices, set to fire in 1 sec
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkForDevices();
           }
        }, 1000);

        // setup Listview click function
        mEntryListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int position, long l) {
                final LogEntry e = (LogEntry)adapter.getItemAtPosition(position);
                Log.d(TAG, "User click on:" + e.toString());
                //TODO: pop up to ask user to grant download, maybe?

                if (ambit_device == 0) {
                    showToast("Ambit device not connected!", Toast.LENGTH_LONG);
                    return;
                }

                if (e.isDownloaded()) {
                    if (gpxDir != null) {
                        File file = new File(gpxDir, e.getFilename("gpx"));
                        if (file.exists()) {
                            // open the gpx file by other app
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            Uri uri = FileProvider.getUriForFile(MainActivity.this,
                                    BuildConfig.APPLICATION_ID + ".provider", file);
                            intent.setDataAndType(uri,
                                    MimeTypeMap.getSingleton().getMimeTypeFromExtension("gpx"));
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            try {
                                startActivity(intent);
                            } catch (ActivityNotFoundException ex) {
                                showToast("No handler for gpx files", Toast.LENGTH_SHORT);
                            }
                        }
                    }
                    return;
                }

                // now we can start downloading move...
                showToast("Downloading Move:" + e.toString(), Toast.LENGTH_LONG);
                uiUpdaterHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        e.markForSync(true);
                        new LogAsyncTask().execute(e);
                    }
                });
            }
        });

        // check external storage permission and request if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (getApplicationContext().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED) {
                this.requestPermissions(
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            }
        }

        checkGPXOutputLocation();

        //refresh log download status from reading external storage
        for (LogEntry e: record.getEntries()) {
            //load downloaded from File
            if (gpxDir != null) {
                File file = new File(gpxDir, e.getFilename("gpx"));
                if (file.exists())
                    e.setDownloaded(true);
                else
                    e.setDownloaded(false);
            }
        }

        // finally we setup a battery query operation every 10 sec in a background thread
        // Note that it's not started at this point
        batteryUpdater = new Runnable() {
            @Override
            public void run() {
                batteryPercentage = 0;
                while (batteryPercentage < 100 && ambit_device != 0) {
                    if (lock.tryLock()) {
                        try {
                            if (ambit_device != 0)
                                batteryPercentage = getBatteryPercent(ambit_device);
                        } finally {
                            lock.unlock();
                        }
                    }

                    // Update the progress bar
                    uiUpdaterHandler.post(new Runnable() {
                        public void run() {
                            mBatteryProgress.setProgress(batteryPercentage);
                            mBatteryText.setText(getString(R.string.bat) + " " + batteryPercentage + "%");
                        }
                    });

                    // Update every 10 sec
                    try {
                        Thread.sleep(10 * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                Log.v(TAG, "Exit battery update thread");
            }
        };
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showToast("Permission granted to write GPX to external storage!",
                            Toast.LENGTH_LONG);
                    checkGPXOutputLocation();
                } else {
                    showToast("Permission to write external storage denied! Not able to export GPX file",
                            Toast.LENGTH_LONG);
                }
                break;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.v(TAG, "onSaveInstanceState");

        outState.putLong("ambit_device", ambit_device);
        outState.putSerializable("connected_devices", connectedDevices);
        outState.putInt("bat_percent", batteryPercentage);

        // saving item visibility and text content
        outState.putInt("info_text_vis", mInfoText.getVisibility());
        outState.putInt("listview_vis", mEntryListView.getVisibility());
        outState.putInt("bat_progress_vis", mBatteryProgress.getVisibility());
        outState.putInt("bat_vis", mBatteryText.getVisibility());
        outState.putString("bat_text", mBatteryText.getText().toString());

        outState.putString("status_text", mAmbitStatusText.getText().toString());
        outState.putInt("log_count_vis", mLogCountText.getVisibility());
        outState.putString("log_count_text", mLogCountText.getText().toString());
        outState.putInt("output_path_vis", mOutputPathText.getVisibility());
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(TAG, "onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "onResume");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v(TAG, "onStop");
        //TODO: maybe saving downloaded AmbitRecord to filesystem?
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.v(TAG, "onRestart");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy");
        unregisterReceiver(usbManagerBroadcastReceiver);

        // when orientation changes, this Activity will be destroyed and recreated. We don't want to
        // disconnect ambit device if it is going through orientation change, but we _DO_ want
        // to disconnect it if we are closing the app
        if (ambit_device != 0) {
            Log.d(TAG, "app is finishing:" + isFinishing());
            if (isFinishing()) {
                //disconnect ambit if app is ready closing
                lock.lock();
                notifyDeviceDetached(ambit_device);
                ambit_device = 0;
                lock.unlock();
                connectedDevices.clear();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void checkGPXOutputLocation() {
        // initialize and check external storage status and update UI to show state if necessary
        if(isExternalStorageWritable()) {
            File root = android.os.Environment.getExternalStorageDirectory();
            Log.d(TAG, "External file system root: " + root);

            gpxDir = new File (root.getAbsolutePath() + "/" + getString(R.string.folder_name));
            try {
                if (!gpxDir.exists())
                    gpxDir.mkdirs();
                if (!gpxDir.canWrite()) {
                    Log.w(TAG, "Can't write to external storage path:" + gpxDir.getAbsolutePath());
                    mOutputPathText.setText(getString(R.string.ext_no_permission));
                } else {
                    Log.d(TAG, "GPX Saving to" + gpxDir.getAbsolutePath());
                    mOutputPathText.setText(getString(R.string.saving_to) + gpxDir.getAbsolutePath());
                }
            } catch (SecurityException e) {
                Log.w(TAG, "Folder creation failure:" + e);
                mOutputPathText.setText(getString(R.string.ext_error));
            }
        } else {
            mOutputPathText.setText(getString(R.string.ext_not_available));
        }
    }

    private void showToast(final String msg, final int length) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, length).show();
            }
        });
    }

    // the task used for syncing all log(move) headers
    private class LogHeaderAsyncTask extends AsyncTask<Void, Integer, ArrayList<LogEntry>> {
        private int sync_ret;

        @Override
        protected void onPreExecute() {
            wakeLock.acquire();
            mAmbitStatusText.setText(getString(R.string.sync_header_status));
            record.setSyncProgress(0, 0, 0);

            // another progress update thread which calls on publishProgress()
            new Thread() {
                int progress;
                public void run() {
                    try {
                        while (true) {
                            progress = record.getCurrentSyncProgress();
                            if (progress != 0 && progress < 100) {
                                publishProgress(progress);
                            }
                            else if (progress == 100) {
                                publishProgress(progress);
                                break;
                            }
                            sleep(500);
                        }

                    } catch(Exception e) {
                        Log.e(TAG, "progress updater thread:" + e.getMessage());
                    }
                }
            }.start();
        }

        @Override
        protected ArrayList<LogEntry> doInBackground(Void... v) {
            lock.lock();
            //sync log header from device, save to record
            publishProgress(0);
            sync_ret = syncHeader(ambit_device, record);
            lock.unlock();

            if (sync_ret == -1) {
                //sync error
                showToast("Failed to sync Move Headers!", Toast.LENGTH_LONG);
                Log.w(TAG, "Sync move headers return:" + sync_ret);
                return record.getEntries();
            }

            //load downloaded from File
            for (LogEntry e: record.getEntries()) {
                if (gpxDir != null) {
                    File file = new File(gpxDir, e.getFilename("gpx"));
                    if (file.exists())
                        e.setDownloaded(true);
                }
            }

            //return results
            return record.getEntries();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            mBatteryProgress.setProgress(progress[0]);
            //TODO: maybe we can reuse the mBatteryText to output some text progress update?
        }

        @Override
        protected void onPostExecute(ArrayList<LogEntry> data) {
            Log.d(TAG, "log header aync task done!");
            entryAdapter.notifyDataSetChanged();
            mAmbitStatusText.setText(getString(R.string.connect_status));
            mBatteryProgress.setProgress(batteryPercentage);
            try {
                wakeLock.release();
            } catch (RuntimeException e) {
                Log.i(TAG, "wakelock release exception:" + e);
            }
        }
    }

    // the task used for syncing a specific log
    private class LogAsyncTask extends AsyncTask<LogEntry, Integer, LogEntry> {
        private int sync_ret;

        @Override
        protected void onPreExecute() {
            wakeLock.acquire();
            mAmbitStatusText.setText(getString(R.string.sync_move_status));
            Log.d(TAG,"Syncing activity... ");
            record.setSyncProgress(0, 0, 0);

            // another progress update thread which calls on publishProgress()
            new Thread() {
                int progress;
                public void run() {

                    try {
                        while (true) {
                            progress = record.getCurrentSyncProgress();
                            if (progress != 0 && progress < 100) {
                                publishProgress(progress);
                            }
                            else if (progress == 100) {
                                publishProgress(progress);
                                break;
                            }
                            sleep(500);
                        }

                    } catch(Exception e) {
                        Log.e(TAG, "progress updater thread:" + e.getMessage());
                    }
                }
            }.start();
        }

        @Override
        protected LogEntry doInBackground(LogEntry... e) {
            lock.lock();
            //sync specific log from device, save to record
            sync_ret = startSync(ambit_device, record);
            lock.unlock();

            Log.d(TAG, "Syncing activity returns:" + sync_ret);
            return e[0];
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            mBatteryProgress.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(LogEntry log) {
            //restore to original battery percent
            mBatteryProgress.setProgress(batteryPercentage);

            Log.d(TAG, "log download finished (ret:" + sync_ret + "):" + log.toString());
            mAmbitStatusText.setText(getString(R.string.connect_status));

            // clear the flag to sync
            log.markForSync(false);
            if (sync_ret == -1) {
                //sync error
                showToast("Failed to sync Move!", Toast.LENGTH_LONG);
                return;
            }

            // set the downloaded flag
            log.setDownloaded(true);

            // start writing GPX file for this log
            if (gpxDir != null) {
                File file = new File(gpxDir, log.getFilename("gpx"));
                if (file.exists()) {
                    Log.w(TAG, "gpx file already exists:" + file.getAbsolutePath());
                    showToast("gpx file already exists:" + file.getAbsolutePath(), Toast.LENGTH_LONG);
                }
                if (!log.writeGPX(file)) {
                    Log.w(TAG, "Failed to write to GPX file");
                    showToast("Failed to write GPX file", Toast.LENGTH_LONG);
                    log.setDownloaded(false);
                } else {
                    // successful case
                    showToast("Move downloaded successfully!", Toast.LENGTH_LONG);
                    // update Listview UI
                    entryAdapter.notifyDataSetChanged();
                }
            }
            try {
                wakeLock.release();
            } catch (RuntimeException e) {
                Log.i(TAG, "wakelock release exception:" + e);
            }
        }
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    // the Listview adapter class
    class MoveListAdapter extends BaseAdapter {

        private ArrayList<LogEntry> items;
        private Context context;
        private LayoutInflater inflater;


        private class ViewHolder {
            TextView titleTextView;
            TextView subtitleTextView;
            TextView typeTextView;
            TextView detailTextView;

            public ViewHolder(View view) {
                titleTextView = (TextView) view.findViewById(R.id.move_list_title);
                subtitleTextView = (TextView) view.findViewById(R.id.move_list_subtitle);
                typeTextView = (TextView) view.findViewById(R.id.move_list_type);
                detailTextView = (TextView) view.findViewById(R.id.move_list_detail);
            }
        }

        public MoveListAdapter(Context context, ArrayList<LogEntry> items) {
            this.context = context;
            this.items = items;
            inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int i) {
            return items.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup parent) {
            ViewHolder vh;

            if (view == null) {
                view = getLayoutInflater().inflate(R.layout.move_list_item_layout, parent, false);
                vh = new ViewHolder(view);
                view.setTag(vh);
            } else {
                vh = (ViewHolder) view.getTag();
            }

            LogEntry e = (LogEntry) getItem(i);
            LogHeader h = e.getHeader();

            vh.typeTextView.setText(h.getMoveType());
            vh.titleTextView.setText(h.getMoveTime());
            vh.subtitleTextView.setText(h.getMoveDetail());
            vh.detailTextView.setText(h.getMoveDuration());

            if (e.isDownloaded()) {
                view.setBackgroundColor(Color.parseColor("#ccffcc")); //light green
                vh.typeTextView.setText(h.getMoveType() + "\n✔"); //add a check symbol below Move Type
            } else
                view.setBackgroundColor(Color.parseColor("#ffffff")); //set white bg

            return view;
        }
    }

    // for receiving system's USB related intents, where we initialize our Ambit watch through libambit
    private final BroadcastReceiver usbManagerBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                Log.v(TAG, "INTENT ACTION: " + action);

                if (ACTION_USB_PERMISSION.equals(action)) {
                    Log.v(TAG, "ACTION_USB_PERMISSION");

                    synchronized (this) {
                        //we should exit and stop continuing because this may be due to orientation change
                        // and it's not necessary to re-initialize
                        if (ambit_device != 0)
                            return;

                        UsbDeviceConnection connection;
                        UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            if(device != null) {
                                connection = usbManager.openDevice(device);
                                int fd = connection.getFileDescriptor();
                                Log.d(TAG,"Ambit device fd:" + fd + " (" + device.getVendorId() + "/" +
                                        device.getProductId() + ")");
                                lock.lock();
                                ambit_device = notifyDeviceAttached(device.getVendorId(), device.getProductId(),
                                        fd, device.getDeviceName());
                                lock.unlock();

                                if (ambit_device != 0) {
                                    connectedDevices.put(device.getDeviceId(), connection.getFileDescriptor());
                                    showToast("Ambit Device Attached", Toast.LENGTH_SHORT);
                                    setAmbitUIState(true);

                                    // start battery updater
                                    new Thread(batteryUpdater).start();

                                    // set log count text on UI
                                    new Handler().post(new Runnable() {
                                        @Override
                                        public void run() {
                                            final int count;
                                            lock.lock();
                                            count = getEntryCount(ambit_device);
                                            lock.unlock();
                                            showToast("Syncing Header now...", Toast.LENGTH_LONG);
                                            uiUpdaterHandler.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    mLogCountText.setVisibility(View.VISIBLE);
                                                    mLogCountText.setText(Integer.toString(count) + " " + getString(R.string.logcount));
                                                    new LogHeaderAsyncTask().execute();
                                                }
                                            });
                                        }
                                    });

                                } else {
                                    showToast("Error initialize Ambit on this device", Toast.LENGTH_LONG);
                                    uiUpdaterHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            mAmbitStatusText.setText(getString(R.string.connect_error));
                                        }
                                    });
                                }
                            }
                        } else {
                            Log.w(TAG, "permission denied for device " + device);
                        }
                    }
                }

                if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                    Log.v(TAG, "ACTION_USB_DEVICE_ATTACHED");

                    synchronized(this) {
                        UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                        if (isAmbitDevice(device))
                            usbManager.requestPermission(device, mPermissionIntent);
                        else
                            Log.d(TAG, "not an Ambit device, ignore...");
                    }
                }

                if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                    Log.v(TAG, "ACTION_USB_DEVICE_DETACHED");

                    synchronized(this) {
                        UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        Log.v(TAG, "the detached device id:" + device.getDeviceId());
                        if (connectedDevices.containsKey(device.getDeviceId())) {
                            lock.lock();
                            notifyDeviceDetached(ambit_device);
                            ambit_device = 0;
                            lock.unlock();
                            connectedDevices.remove(device.getDeviceId());
                            showToast("Ambit Device Removed", Toast.LENGTH_LONG);
                            //UI updates
                            setAmbitUIState(false);
                        }
                    }
                }
            } catch(Exception e) {
                Log.d(TAG, "Exception: " + e);
            }
        }
    };

    private void setAmbitUIState(final boolean ambit_connected) {
        batteryPercentage = 0;
        uiUpdaterHandler.post(new Runnable() {
            @Override
            public void run() {
                if (ambit_connected) {
                    // when ambit device is attached and granted for permission
                    mAmbitStatusText.setText(getString(R.string.connect_status));
                    mInfoText.setVisibility(View.INVISIBLE);//will not bring it up ever
                    mEntryListView.setVisibility(View.VISIBLE);
                    mBatteryProgress.setVisibility(View.VISIBLE);
                    mBatteryText.setVisibility(View.VISIBLE);
                    mOutputPathText.setVisibility(View.VISIBLE);
                } else {
                    // when ambit device is detached
                    mBatteryProgress.setProgress(batteryPercentage);
                    mBatteryProgress.setVisibility(View.INVISIBLE);
                    mBatteryText.setText("");
                    mBatteryText.setVisibility(View.INVISIBLE);
                    mLogCountText.setText("");
                    mLogCountText.setVisibility(View.INVISIBLE);
                    mOutputPathText.setVisibility(View.INVISIBLE);
                    mAmbitStatusText.setText(getString(R.string.disconnect_status));
                }
            }
        });
    }

    // check if the USB device's VID/PID is among all ambit devices
    private boolean isAmbitDevice(UsbDevice device) {
        if (device == null)
            return false;

        if (device.getVendorId() == VID)
            for (int pid: PID)
                if (device.getProductId() == pid)
                    return true;

        return false;
    }

    // check for newly connected Ambit device on the USB
    private void checkForDevices() {
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

        Log.d(TAG, "check for ambit device...");
        while(deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();
            if (isAmbitDevice(device)) {
                usbManager.requestPermission(device, mPermissionIntent);
                break;
            }
        }
    }

    private boolean isAmbitDisconnected() {
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

        while(deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();
            if (connectedDevices.containsKey(device.getDeviceId()))
                return false;
        }
        return true;
    }

    /* reserved for future use
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    */

    // native APIs
    private static native long notifyDeviceAttached(int vid, int pid, int fd, String path);
    private static native void notifyDeviceDetached(long device);
    private static native int getBatteryPercent(long device);
    private static native int getEntryCount(long device);
    private static native int syncHeader(long device, AmbitRecord record);
    private static native int startSync(long device, AmbitRecord record);
    private static native void stopSync(long device); // not implemented yet
    private static native void nativeInit();


    // loading native libraries
    static {
        System.loadLibrary("iconv");
        System.loadLibrary("usb-android");
        System.loadLibrary("ambit");
        System.loadLibrary("ambitsync");
        nativeInit();
    }
}
