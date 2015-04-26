package com.bubelov.coins.ui.activity;

import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

/**
 * Author: Igor Bubelov
 * Date: 26/04/15 10:15
 */

public abstract class AbstractActivity extends AppCompatActivity {
    protected <T extends View> T findView(int id) {
        return (T) findViewById(id);
    }

    protected void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
