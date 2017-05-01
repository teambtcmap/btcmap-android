package com.bubelov.coins.data.repository.place;

import com.bubelov.coins.data.api.coins.CoinsApi;
import com.bubelov.coins.domain.Place;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Response;

/**
 * @author Igor Bubelov
 */

@Singleton
class PlacesDataSourceNetwork implements PlacesDataSource {
    private final CoinsApi api;

    @Inject
    PlacesDataSourceNetwork(CoinsApi api) {
        this.api = api;
    }

    @Override
    public Place get(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(Place place) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(Place place) {
        throw new UnsupportedOperationException();
    }

    public Collection<Place> getPlaces(Date updatedAfter) throws IOException {
        Response<List<Place>> response = api.getPlaces(updatedAfter, Integer.MAX_VALUE).execute();

        if (response.isSuccessful()) {
            return response.body();
        } else {
            throw new IOException("Couldn't fetch places");
        }
    }
}