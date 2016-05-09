package com.whitespider.wificonfig.zip.zipwi_ficonfigurator;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.List;


public class ScanFragment extends Fragment implements IConfigureWiFiActivity {

    private WiFiNetworksListView mWiFiNetworksListView;
    private Button mBtnScan = null;
    private WiFiConnectCode connectCode;
    private WiFiBroadcastReceiver mWifiScanReceiver;
    private ProgressBar wifiCollectProgressBar;
    private ConnectionStateIndicator connectionStateIndicator;

    public ScanFragment() {
    }

    public static ScanFragment newInstance(String param1, String param2) {
        ScanFragment fragment = new ScanFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_scan, container, false);
        mWiFiNetworksListView = (WiFiNetworksListView) view.findViewById(R.id.device_list);
        mBtnScan = (Button)view.findViewById(R.id.btn_scan);
        wifiCollectProgressBar = (ProgressBar)view.findViewById(R.id.wifiCollectProgressBar);

        mWiFiNetworksListView.setOnItemClickListener(mDeviceClickListener);
        mWifiScanReceiver = new WiFiBroadcastReceiver(getActivity(), this);

        mBtnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startScan();
            }
        });

        connectCode = new WiFiConnectCode(this, mWiFiNetworksListView);
        startScan();
        connectionStateIndicator = new ConnectionStateIndicator(getActivity(), mWiFiNetworksListView, 1000);
        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public void startScan() {
        mBtnScan.setEnabled(false);
        mWiFiNetworksListView.clear();
        WifiManager wifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
        boolean success = wifiManager.setWifiEnabled(true);
        if(success) {
            success = wifiManager.disconnect();
            if(success) {
                mWifiScanReceiver.startScan();
                wifiCollectProgressBar.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(getActivity(), "Could not disconnect from current WiFi", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getActivity(), "Could not start WiFi", Toast.LENGTH_SHORT).show();
        }
    }

    public void stopScan() {
        mBtnScan.setEnabled(true);
        wifiCollectProgressBar.setVisibility(View.INVISIBLE);
        mWifiScanReceiver.stopScan();
    }

    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
            // Log.d(TAG,"item click");
            //mBtnScan.setEnabled(false);
            WiFiContent.WiFiItem item = (WiFiContent.WiFiItem) parent.getItemAtPosition(pos);
            connect(item, view);
        }
    };

    private void connect(WiFiContent.WiFiItem wiFiItem, View view) {
        if("Open".equalsIgnoreCase(wiFiItem.securityType)) {
            connectCode.connect(wiFiItem, "", view);
        } else {
            showCollectPasswordDialog(wiFiItem, view);
        }
    }

    protected void showCollectPasswordDialog(final WiFiContent.WiFiItem wiFiItem, final View view) {

        // get prompts.xml view
        LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
        View promptView = layoutInflater.inflate(R.layout.password_dialog, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setView(promptView);

        final EditText editText = (EditText) promptView.findViewById(R.id.passwordEdittext);
        // setup a dialog window

        Resources res = getResources();
        final String key = getKey(R.string.zip_saved_password);
        String password = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(key, "");
        editText.setText(password);
        alertDialogBuilder.setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                saveNewWifiPasswordValue(editText);
                connectCode.connect(wiFiItem, editText.getText().toString(), view);
            }
        }).setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        // create an alert dialog
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    private boolean saveNewWifiPasswordValue(TextView savedPassword) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = settings.edit();
        final String key = getKey(R.string.zip_saved_password);
        editor.putString(key, savedPassword.getText().toString());
        return editor.commit();
    }

    @NonNull
    private String getKey(int zip_config_password) {
        Resources res = getResources();
        return res.getString(zip_config_password);
    }

    @Override
    public void refreshWiFiList(List<ScanResult> scanResults) {
        String wiFiNamePrefix = getStringPreference(R.string.wifi_name_prefix, "ZIP-");
        List<WiFiContent.WiFiItem> filteredWiFiNetworks = mWiFiNetworksListView.getFilteredNetworks(scanResults, wiFiNamePrefix);
        mWiFiNetworksListView.refreshFromScanResult(filteredWiFiNetworks);
        connectCode.refreshConnectionDisplay(filteredWiFiNetworks);
        stopScan();
    }

    public String getStringPreference(int preferenceId, String defaultValue) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String preferenceValue = this.getResources().getString(preferenceId);
        return sharedPrefs.getString(preferenceValue, defaultValue);
    }

}
