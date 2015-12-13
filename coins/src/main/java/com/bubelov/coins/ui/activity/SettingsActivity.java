package com.bubelov.coins.ui.activity;

import android.os.Bundle;

import com.bubelov.coins.ui.fragment.SettingsFragment;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

/**
 * Author: Igor Bubelov
 * Date: 11/07/14 20:32
 */

public class SettingsActivity extends AbstractActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(android.R.id.content, new SettingsFragment())
                    .commit();
        }

        Answers.getInstance().logContentView(new ContentViewEvent()
                .putContentName("Settings")
                .putContentType("Screens")
                .putContentId("Settings"));
    }
}