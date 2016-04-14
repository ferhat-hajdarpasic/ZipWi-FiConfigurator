package com.whitespider.wificonfig.zip.zipwi_ficonfigurator;

import android.net.wifi.ScanResult;

import java.util.List;

/**
 * Created by ferhat on 2/18/2016.
 */
public interface IConfigureWiFiActivity {
    void refreshWiFiList(List<ScanResult> mScanResults);
}
