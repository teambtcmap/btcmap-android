package com.bubelov.coins.ui.activity;

import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.bubelov.coins.EventBus;
import com.bubelov.coins.receiver.NetworkStateReceiver;
import com.bubelov.coins.ui.ProgressDialog;
import com.bubelov.coins.util.ThemeUtils;

/**
 * Author: Igor Bubelov
 * Date: 26/04/15 10:15
 */

public abstract class AbstractActivity extends AppCompatActivity implements NetworkStateReceiver.NetworkStateListener {
    private NetworkStateReceiver networkStateReceiver;

    public ProgressDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.ensureRuntimeTheme(this);
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

    protected void showProgress() {
        hideProgress();
        loadingDialog = ProgressDialog.show(this, "", "", true, false);
    }

    protected void hideProgress() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (loadingDialog != null) {
                    loadingDialog.hide();
                }
            }
        });
    }
}