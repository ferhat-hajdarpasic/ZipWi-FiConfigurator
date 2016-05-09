package com.whitespider.wificonfig.zip.zipwi_ficonfigurator;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by ferhat on 3/12/2016.
 */
public class WiFiNetworksListViewAdapter extends ArrayAdapter<WiFiContent.WiFiItem> {
    private static final String TAG = "WiFiNet...ViewAdapter";
    public static final String RED = "#FF3336";
    public static final String GREEN = "#04780F";
    private WiFiContent.WiFiItem mSelectedWiFiItem;
    private String connectingState;
    private int secondsSinceLastConnectionOk = 1000;
    private final Handler connectionStatusHandler = new Handler();
    public boolean blink;
    private String color = GREEN;

    public WiFiNetworksListViewAdapter(Context context, int resource, ArrayList<WiFiContent.WiFiItem> wiFiItems) {
        super(context, resource, wiFiItems);
        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                if(secondsSinceLastConnectionOk < 2) {
                    WiFiNetworksListViewAdapter.this.blink = !WiFiNetworksListViewAdapter.this.blink;
                    WiFiNetworksListViewAdapter.this.color = GREEN;
                } else if(secondsSinceLastConnectionOk < 5) {
                    WiFiNetworksListViewAdapter.this.blink = false;
                    WiFiNetworksListViewAdapter.this.color = GREEN;
                } else {
                    WiFiNetworksListViewAdapter.this.blink = false;
                    WiFiNetworksListViewAdapter.this.color = RED;
                }
                connectionStatusHandler.postDelayed(this, 1000);
            }
        };
        connectionStatusHandler.postDelayed(runnable, 1000);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final TextView view = (TextView) super.getView(position, convertView, parent);
        WiFiContent.WiFiItem thisItem = getItem(position);

        WifiManager mWifiManager = (WifiManager) getContext().getSystemService(Context.WIFI_SERVICE);
        final WifiInfo connectionInfo = mWifiManager.getConnectionInfo();
        final String connectionInfoSSID = WiFiConnectCode.removeQuotedString(connectionInfo.getSSID());
        final String ssid = thisItem.ssid;
        if(connectionInfoSSID.equalsIgnoreCase(ssid)) {
            connectingState = connectionInfo.getSupplicantState().name();
            this.mSelectedWiFiItem = thisItem;
        }

        if(this.mSelectedWiFiItem != null) {
            if((connectingState != null) && thisItem.equals(mSelectedWiFiItem)) {
                String connectionState = " - <font color=\"" + this.color + "\">" + connectingState + "</font>";
                if(this.blink) {
                    connectionState = "";
                }
                view.setText(Html.fromHtml(mSelectedWiFiItem.ssid + connectionState));
            }
        }
        Log.d(TAG, "mSelectedWiFiItem=" + view.getText().toString());
        return view;
    }

    @Override
    public void clear() {
        super.clear();
    }

    public void setSelectedWiFiItem(WiFiContent.WiFiItem wiFiItem) {
        this.mSelectedWiFiItem = wiFiItem;
        this.connectingState = null;
        this.secondsSinceLastConnectionOk = 1000;
        this.notifyDataSetChanged();
    }

    public void setConnectingState(String connectingState) {
        this.connectingState = connectingState;
        this.notifyDataSetChanged();
    }

    public void onConnectionCheck(int secondsSinceLastConnectionOk, WiFiContent.WiFiItem wiFiItem) {
        setSelectedWiFiItem(wiFiItem);
        this.secondsSinceLastConnectionOk = Math.abs(secondsSinceLastConnectionOk);
    }
}
