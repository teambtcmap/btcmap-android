package com.bubelov.coins.ui.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.bubelov.coins.R;
import com.bubelov.coins.util.Utils;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Author: Igor Bubelov
 * Date: 23/05/15 19:42
 */

public class FeedbackActivity extends AbstractActivity {
    @Bind(R.id.toolbar) Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                supportFinishAfterTransition();
            }
        });
    }

    @OnClick(R.id.osm_contributors)
    public void onOSMContributorsClicked() {
        Utils.openUrl(this, "https://www.openstreetmap.org/");
    }

    @OnClick(R.id.coin_atm_radar)
    public void onCoinAtmRadarClicked() {
        Utils.openUrl(this, "http://coinatmradar.com/");
    }

    @OnClick(R.id.developer_profile)
    public void onOpenDeveloperProfileClicked() {
        Utils.openUrl(this, "https://linkedin.com/in/bubelov");
    }

    @OnClick(R.id.designer_profile)
    public void onDesignerProfileClicked() {
        Utils.openUrl(this, "https://www.behance.net/yushkov");
    }

    @OnClick(R.id.copy_donation_address)
    public void onCopyDonationAddress() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Donation address", "1P9dvxyktwUJVVU52hor7HwBaF8nEcGL6");
        clipboard.setPrimaryClip(clip);
    }
}