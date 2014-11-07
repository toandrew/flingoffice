package com.infthink.myflingoffice;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import tv.matchstick.fling.ApplicationMetadata;
import tv.matchstick.fling.ConnectionResult;
import tv.matchstick.fling.Fling;
import tv.matchstick.fling.Fling.ApplicationConnectionResult;
import tv.matchstick.fling.FlingDevice;
import tv.matchstick.fling.FlingManager;
import tv.matchstick.fling.FlingMediaControlIntent;
import tv.matchstick.fling.ResultCallback;
import tv.matchstick.fling.Status;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nanohttpd.webserver.src.main.java.fi.iki.elonen.SimpleWebServer;

public class MainActivity extends ActionBarActivity {
    private static final String TAG = "MyFlingOffice";

    private static final String APP_ID = Fling.FlingApi
            .makeApplicationId("http://toandrew.github.io/demo/office/receiver/index.html");

    private static final int DEFAULT_PORT = 9012;

    private Context mContext;
    private TextView mTextView;
    private ProgressBar mProgressBar;

    private File mDocFile;
    private File mPdfFile;

    private FlingDevice mSelectedDevice;
    private FlingManager mApiClient;
    private Fling.Listener mFlingListener;
    private ConnectionCallbacks mConnectionCallbacks;
    private ConnectionFailedListener mConnectionFailedListener;
    private MediaRouter mMediaRouter;
    private MediaRouteSelector mMediaRouteSelector;
    private MediaRouter.Callback mMediaRouterCallback;
    private FlingOfficeChannel mOfficeChannel;

    private View mCastView;

    private Handler mHandler = new Handler();

    private NotificationManager mNotificationManager;
    private String mIpAddress;

    SimpleWebServer mNanoHTTPD;
    int port = 8080;
    String mRootDir = "/";

    private static final int ACT_CMD_PAGE_PREVIOUS = 1;
    private static final int ACT_CMD_PAGE_NEXT = 2;

    private String mFileType = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mProgressBar = (ProgressBar) findViewById(R.id.processBar);

        initWebserver();

        mContext = this;

        mOfficeChannel = new FlingOfficeChannel();

        mMediaRouter = MediaRouter.getInstance(getApplicationContext());
        mMediaRouteSelector = new MediaRouteSelector.Builder()
                .addControlCategory(
                        FlingMediaControlIntent.categoryForFling(APP_ID))
                .build();

        mMediaRouterCallback = new MediaRouterCallback();
        mFlingListener = new FlingListener();
        mConnectionCallbacks = new ConnectionCallbacks();
        mConnectionFailedListener = new ConnectionFailedListener();

        Intent intent = getIntent();

