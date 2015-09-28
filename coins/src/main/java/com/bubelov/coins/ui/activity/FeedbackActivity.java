package com.bubelov.coins.ui.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.bubelov.coins.R;
import com.bubelov.coins.util.Utils;

/**
 * Author: Igor Bubelov
 * Date: 23/05/15 19:42
 */

public class FeedbackActivity extends AbstractActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        Toolbar toolbar = findView(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> supportFinishAfterTransition());
    }

    public void onOSMContributorsClicked(View view) {
        Utils.openUrl(this, "https://www.openstreetmap.org/");
    }

    public void onCoinAtmRadarClicked(View view) {
        Utils.openUrl(this, "http://coinatmradar.com/");
    }

    public void onOpenDeveloperProfileClicked(View view) {
        Utils.openUrl(this, "https://linkedin.com/in/bubelov");
    }

    public void onDesignerProfileClicked(View view) {
        Utils.openUrl(this, "https://www.behance.net/yushkov");
    }

    public void onCopyDonationAddress(View view) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Donation address", "1P9dvxyktwUJVVU52hor7HwBaF8nEcGL6");
        clipboard.setPrimaryClip(clip);
    }
}