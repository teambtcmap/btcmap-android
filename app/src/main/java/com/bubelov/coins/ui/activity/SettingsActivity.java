package com.bubelov.coins.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.widget.Toolbar;

import com.bubelov.coins.R;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author Igor Bubelov
 */

public class SettingsActivity extends AbstractActivity {
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    public static void start(Activity activity) {
        Intent intent = new Intent(activity, SettingsActivity.class);
        activity.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity).toBundle());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);
        toolbar.setNavigationOnClickListener(v -> supportFinishAfterTransition());
    }
}