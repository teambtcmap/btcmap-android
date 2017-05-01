package com.bubelov.coins.data.repository.place;

import com.bubelov.coins.domain.Place;

import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Igor Bubelov
 */

@Singleton
class PlacesDataSourceMemory implements PlacesDataSource {
    private Collection<Place> places = new ArrayList<>();

    @Inject
    PlacesDataSourceMemory() {}

    @Override
    public Place get(long id) {
        for (Place place : places) {
            if (place.id() == id) {
                return place;
            }
        }

        return null;
    }

    @Override
    public void add(Place place) {
        places.add(place);
    }

    @Override
    public void update(Place place) {
        places.remove(place);
        places.add(place);
    }

    Collection<Place> getAll() {
        return places;
    }

    void setData(Collection<Place> places) {
        this.places = places;
    }
}