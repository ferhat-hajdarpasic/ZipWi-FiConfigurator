package com.whitespider.wificonfig.zip.zipwi_ficonfigurator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import static android.net.wifi.SupplicantState.COMPLETED;

/**
 * Created by ferhat on 5/9/2016.
 */
public class ConnectionStateIndicator {
    private static final String TAG = "ConnectionStateInd";
    private final Handler connectionStatusHandler = new Handler();
    private final FragmentActivity activity;
    private final WiFiNetworksListView mWiFiNetworksListView;
    private final int updatePeriodMilliSeconds;
    private long lastTimeConnectionWasOk = -1;
    public ConnectionStateIndicator(FragmentActivity activity, WiFiNetworksListView mWiFiNetworksListView, final int updatePeriodMilliSeconds) {
        this.activity = activity;
        this.mWiFiNetworksListView = mWiFiNetworksListView;
        this.updatePeriodMilliSeconds = updatePeriodMilliSeconds;
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                //sendZipMessage();
                updateConnectionState();
                connectionStatusHandler.postDelayed(this, updatePeriodMilliSeconds);
            }
        };
        connectionStatusHandler.postDelayed(runnable, updatePeriodMilliSeconds);

        //registerZipMessageListener();
    }

    private void registerZipMessageListener() {
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String message = intent.getStringExtra(TcpCommunicationIntentService.CHECK_CONNECTION_RESULT);
                lastTimeConnectionWasOk = System.currentTimeMillis();
                Log.d(TAG, "Received ZIP response hence connection is OK.");
                //Toast.makeText(ConnectionStateIndicator.this.activity, message, Toast.LENGTH_LONG).show();
            }
        };
        LocalBroadcastManager.getInstance(ConnectionStateIndicator.this.activity).registerReceiver(
                broadcastReceiver,
                new IntentFilter(TcpCommunicationIntentService.CHECK_CONNECTION_EVENT));
    }

    private void sendZipMessage() {
        WifiManager mWifiManager = (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);
        final WifiInfo connectionInfo = mWifiManager.getConnectionInfo();
        SupplicantState supplicantState = connectionInfo.getSupplicantState();
        if(COMPLETED == supplicantState) {
            final Intent intent = new Intent(activity, TcpCommunicationIntentService.class);
            intent.putExtra(TcpCommunicationIntentService.ACTION, TcpCommunicationIntentService.CHECK_CONNECTION_ACTION);
            activity.startService(intent);
            Log.d(TAG, "Send ZIP command in order to check the connection.");
        } else {
            Log.d(TAG, "Not connected - not checking the connection.");
        }
    }

    private void updateConnectionState() {
        final String connectionInfoSSID = getConnectionWiFiName();
        WiFiContent.WiFiItem selectedWiFiItem = mWiFiNetworksListView.getSelectedWiFiItem();
        if((connectionInfoSSID != null) && (connectionInfoSSID.trim().length() > 0)) {
            if(selectedWiFiItem != null) {
                if (connectionInfoSSID.equalsIgnoreCase(selectedWiFiItem.ssid)) {
                    lastTimeConnectionWasOk = System.currentTimeMillis();
                }
            }
        }

        int secondsSinceLastTimeConnectionOk = (int)(System.currentTimeMillis() - lastTimeConnectionWasOk)/1000;
        mWiFiNetworksListView.connectionOk(secondsSinceLastTimeConnectionOk);
    }

    private String getConnectionWiFiName() {
        WifiManager mWifiManager = (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);
        final WifiInfo connectionInfo = mWifiManager.getConnectionInfo();
        return WiFiConnectCode.removeQuotedString(connectionInfo.getSSID());
    }
}
