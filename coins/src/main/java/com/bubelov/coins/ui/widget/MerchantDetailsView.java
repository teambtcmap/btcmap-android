package com.bubelov.coins.ui.widget;

import android.content.Context;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.bubelov.coins.App;
import com.bubelov.coins.R;
import com.bubelov.coins.database.Tables;
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

    private TextView description;

    private View directions;

    private View call;

    private View openWebsite;

    private View share;

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
            Cursor cursor = App.getInstance().getDatabaseHelper().getReadableDatabase().query(Tables.Merchants.TABLE_NAME,
                    new String[] { Tables.Merchants.NAME, Tables.Merchants.PHONE, Tables.Merchants.WEBSITE },
                    "_id = ?",
                    new String[] { String.valueOf(merchant.getId()) },
                    null,
                    null,
                    null);

            if (cursor.moveToNext()) {
                merchant.setName(cursor.getString(cursor.getColumnIndex(Tables.Merchants.NAME)));
                merchant.setPhone(cursor.getString(cursor.getColumnIndex(Tables.Merchants.PHONE)));
                merchant.setWebsite(cursor.getString(cursor.getColumnIndex(Tables.Merchants.WEBSITE)));
            }
        }

        name.setText(merchant.getName());
        description.setText(merchant.getDescription());

        call.setEnabled(!TextUtils.isEmpty(merchant.getPhone()));
        openWebsite.setEnabled(!TextUtils.isEmpty(merchant.getWebsite()));

        directions.setOnClickListener(v -> Utils.showDirections(getContext(), merchant.getLatitude(), merchant.getLongitude()));
        call.setOnClickListener(v -> Utils.call(getContext(), merchant.getPhone()));
        openWebsite.setOnClickListener(v -> Utils.openUrl(getContext(), merchant.getWebsite()));
        share.setOnClickListener(v -> Utils.share(getContext(), "Check it out!", "This place accept cryptocurrency"));

        new LoadAddressTask().execute(merchant.getPosition());
    }

    private void init() {
        inflate(getContext(), R.layout.widget_merchant_details, this);

        header = findViewById(R.id.merchant_header);
        name = (TextView) findViewById(R.id.merchant_name);
        address = (TextView) findViewById(R.id.merchant_address);
        description = (TextView) findViewById(R.id.merchant_description);

        directions = findViewById(R.id.directions);
        call = findViewById(R.id.call_merchant);
        openWebsite = findViewById(R.id.open_merchant_website);
        share = findViewById(R.id.share_merchant);
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
