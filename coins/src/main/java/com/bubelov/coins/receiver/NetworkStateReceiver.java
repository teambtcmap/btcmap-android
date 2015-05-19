package com.bubelov.coins.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: Igor Bubelov
 * Date: 19/05/15 14:02
 */

public class NetworkStateReceiver extends BroadcastReceiver {
    protected List<NetworkStateListener> listeners;

    protected Boolean connected;

    public NetworkStateReceiver() {
        listeners = new ArrayList<>();
        connected = null;
    }

    public void onReceive(Context context, Intent intent) {
        if(intent == null || intent.getExtras() == null) {
            return;
        }

        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo network = manager.getActiveNetworkInfo();

        if (network != null && network.isAvailable() && network.isConnectedOrConnecting()) {
            connected = true;
        } else {
            connected = false;
        }

        notifyStateToAll();
    }

    private void notifyStateToAll() {
        for(NetworkStateListener listener : listeners) {
            notifyState(listener);
        }
    }

    private void notifyState(NetworkStateListener listener) {
        if(connected == null || listener == null) {
            return;
        }

        if(connected) {
            listener.networkAvailable();
        } else {
            listener.networkUnavailable();
        }
    }

    public void addListener(NetworkStateListener listener) {
        listeners.add(listener);
        notifyState(listener);
    }

    public void removeListener(NetworkStateListener listener) {
        listeners.remove(listener);
    }
    public interface NetworkStateListener {
        void networkAvailable();

        void networkUnavailable();
    }
}