        if (intent.getAction() == Intent.ACTION_VIEW) {
            try {
                mDocFile = new File(intent.getData().getPath());

                int dot = mDocFile.getName().lastIndexOf(".") + 1;
                mFileType = mDocFile.getName().substring(dot);
            } catch (Exception e) {
                Toast.makeText(mContext, R.string.msg_file_empty,
                        Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            Log.e(TAG, "file path: " + intent.getData().getPath());
            sendRequest();
        } else {
            Toast.makeText(mContext, R.string.msg_file_empty, Toast.LENGTH_LONG)
                    .show();
            finish();
        }
    }

    /**
     * Called on application start. Using the previously selected Cast device,
     * attempts to begin a session using the application name TicTacToe.
     */
    @Override
    protected void onStart() {
        super.onStart();
        mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
                MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
     * Removes the activity from memory when the activity is paused.
     */
    @Override
    protected void onPause() {
        finish();
        super.onPause();
    }

    /**
     * Attempts to end the current game session when the activity stops.
     */
    @Override
    protected void onStop() {
        setSelectedDevice(null);
        mMediaRouter.removeCallback(mMediaRouterCallback);
        super.onStop();
    }

    @Override
    public void onDestroy() {

        stopWebServer();

        if (mPdfFile != null) {
            try {
                mPdfFile.delete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        super.onDestroy();
    }

    /**
     * Returns the screen configuration to portrait mode whenever changed.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    /**
     * Called when the options menu is first created.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
        MediaRouteActionProvider mediaRouteActionProvider = (MediaRouteActionProvider) MenuItemCompat
                .getActionProvider(mediaRouteMenuItem);
        mediaRouteActionProvider.setRouteSelector(mMediaRouteSelector);
        return true;
    }

    /**
     * An extension of the MediaRoute.Callback specifically for the TicTacToe
     * game.
     */
    private class MediaRouterCallback extends MediaRouter.Callback {
        @Override
        public void onRouteSelected(MediaRouter router, RouteInfo route) {
            Log.d(TAG, "onRouteSelected: " + route);
            MainActivity.this.onRouteSelected(route);
        }

        @Override
        public void onRouteUnselected(MediaRouter router, RouteInfo route) {
            Log.d(TAG, "onRouteUnselected: " + route);
            MainActivity.this.onRouteUnselected(route);
        }
    }

    /**
     * Stop receiver application.
     */
    public void stopApplication() {
        if (!mApiClient.isConnected()) {
            return;
        }

        Fling.FlingApi.stopApplication(mApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status result) {
                        if (result.isSuccess()) {
                            //
                        }
                    }
                });
    }

    private void setSelectedDevice(FlingDevice device) {
        Log.d(TAG, "setSelectedDevice: " + device);
        mSelectedDevice = device;

        if (mSelectedDevice != null) {
            try {
                disconnectApiClient();
                connectApiClient();
            } catch (IllegalStateException e) {
                Log.w(TAG, "Exception while connecting API client", e);
                disconnectApiClient();
            }
        } else {
            if (mApiClient != null) {
                if (mApiClient.isConnected()) {
                    mOfficeChannel.leave(mApiClient);
                }

                stopApplication();

                disconnectApiClient();
            }

            mMediaRouter.selectRoute(mMediaRouter.getDefaultRoute());
        }
    }

    private void connectApiClient() {
        Fling.FlingOptions apiOptions = Fling.FlingOptions.builder(
                mSelectedDevice, mFlingListener).build();
        mApiClient = new FlingManager.Builder(this)
                .addApi(Fling.API, apiOptions)
                .addConnectionCallbacks(mConnectionCallbacks)
                .addOnConnectionFailedListener(mConnectionFailedListener)
                .build();
        mApiClient.connect();
    }

    private void disconnectApiClient() {
        if (mApiClient != null) {
            mApiClient.disconnect();
            mApiClient = null;
        }
    }

    /**
     * Called when a user selects a route.
     */
    private void onRouteSelected(RouteInfo route) {
        Log.d(TAG, "onRouteSelected: " + route.getName() + " mProgressBar:"
                + mProgressBar);

        if (mProgressBar != null
                && mProgressBar.getVisibility() == View.VISIBLE) {
            Toast.makeText(mContext, R.string.loading, Toast.LENGTH_SHORT)
                    .show();
            mMediaRouter.selectRoute(mMediaRouter.getDefaultRoute());
            return;
        }

        FlingDevice device = FlingDevice.getFromBundle(route.getExtras());
        setSelectedDevice(device);
    }

    /**
     * Called when a user unselects a route.
     */
    private void onRouteUnselected(RouteInfo route) {
        if (route != null) {
            Log.d(TAG, "onRouteUnselected: " + route.getName());
        }
        setSelectedDevice(null);
    }

    private class FlingListener extends Fling.Listener {
        @Override
        public void onApplicationDisconnected(int statusCode) {
            Log.d(TAG, "Cast.Listener.onApplicationDisconnected: " + statusCode);
            try {
                Fling.FlingApi.removeMessageReceivedCallbacks(mApiClient,
                        mOfficeChannel.getNamespace());
            } catch (IOException e) {
                Log.w(TAG, "Exception while launching application", e);
            }
        }
    }

    private class ConnectionCallbacks implements
            FlingManager.ConnectionCallbacks {
        @Override
        public void onConnectionSuspended(int cause) {
            Log.d(TAG, "ConnectionCallbacks.onConnectionSuspended");
        }

        @Override
        public void onConnected(Bundle connectionHint) {
            Log.d(TAG, "ConnectionCallbacks.onConnected");
            Fling.FlingApi.launchApplication(mApiClient, APP_ID)
                    .setResultCallback(new ConnectionResultCallback());
        }
    }

    private class ConnectionFailedListener implements
            FlingManager.OnConnectionFailedListener {
        @Override
        public void onConnectionFailed(ConnectionResult result) {
            Log.d(TAG, "ConnectionFailedListener.onConnectionFailed");
            setSelectedDevice(null);
        }
    }

    private final class ConnectionResultCallback implements
            ResultCallback<ApplicationConnectionResult> {
        @Override
        public void onResult(ApplicationConnectionResult result) {
            Status status = result.getStatus();
            ApplicationMetadata appMetaData = result.getApplicationMetadata();

            if (status.isSuccess()) {
                Log.d(TAG, "ConnectionResultCallback: " + appMetaData.getName());
                try {
                    Fling.FlingApi.setMessageReceivedCallbacks(mApiClient,
                            mOfficeChannel.getNamespace(), mOfficeChannel);

                    String path = "http://" + mIpAddress + ":" + DEFAULT_PORT
                            + mPdfFile.getAbsolutePath();
                    Log.e(TAG, "ready to join:" + path);
                    mOfficeChannel.show(mApiClient, path);
                } catch (IOException e) {
                    Log.w(TAG, "Exception while launching application", e);
                }
            } else {
                Log.d(TAG,
                        "ConnectionResultCallback. Unable to launch the game. statusCode: "
                                + status.getStatusCode());
            }
        }
    }

    /**
     * An extension of the GameChannel specifically for the Office game.
     */
    private class FlingOfficeChannel extends OfficeChannel {
    }

    private void sendMessage(final int keyCode) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                int cmd = -1;
                switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    Log.e(TAG, "KEYCODE_DPAD_UP?!!!");
                    cmd = ACT_CMD_PAGE_PREVIOUS;
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    cmd = ACT_CMD_PAGE_NEXT;
                    break;
                }

                if (mOfficeChannel != null) {
                    mOfficeChannel.act(mApiClient, cmd);
                }
            }
        });
    }

    public void addButton(GridLayout gridLayout, String text,
            final int keyCode, int cellPixels, int row, int col) {
        Button button = new Button(this);
        button.setBackgroundResource(R.drawable.button);
        button.setText(text);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (keyCode) {
                case KeyEvent.KEYCODE_ENTER:
                    break;
                case KeyEvent.KEYCODE_MENU:
                    if (mDocFile != null) {
                        String fileInfo = "Name: " + mDocFile.getName() + "\n"
                                + " Size: " + mDocFile.length() + " bytes";
                        Toast.makeText(mContext, fileInfo, Toast.LENGTH_SHORT)
                                .show();
                    }
                    break;
                case KeyEvent.KEYCODE_BACK:
                    onRouteUnselected(null);
                    finish();
                    return;
                default:
                    if (mApiClient != null && mApiClient.isConnected()) {
                        sendMessage(keyCode);
                    }
                }
            }
        });

        GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams(
                GridLayout.spec(row, 1), GridLayout.spec(col, 1));
        layoutParams.width = cellPixels;
        layoutParams.height = cellPixels;

        gridLayout.addView(button, layoutParams);
    }

    public View buildUI() {
        mCastView = getLayoutInflater().inflate(R.layout.activity_main, null);

        RelativeLayout relative = new RelativeLayout(this);
        relative.setBackground(getResources().getDrawable(
                R.drawable.window_background));

        RelativeLayout.LayoutParams rl = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        rl.addRule(RelativeLayout.CENTER_HORIZONTAL);
        relative.addView(mCastView, rl);

        GridLayout gridLayout = new GridLayout(this);
        gridLayout.setRowCount(4);
        gridLayout.setColumnCount(3);
        gridLayout.setOrientation(GridLayout.HORIZONTAL);
        gridLayout.setAlignmentMode(GridLayout.ALIGN_MARGINS);

        gridLayout.setBackgroundResource(R.drawable.window_background);

        int widthPixels = getResources().getDisplayMetrics().widthPixels;
        int cellPixels = (int) ((widthPixels * 1f) / 3);

        addButton(gridLayout,
                mContext.getResources()
                        .getString(R.string.button_previous_page),
                KeyEvent.KEYCODE_DPAD_LEFT, cellPixels, 1, 0);
        addButton(gridLayout,
                mContext.getResources().getString(R.string.button_up),
                KeyEvent.KEYCODE_DPAD_UP, cellPixels, 0, 1);
        addButton(gridLayout,
                mContext.getResources().getString(R.string.button_next_page),
                KeyEvent.KEYCODE_DPAD_RIGHT, cellPixels, 1, 2);
        addButton(gridLayout,
                mContext.getResources().getString(R.string.button_down),
                KeyEvent.KEYCODE_DPAD_DOWN, cellPixels, 2, 1);

        addButton(gridLayout,
                mContext.getResources().getString(R.string.button_ok),
                KeyEvent.KEYCODE_ENTER, cellPixels, 1, 1);
        addButton(gridLayout,
                mContext.getResources().getString(R.string.button_file_info),
                KeyEvent.KEYCODE_MENU, cellPixels, 3, 0);
        addButton(gridLayout,
                mContext.getResources().getString(R.string.button_back),
                KeyEvent.KEYCODE_BACK, cellPixels, 3, 2);

        rl = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        rl.addRule(RelativeLayout.CENTER_IN_PARENT);

        relative.addView(gridLayout, rl);

        return relative;
    }

    private void sendRequest() {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        try {
            JSONObject json = new JSONObject();
            json.put(Utils.REQUEST_JSON_KEY_CMD, "view");
            json.put("type", mFileType);
            params.add(new BasicNameValuePair(Utils.REQUEST_JSON_NAME, json
                    .toString()));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        new HttpCommunicateTask(this, Utils.KEY_HTTP_BASE_URL, params,
                new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        int result = -1;
                        switch (msg.what) {
                        case Utils.HANDLE_COMMUNICATION_RESPONSE_BODY_ENTITY:
                            try {
                                JSONObject obj = new JSONObject(
                                        (String) msg.obj);
                                result = obj.getInt("result");
                                if (result == 0) {
                                    sendFile();
                                }
                            } catch (JSONException e) {
                                Toast.makeText(mContext,
                                        R.string.msg_http_connection_fail_str,
                                        Toast.LENGTH_SHORT).show();
                                e.printStackTrace();
                                onRouteUnselected(null);
                                finish();
                            }
                            break;
                        case Utils.HANDLE_CONNECTION_FAIL:
                            Toast.makeText(mContext,
                                    R.string.msg_http_connection_fail_str,
                                    Toast.LENGTH_SHORT).show();
                            onRouteUnselected(null);
                            finish();
                            break;
                        case Utils.HANDLE_COMMUNICATION_RESPONSE_ERROR:
                            onRouteUnselected(null);
                            finish();
                            break;
                        default:
                            Toast.makeText(mContext,
                                    R.string.msg_http_connection_unknown_str,
                                    Toast.LENGTH_SHORT).show();
                            onRouteUnselected(null);
                            finish();
                            break;
                        }
                    }
                }).execute();

    }

    protected void sendFile() {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        final String md5Str = Utils.BigFile2MD5(mDocFile);
        try {
            JSONObject json = new JSONObject();
            json.put("filename", mDocFile.getName());
            json.put("type", mFileType);
            json.put("filenamemd5", Utils.String2MD5(mDocFile.getName()));
            json.put("md5", md5Str);
            json.put("file", Utils.File2Base64(mDocFile));
            params.add(new BasicNameValuePair(Utils.REQUEST_JSON_NAME, json
                    .toString()));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        new HttpCommunicateTask(this, Utils.KEY_HTTP_BASE_URL, params,
                new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        int result = -1;
                        switch (msg.what) {
                        case Utils.HANDLE_COMMUNICATION_RESPONSE_BODY_ENTITY:
                            try {
                                JSONObject obj = new JSONObject(
                                        (String) msg.obj);
                                JSONArray contentFiles = obj
                                        .getJSONArray("content");
                                result = obj.getInt("result");
                                if (result == 0) {

                                    setContentView(buildUI());

                                    mTextView = (TextView) mCastView
                                            .findViewById(R.id.hint);
                                    mTextView.setVisibility(View.GONE);

                                    mProgressBar = (ProgressBar) mCastView
                                            .findViewById(R.id.processBar);

                                    mProgressBar.setVisibility(View.GONE);

                                    int n = contentFiles.length();
                                    if (n == 0) {
                                        Toast.makeText(
                                                mContext,
                                                R.string.msg_http_connection_fail_str,
                                                Toast.LENGTH_SHORT).show();
                                    }

                                    for (int i = 0; i < n; i++) {
                                        JSONObject fileObj = contentFiles
                                                .getJSONObject(i);
                                        File jpgDir = new File(
                                                getExternalCacheDir(), md5Str);
                                        if (!jpgDir.exists()) {
                                            jpgDir.mkdir();
                                        }

                                        mPdfFile = Utils.Base64File(
                                                jpgDir.getPath()
                                                        + "/"
                                                        + fileObj
                                                                .getString("name"),
                                                fileObj.getString("file"));
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                Toast.makeText(mContext,
                                        R.string.msg_http_connection_fail_str,
                                        Toast.LENGTH_SHORT).show();
                                onRouteUnselected(null);
                                finish();
                            }
                            break;
                        case Utils.HANDLE_CONNECTION_FAIL:
                            Toast.makeText(mContext,
                                    R.string.msg_http_connection_fail_str,
                                    Toast.LENGTH_SHORT).show();
                            onRouteUnselected(null);
                            finish();
                            break;
                        case Utils.HANDLE_COMMUNICATION_RESPONSE_ERROR:
                            onRouteUnselected(null);
                            finish();
                            break;
                        default:
                            Toast.makeText(mContext,
                                    R.string.msg_http_connection_unknown_str,
                                    Toast.LENGTH_SHORT).show();
                            onRouteUnselected(null);
                            finish();
                            break;
                        }
                    }
                }).execute();

    }

    private void initWebserver() {
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        startWetServer(DEFAULT_PORT);
    }

    private void startWetServer(int port) {
        try {
            WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();

            mIpAddress = intToIp(wifiInfo.getIpAddress());

            if (wifiInfo.getSupplicantState() != SupplicantState.COMPLETED) {
                new AlertDialog.Builder(this)
                        .setTitle("Error")
                        .setMessage(
                                "Please connect to a WIFI-network for starting the webserver.")
                        .setPositiveButton("OK", null).show();
                throw new Exception("Please connect to a WIFI-network.");
            }

            Log.e(TAG, "Starting server " + mIpAddress + ":" + port + ".");

            List<File> rootDirs = new ArrayList<File>();
            boolean quiet = false;
            Map<String, String> options = new HashMap<String, String>();
            rootDirs.add(new File(mRootDir).getAbsoluteFile());

            // mNanoHTTPD
            try {
                mNanoHTTPD = new SimpleWebServer(mIpAddress, port, rootDirs,
                        quiet);
                mNanoHTTPD.start();
            } catch (IOException ioe) {
                Log.e(TAG, "Couldn't start server:\n" + ioe);
            }

            Intent i = new Intent(this, MainActivity.class);
            i.setAction(Intent.ACTION_VIEW);
            Uri uri = Uri.fromFile(mDocFile);
            i.setData(uri);

            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i,
                    0);

            Notification notif = new Notification(R.drawable.ic_launcher,
                    "Webserver is running:" + mIpAddress + ":" + port,
                    System.currentTimeMillis());
            notif.setLatestEventInfo(this, "Webserver", "Webserver is running:"
                    + mIpAddress + ":" + port, contentIntent);
            notif.flags = Notification.FLAG_NO_CLEAR;
            mNotificationManager.notify(1234, notif);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void stopWebServer() {
        if (mNanoHTTPD != null) {
            mNanoHTTPD.stop();
            Log.e(TAG, "Server was killed.");
            mNotificationManager.cancelAll();
        } else {
            Log.e(TAG, "Cannot kill server!? Please restart your phone.");
        }
    }

    public static String intToIp(int i) {
        return ((i) & 0xFF) + "." + ((i >> 8) & 0xFF) + "."
                + ((i >> 16) & 0xFF) + "." + (i >> 24 & 0xFF);
    }
}
