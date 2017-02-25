package com.bubelov.coins.ui.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.bubelov.coins.ui.dialog.ProgressDialog;
import com.bubelov.coins.util.ThemeUtils;

/**
 * @author Igor Bubelov
 */

public abstract class AbstractActivity extends AppCompatActivity {
    public ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.ensureRuntimeTheme(this);
        super.onCreate(savedInstanceState);
    }

    protected void showProgress() {
        hideProgress();
        progressDialog = ProgressDialog.show(this, "", "", true, false);
    }

    protected void hideProgress() {
        if (progressDialog != null) {
            progressDialog.hide();
        }
    }
}