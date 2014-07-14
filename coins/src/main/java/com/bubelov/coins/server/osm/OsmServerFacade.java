package com.bubelov.coins.server.osm;

import com.bubelov.coins.server.ServerException;
import com.bubelov.coins.server.ServerFacade;
import com.bubelov.coins.model.Currency;
import com.bubelov.coins.model.Merchant;
import com.google.gson.Gson;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Author: Igor Bubelov
 * Date: 02/06/14 21:45
 */

public class OsmServerFacade implements ServerFacade {
    public static final String QUERY = "http://overpass.osm.rambler.ru/cgi/interpreter?data=[out:json];node[%%22payment:%s%%22=yes];out;";

    public Collection<Merchant> getMerchants(Currency currency) throws ServerException {
        HttpParams httpParameters = new BasicHttpParams();
        int timeoutConnection = 120000;
        HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
        int timeoutSocket = 120000;
        HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);

        HttpClient client = new DefaultHttpClient(httpParameters);
        String requestUrl = String.format(QUERY, getQueryParameter(currency));
        HttpGet post = new HttpGet(requestUrl);

        Collection<Merchant> merchants = new ArrayList<>();

        try {
            HttpResponse response = client.execute(post);
            String responseString = EntityUtils.toString(response.getEntity());

            OsmQueryResult result = new Gson().fromJson(responseString, OsmQueryResult.class);

            if (result == null) {
                return merchants;
            }

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

            return merchants;
        } catch (IOException exception) {
            throw new ServerException(exception);
        }
    }

    private String getQueryParameter(Currency currency) {
        // TODO add mapping from http://wiki.openstreetmap.org/wiki/Key:payment
        return currency.getName().toLowerCase();
    }
}
