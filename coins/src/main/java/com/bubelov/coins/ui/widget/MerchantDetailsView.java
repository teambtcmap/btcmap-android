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

    private View directions;

    private View call;

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
        name.setText(merchant.getName());

        if (!TextUtils.isEmpty(merchant.getAddress())) {
            address.setText(merchant.getAddress());
        } else {
            new LoadAddressTask().execute(merchant.getPosition());
        }

        description.setText(TextUtils.isEmpty(merchant.getDescription()) ? "Not provided" : merchant.getDescription());

        call.setEnabled(!TextUtils.isEmpty(merchant.getPhone()));
        openWebsite.setEnabled(!TextUtils.isEmpty(merchant.getWebsite()));

        directions.setOnClickListener(v -> Utils.showDirections(getContext(), merchant.getLatitude(), merchant.getLongitude()));
        call.setOnClickListener(v -> Utils.call(getContext(), merchant.getPhone()));
        openWebsite.setOnClickListener(v -> Utils.openUrl(getContext(), merchant.getWebsite()));
        share.setOnClickListener(v -> Utils.share(getContext(), "Check it out!", "This place accept cryptocurrency"));

        openingHours.setText(TextUtils.isEmpty(merchant.getOpeningHours()) ? "Not provided" : merchant.getOpeningHours());

        if (merchant.getCurrencies() != null && !merchant.getCurrencies().isEmpty()) {
            StringBuilder builder = new StringBuilder();

            for (Currency currency : merchant.getCurrencies()) {
                builder.append(currency.getName()).append("\n");
            }

            acceptedCurrencies.setText(builder.toString());
        } else {
            acceptedCurrencies.setText("Not provided");
        }
    }

    public void setMultilineHeader(boolean multiline) {
        name.setSingleLine(!multiline);
        address.setSingleLine(!multiline);
    }

    private void init() {
        inflate(getContext(), R.layout.widget_merchant_details, this);

        header = findViewById(R.id.header);
        name = (TextView) findViewById(R.id.name);
        address = (TextView) findViewById(R.id.address);

        directions = findViewById(R.id.directions);
        call = findViewById(R.id.call);
        openWebsite = findViewById(R.id.open_website);
        share = findViewById(R.id.share);

        description = (TextView) findViewById(R.id.description);
        openingHours = (TextView) findViewById(R.id.opening_hours);
        acceptedCurrencies = (TextView) findViewById(R.id.accepted_currencies);
    }

    private class LoadAddressTask extends AsyncTask<LatLng, Void, String> {
        @Override
        protected void onPreExecute() {
            address.setText("Loading address...");
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

            if (addresses != null || !addresses.isEmpty()) {
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
                MerchantDetailsView.this.address.setText("Couldn't load address");
            } else {
                MerchantDetailsView.this.address.setText(address);
            }
        }
    }
}
