package com.bubelov.coins.ui.widget;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.bubelov.coins.R;
import com.bubelov.coins.model.Currency;
import com.bubelov.coins.model.Merchant;
import com.bubelov.coins.util.Utils;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Author: Igor Bubelov
 * Date: 03/05/15 11:03
 */

public class MerchantDetailsView extends FrameLayout {
    @BindView(R.id.header)
    View header;

    @BindView(R.id.name)
    TextView name;

    @BindView(R.id.call)
    MerchantActionButton call;

    @BindView(R.id.open_website)
    View openWebsite;

    @BindView(R.id.share)
    View share;

    @BindView(R.id.description)
    TextView description;

    @BindView(R.id.opening_hours)
    TextView openingHours;

    @BindView(R.id.accepted_currencies)
    TextView acceptedCurrencies;

    public MerchantDetailsView(Context context) {
        super(context);
        init();
    }

    public MerchantDetailsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MerchantDetailsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public int getHeaderHeight() {
        return header.getHeight();
    }

    public void setMerchant(final Merchant merchant) {
        if (TextUtils.isEmpty(merchant.getName())) {
            name.setText(R.string.name_unknown);
        } else {
            name.setText(merchant.getName());
        }

        description.setText(TextUtils.isEmpty(merchant.getDescription()) ? getResources().getString(R.string.not_provided) : merchant.getDescription());

        call.setEnabled(!TextUtils.isEmpty(merchant.getPhone()));
        openWebsite.setEnabled(!TextUtils.isEmpty(merchant.getWebsite()));

        call.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.call(getContext(), merchant.getPhone());
            }
        });

        openWebsite.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.openUrl(getContext(), merchant.getWebsite());
            }
        });

        share.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.share(getContext(), getResources().getString(R.string.share_merchant_message_title), getResources().getString(R.string.share_merchant_message_text, String.format("https://www.google.com/maps/@%s,%s,19z?hl=en", merchant.getLatitude(), merchant.getLongitude())));
            }
        });

        openingHours.setText(TextUtils.isEmpty(merchant.getOpeningHours()) ? getResources().getString(R.string.not_provided) : merchant.getOpeningHours());

        if (merchant.getCurrencies() != null && !merchant.getCurrencies().isEmpty()) {
            StringBuilder builder = new StringBuilder();

            for (Currency currency : merchant.getCurrencies()) {
                builder.append(currency.getName()).append("\n");
            }

            acceptedCurrencies.setText(builder.toString());
        } else {
            acceptedCurrencies.setText(getResources().getString(R.string.not_provided));
        }
    }

    private void init() {
        inflate(getContext(), R.layout.widget_merchant_details, this);
        ButterKnife.bind(this);
    }
}
