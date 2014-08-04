package ru.babay.lib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;

/**
 * Created with IntelliJ IDEA.
 * User: Babay
 * Date: 9/11/13
 * Time: 9:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class WifiStatusManager {
    static boolean wifiConnected;
    static BroadcastReceiver receiver;

    public static void startListening(Context context) {
        if (receiver == null) {
            receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    final String action = intent.getAction();
                    if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
                        if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)) {
                            wifiConnected = true;
                        } else {
                            // wifi connection was lost
                            wifiConnected = false;
                        }
                    }
                }
            };
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        context.registerReceiver(receiver, intentFilter);
    }

    public static void pauseListening(Context context) {
        if (receiver != null)
            try {
            context.unregisterReceiver(receiver);
            } catch (Exception e){}
    }

    public boolean isWifiConnected(){
        return wifiConnected;
    }
}
