package com.bubelov.coins.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

/**
 * @author Igor Bubelov
 */

public class LauncherActivity extends AbstractActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(new Intent(this, MapActivity.class));
    }
}