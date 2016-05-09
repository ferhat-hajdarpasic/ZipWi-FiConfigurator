package com.whitespider.wificonfig.zip.zipwi_ficonfigurator;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.whitespider.wificonfig.zip.zipwi_ficonfigurator.command.ConfigureWiFiCommand;
import com.whitespider.wificonfig.zip.zipwi_ficonfigurator.command.MotionCommand;
import com.whitespider.wificonfig.zip.zipwi_ficonfigurator.command.ZipReadProductInfoCommand;

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
    public static final String ACTION = "TCP_COMMUNICATION_ACTION";
    public static final String CONFIGURE_WIFI_ACTION = "CONFIGURE_WIFI_ACTION";
    public static final String CHECK_CONNECTION_ACTION = "CHECK_CONNECTION_ACTION";
    public static final String UNKNOWN_RESPONSE = "Unknown response!";
    public static final String ZIP_TCP_COMMUNICATION_EVENT = "configure-wifi-event";
    public static final String CONFIGURE_WFI_RESULT = "CONFIGURE_WIFI_RESULT";
    public static final String CHECK_CONNECTION_RESULT = "CHECK_CONNECTION_RESULT";
    public static final String EXCEPTION_RESULT = "EXCEPTION_RESULT";

    public static final String CONFIGURE_WIFI_EVENT = "CONFIGURE_WIFI_EVENT";
    public static final String CHECK_CONNECTION_EVENT = "CHECK_CONNECTION_EVENT";
    public static final String UNKNOWN_EVENT = "UNKNOW_EVENT";


    public TcpCommunicationIntentService() {
        super("TCP COMMS");
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        String action = intent.getStringExtra(ACTION);
        if(CONFIGURE_WIFI_ACTION.equalsIgnoreCase(action)) {
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
        if(CHECK_CONNECTION_ACTION.equalsIgnoreCase(action)) {
            ZipReadProductInfoCommand command = new ZipReadProductInfoCommand();
            sendWiFiCommand(command);
        }
    }

    private void sendWiFiCommand(MotionCommand command) {
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
                String message = UNKNOWN_RESPONSE;
                String messageKey = UNKNOWN_RESPONSE;
                switch(buffer[0]) {
                    case (byte)'V':
                        message = "Check connection is OK.";
                        messageKey = CHECK_CONNECTION_RESULT;
                        break;
                    case (byte)'h':
                        message = "Configure WiFi is OK.";
                        messageKey = CONFIGURE_WFI_RESULT;
                        break;
                    default:
                        break;
                }
                Log.i(TAG, message);
                sendResponse(messageKey, message);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error configuring wifi.", e);
            if(command instanceof ConfigureWiFiCommand) {
                sendResponse(CONFIGURE_WFI_RESULT, e.getMessage());
            }
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing socket.", e);
            }
        }
    }

    private void sendResponse(String messageKey, String message) {
        Intent responseIntent;
        if(CONFIGURE_WFI_RESULT.equalsIgnoreCase(messageKey)) {
            responseIntent = new Intent(CONFIGURE_WIFI_EVENT);
        } else if(CHECK_CONNECTION_RESULT.equalsIgnoreCase(messageKey)) {
            responseIntent = new Intent(CHECK_CONNECTION_EVENT);
        } else {
            responseIntent = new Intent(UNKNOWN_EVENT);
        }
        responseIntent.putExtra(messageKey, message);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(responseIntent);
    }
}
