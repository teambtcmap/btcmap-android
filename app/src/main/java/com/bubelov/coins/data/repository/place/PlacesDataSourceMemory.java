package com.bubelov.coins.data.repository.place;

import com.bubelov.coins.domain.Place;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Igor Bubelov
 */

@Singleton
public class PlacesDataSourceMemory {
    private List<Place> places = new ArrayList<>();

    @Inject
    PlacesDataSourceMemory() {}

    public List<Place> getPlaces() {
        return places;
    }

    public List<Place> getPlaces(LatLngBounds bounds) {
        List<Place> result = new ArrayList<>();

        for (Place place : places) {
            if (bounds.contains(place.getPosition())) {
                result.add(place);
            }
        }

        return result;
    }

    public Place getPlace(long id) {
        for (Place place : places) {
            if (place.id() == id) {
                return place;
            }
        }

        return null;
    }

    void updatePlace(Place place) {
        places.remove(place);
        places.add(place);
    }

    void setPlaces(List<Place> places) {
        this.places = places;
    }
}