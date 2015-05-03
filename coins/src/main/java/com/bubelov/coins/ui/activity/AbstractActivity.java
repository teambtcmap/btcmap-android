package com.bubelov.coins.ui.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.bubelov.coins.App;

/**
 * Author: Igor Bubelov
 * Date: 26/04/15 10:15
 */

public abstract class AbstractActivity extends AppCompatActivity {
    private App app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = ((App) getApplication());
    }

    @Override
    protected void onResume() {
        super.onResume();
        app.getBus().register(this);
    }

    @Override
    protected void onPause() {
        app.getBus().unregister(this);
        super.onPause();
    }

    protected <T extends View> T findView(int id) {
        return (T) findViewById(id);
    }

    protected void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
