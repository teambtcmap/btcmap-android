package com.bubelov.coins.ui.activity;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

/**
 * @author Igor Bubelov
 */

public class LauncherActivity extends AbstractActivity {
    private static final int REQUEST_RESOLVE_PLAY_SERVICES = 10;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int playServicesAvailabilityResult = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        if (playServicesAvailabilityResult == ConnectionResult.SUCCESS) {
            onPlayServicesAvailable();
        } else {
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(playServicesAvailabilityResult, this, REQUEST_RESOLVE_PLAY_SERVICES);

            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    supportFinishAfterTransition();
                }
            });

            dialog.show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_RESOLVE_PLAY_SERVICES) {
            if (resultCode == RESULT_OK) {
                onPlayServicesAvailable();
            } else {
                supportFinishAfterTransition();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void onPlayServicesAvailable() {
        startActivity(new Intent(this, MapActivity.class));
    }
}