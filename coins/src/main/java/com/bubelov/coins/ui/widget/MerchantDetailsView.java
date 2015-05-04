package com.bubelov.coins.ui.widget;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bubelov.coins.R;
import com.bubelov.coins.model.Merchant;
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

    private TextView description;

    private ImageView call;

    private ImageView openWebsite;

    private ImageView share;

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
        description.setText(merchant.getDescription());

        if (TextUtils.isEmpty(merchant.getPhone())) {
            call.setColorFilter(getResources().getColor(R.color.icons));
        } else {
            call.setColorFilter(getResources().getColor(R.color.primary));
        }

        if (TextUtils.isEmpty(merchant.getWebsite())) {
            openWebsite.setColorFilter(getResources().getColor(R.color.icons));
        } else {
            openWebsite.setColorFilter(getResources().getColor(R.color.primary));
        }

        new LoadAddressTask().execute(merchant.getPosition());
    }

    private void init() {
        inflate(getContext(), R.layout.widget_merchant_details, this);

        header = findViewById(R.id.merchant_header);
        name = (TextView) findViewById(R.id.merchant_name);
        address = (TextView) findViewById(R.id.merchant_address);
        description = (TextView) findViewById(R.id.merchant_description);

        call = (ImageView) findViewById(R.id.call_merchant);
        openWebsite = (ImageView) findViewById(R.id.open_merchant_website);
        share = (ImageView) findViewById(R.id.share_merchant);
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

            if (addresses == null || addresses.isEmpty()) {
                name.setText("Waiting for Location");
            } else {
                Address address = addresses.get(0);

                if (addresses.size() > 0) {
                    String addressText = String.format("%s, %s, %s",
                            address.getMaxAddressLineIndex() > 0 ? address.getAddressLine(0) : "",
                            address.getLocality(),
                            address.getCountryName());

                    return addressText;
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
