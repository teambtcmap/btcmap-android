package com.bubelov.coins.ui.activity;

import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.Toast;

import com.bubelov.coins.EventBus;
import com.bubelov.coins.receiver.NetworkStateReceiver;

/**
 * Author: Igor Bubelov
 * Date: 26/04/15 10:15
 */

public abstract class AbstractActivity extends AppCompatActivity implements NetworkStateReceiver.NetworkStateListener {
    private NetworkStateReceiver networkStateReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        networkStateReceiver = new NetworkStateReceiver();
        networkStateReceiver.addListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getInstance().register(this);
        registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            supportFinishAfterTransition();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        unregisterReceiver(networkStateReceiver);
        EventBus.getInstance().unregister(this);
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

    protected void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}