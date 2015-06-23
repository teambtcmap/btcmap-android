package com.bubelov.coins.ui.widget;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.bubelov.coins.R;
import com.bubelov.coins.model.Currency;
import com.bubelov.coins.model.Merchant;
import com.bubelov.coins.util.Utils;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Author: Igor Bubelov
 * Date: 03/05/15 11:03
 */

public class MerchantDetailsView extends FrameLayout {
    private View header;

    private TextView name;

    private TextView address;

    private MerchantActionButton call;

    private View openWebsite;

    private View share;

    private TextView description;

    private TextView openingHours;

    private TextView acceptedCurrencies;

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

    public void setMerchant(Merchant merchant) {
        if (TextUtils.isEmpty(merchant.getName())) {
            name.setText(R.string.name_unknown);
        } else {
            name.setText(merchant.getName());
        }

        if (!TextUtils.isEmpty(merchant.getAddress())) {
            address.setText(merchant.getAddress());
        } else {
            if (Utils.isOnline(getContext())) {
                new LoadAddressTask().execute(merchant.getPosition());
            } else {
                address.setText(R.string.could_not_load_address);
            }
        }

        description.setText(TextUtils.isEmpty(merchant.getDescription()) ? getResources().getString(R.string.not_provided) : merchant.getDescription());

        call.setEnabled(!TextUtils.isEmpty(merchant.getPhone()));
        openWebsite.setEnabled(!TextUtils.isEmpty(merchant.getWebsite()));

        call.setOnClickListener(v -> Utils.call(getContext(), merchant.getPhone()));
        openWebsite.setOnClickListener(v -> Utils.openUrl(getContext(), merchant.getWebsite()));
        share.setOnClickListener(v -> Utils.share(getContext(), getResources().getString(R.string.share_merchant_message_title), getResources().getString(R.string.share_merchant_message_text, String.format("https://www.google.com/maps/@%s,%s,19z?hl=en", merchant.getLatitude(), merchant.getLongitude()))));

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

        header = findViewById(R.id.header);
        name = (TextView) findViewById(R.id.name);
        address = (TextView) findViewById(R.id.address);

        call = (MerchantActionButton) findViewById(R.id.call);
        openWebsite = findViewById(R.id.open_website);
        share = findViewById(R.id.share);

        description = (TextView) findViewById(R.id.description);
        openingHours = (TextView) findViewById(R.id.opening_hours);
        acceptedCurrencies = (TextView) findViewById(R.id.accepted_currencies);
    }

    private class LoadAddressTask extends AsyncTask<LatLng, Void, String> {
        @Override
        protected void onPreExecute() {
            address.setText(R.string.loading_address);
        }

        @Override
        protected String doInBackground(LatLng... params) {
            Geocoder geo = new Geocoder(getContext(), Locale.getDefault());

            List<Address> addresses = null;

            try {
                addresses = geo.getFromLocation(params[0].latitude, params[0].longitude, 1);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                if (addresses.size() > 0) {
                    return String.format("%s, %s, %s",
                            address.getMaxAddressLineIndex() > 0 ? address.getAddressLine(0) : "",
                            address.getLocality(),
                            address.getCountryName());
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(String address) {
            if (TextUtils.isEmpty(address)) {
                MerchantDetailsView.this.address.setText(R.string.could_not_load_address);
            } else {
                MerchantDetailsView.this.address.setText(address);
            }
        }
    }
}
