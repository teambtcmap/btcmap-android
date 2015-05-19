package com.bubelov.coins.ui.activity;

import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.bubelov.coins.App;
import com.bubelov.coins.receiver.NetworkStateReceiver;

/**
 * Author: Igor Bubelov
 * Date: 26/04/15 10:15
 */

public abstract class AbstractActivity extends AppCompatActivity implements NetworkStateReceiver.NetworkStateListener {
    private App app;

    private NetworkStateReceiver networkStateReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = ((App) getApplication());
        networkStateReceiver = new NetworkStateReceiver();
        networkStateReceiver.addListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        app.getBus().register(this);
        registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    protected void onPause() {
        unregisterReceiver(networkStateReceiver);
        app.getBus().unregister(this);
        super.onPause();
    }

    @Override
    public void networkAvailable() {
        // Nothing to do here
    }

    @Override
    public void networkUnavailable() {
        // Nothing to do here
    }

    public App getApp() {
        return app;
    }

    protected <T extends View> T findView(int id) {
        return (T) findViewById(id);
    }

    protected void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
