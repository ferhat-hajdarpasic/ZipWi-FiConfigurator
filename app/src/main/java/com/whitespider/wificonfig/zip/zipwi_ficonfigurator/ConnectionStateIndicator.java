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
    private long lastZipMessageTimestamp = -1;
    public ConnectionStateIndicator(FragmentActivity activity, WiFiNetworksListView mWiFiNetworksListView, final int updatePeriodMilliSeconds) {
        this.activity = activity;
        this.mWiFiNetworksListView = mWiFiNetworksListView;
        this.updatePeriodMilliSeconds = updatePeriodMilliSeconds;
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                sendZipMessage();
                updateConnectionState();
                connectionStatusHandler.postDelayed(this, updatePeriodMilliSeconds);
            }
        };
        connectionStatusHandler.postDelayed(runnable, updatePeriodMilliSeconds);

        {
            BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String message = intent.getStringExtra(TcpCommunicationIntentService.CHECK_CONNECTION_RESULT);
                    lastZipMessageTimestamp = System.currentTimeMillis();
                    Log.d(TAG, "Received ZIP response hence connection is OK.");
                    //Toast.makeText(ConnectionStateIndicator.this.activity, message, Toast.LENGTH_LONG).show();
                }
            };
            LocalBroadcastManager.getInstance(ConnectionStateIndicator.this.activity).registerReceiver(
                    broadcastReceiver,
                    new IntentFilter(TcpCommunicationIntentService.CHECK_CONNECTION_EVENT));
        }

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
        int secondsSinceLastZipMessage = (int)(System.currentTimeMillis() - lastZipMessageTimestamp)/1000;
        WifiManager mWifiManager = (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);
        final WifiInfo connectionInfo = mWifiManager.getConnectionInfo();
        final String connectionInfoSSID = WiFiConnectCode.removeQuotedString(connectionInfo.getSSID());

        mWiFiNetworksListView.connectionOk(secondsSinceLastZipMessage, connectionInfoSSID);
    }
}
