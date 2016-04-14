package com.whitespider.wificonfig.zip.zipwi_ficonfigurator;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.whitespider.wificonfig.zip.zipwi_ficonfigurator.command.ConfigureWiFiCommand;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class TcpCommunicationIntentService extends IntentService {
    private static final String TAG = TcpCommunicationIntentService.class.getSimpleName();
    public static final String DOMAIN_KEY = "DOMAIN_KEY";
    public static final String PASSWORD_KEY ="PASSWORD_KEY";
    public static final String SECURITY_TYPE_KEY = "SECURITY_TYPE_KEY";
    public static final String PROXY_HOST = "PROXY_HOST";
    public static final String PROXY_PORT = "PROXY_PORT";
    public static final String PROXY_USERNAME = "PROXY_USERNAME";
    public static final String PROXY_PASSWORD = "PROXY_PASSWORD";

    public TcpCommunicationIntentService() {
        super("TCP COMMS");
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        String domain = intent.getStringExtra(DOMAIN_KEY);
        String password = intent.getStringExtra(PASSWORD_KEY);
        int securityType = intent.getIntExtra(SECURITY_TYPE_KEY, -1);
        String proxyHostname = intent.getStringExtra(PROXY_HOST);
        String proxyPort = intent.getStringExtra(PROXY_PORT);
        String proxyUsername = intent.getStringExtra(PROXY_USERNAME);
        String proxyPassword = intent.getStringExtra(PROXY_PASSWORD);

        ConfigureWiFiCommand command = new ConfigureWiFiCommand();
        command.Domain = domain;
        command.Password = password;
        command.SecurityType = securityType;
        command.proxyHostname = proxyHostname;
        command.proxyPort = proxyPort;
        command.proxyUsername = proxyUsername;
        command.proxyPassword = proxyPassword;

        sendWiFiCommand(command);
    }

    private void sendWiFiCommand(ConfigureWiFiCommand command) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String routerHostname = prefs.getString("router_hostname", "192.168.1.1");
        int routerPort = Integer.valueOf(prefs.getString("router_port", "9001"));

        SocketAddress sockaddr = new InetSocketAddress(routerHostname, routerPort);

        Socket socket = new Socket();
        final Context applicationContext = getApplicationContext();
        try {
            socket.connect(sockaddr, 15000); //10 second connection timeout
            if (socket.isConnected()) {
                InputStream nis = socket.getInputStream();
                OutputStream nos = socket.getOutputStream();
                byte[] dataToSend = command.getBytes();
                nos.write(dataToSend); //This is blocking
                byte[] buffer = new byte[4096];
                int read = nis.read(buffer, 0, buffer.length);
                final String message = "Finished configuring wifi";
                Log.i(TAG, message);
                sendResponse(message);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error configuring wifi.", e);
            sendResponse(e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing socket.", e);
            }
        }
    }

    public static final String CONFIGURE_WIFI_EVENT = "configure-wifi-event";
    public static final String CONFIGURE_WFI_RESULT_MESSAGE = "CONFIGURE_WFI_RESULT_MESSAGE";

    private void sendResponse(String message) {
        Intent responseIntent = new Intent(CONFIGURE_WIFI_EVENT);
        responseIntent.putExtra(CONFIGURE_WFI_RESULT_MESSAGE, message);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(responseIntent);
    }
}
