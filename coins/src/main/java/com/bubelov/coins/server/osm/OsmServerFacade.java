package com.bubelov.coins.server.osm;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.bubelov.coins.server.ServerException;
import com.bubelov.coins.server.ServerFacade;
import com.bubelov.coins.model.Currency;
import com.bubelov.coins.model.Merchant;
import com.google.gson.Gson;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Author: Igor Bubelov
 * Date: 02/06/14 21:45
 */

public class OsmServerFacade implements ServerFacade {
    private static final String TAG = OsmServerFacade.class.getName();

    private static final String SERVER_URL = "http://overpass.osm.rambler.ru/";

    private SharedPreferences preferences;

    public OsmServerFacade(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public Collection<Merchant> getMerchants(Currency currency) throws ServerException {
        Collection<Merchant> merchants = new ArrayList<>();
        HttpURLConnection connection = null;

        try {
            URL url = new URL(getRequestUrl(currency));

            connection = (HttpURLConnection) url.openConnection();
            OsmQueryResult result = new Gson().fromJson(new InputStreamReader(connection.getInputStream()), OsmQueryResult.class);

            for (Element element : result.getElements()) {
                Merchant merchant = new Merchant();
                merchant.setId(element.getId());
                merchant.setLatitude(element.getLat());
                merchant.setLongitude(element.getLon());

                if (element.getTags() != null) {
                    merchant.setName(element.getTags().get("name"));
                    merchant.setDescription(element.getTags().get("description"));
                    merchant.setPhone(element.getTags().get("phone"));
                    merchant.setWebsite(element.getTags().get("website"));

                    merchants.add(merchant);
                }
            }

            preferences.edit()
                    .putString(getLastSyncKey(currency), result.getMetadata().getTimestampOsmBase())
                    .commit();
        } catch (Exception exception) {
            throw new ServerException(exception);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return merchants;
    }

    private String getRequestUrl(Currency currency) {
        StringBuilder builder = new StringBuilder();

        builder.append(SERVER_URL);
        builder.append("cgi/interpreter?data=[out:json];node");

        if (preferences.contains(getLastSyncKey(currency))) {
            builder.append(String.format("(newer:%%22%s%%22)", preferences.getString(getLastSyncKey(currency), "")));
        }

        builder.append(String.format("[%%22payment:%s%%22=yes];out;", getQueryParameter(currency)));
        Log.d(TAG, "Request URL: " + builder.toString());
        return builder.toString();
    }

    private String getQueryParameter(Currency currency) {
        // TODO add mapping from http://wiki.openstreetmap.org/wiki/Key:payment
        return currency.getName().toLowerCase();
    }

    private String getLastSyncKey(Currency currency) {
        return currency.getName().toLowerCase() + "_last_sync";
    }
}
