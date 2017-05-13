package com.bubelov.coins.repository.place;

import com.bubelov.coins.repository.user.UserRepository;
import com.bubelov.coins.model.Place;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.firebase.crash.FirebaseCrash;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

/**
 * @author Igor Bubelov
 */

@Singleton
public class PlacesRepository {
    private final PlacesDataSourceNetwork networkDataSource;

    private final PlacesDataSourceDb dbDataSource;

    private final PlacesDataSourceMemory memoryDataSource;

    private final UserRepository userRepository;

    @Inject
    public PlacesRepository(PlacesDataSourceNetwork networkDataSource, PlacesDataSourceDb dbDataSource, PlacesDataSourceMemory memoryDataSource, UserRepository userRepository) {
        this.networkDataSource = networkDataSource;
        this.dbDataSource = dbDataSource;
        this.memoryDataSource = memoryDataSource;
        this.userRepository = userRepository;
    }

    public List<Place> getPlaces(LatLngBounds bounds) {
        if (memoryDataSource.getPlaces().isEmpty()) {
            memoryDataSource.setPlaces(dbDataSource.getPlaces());
        }

        return memoryDataSource.getPlaces(bounds);
    }

    public List<Place> getPlaces(String searchQuery) {
        if (memoryDataSource.getPlaces().isEmpty()) {
            memoryDataSource.setPlaces(dbDataSource.getPlaces());
        }

        List<Place> places = new ArrayList<>();

        for (Place place : memoryDataSource.getPlaces()) {
            if (place.name().toLowerCase().contains(searchQuery.toLowerCase())) {
                places.add(place);
            }
        }

        return places;
    }

    public Place getRandomPlace() {
        if (memoryDataSource.getPlaces().isEmpty()) {
            memoryDataSource.setPlaces(dbDataSource.getPlaces());
        }

        if (!memoryDataSource.getPlaces().isEmpty()) {
            return memoryDataSource.getPlaces().get((int)(Math.random() * memoryDataSource.getPlaces().size()));
        }

        return null;
    }

    public Place getPlace(long id) {
        if (memoryDataSource.getPlaces().isEmpty()) {
            memoryDataSource.setPlaces(dbDataSource.getPlaces());
        }

        return memoryDataSource.getPlace(id);
    }

    public boolean add(Place place) {
        Place result = networkDataSource.addPlace(place, userRepository.getUserAuthToken());;

        if (result == null) {
            return false;
        }

        dbDataSource.insertOrUpdatePlace(result);
        memoryDataSource.updatePlace(result);
        return true;
    }

    public boolean update(Place place) {
        Place result = networkDataSource.updatePlace(place, userRepository.getUserAuthToken());

        if (result == null) {
            return false;
        }

        dbDataSource.insertOrUpdatePlace(result);
        memoryDataSource.updatePlace(result);
        return true;
    }

    public List<Place> fetchNewPlaces() {
        if (memoryDataSource.getPlaces().isEmpty()) {
            memoryDataSource.setPlaces(dbDataSource.getPlaces());
        }

        Date latestUpdatedAt = new Date(0);

        for (Place place : memoryDataSource.getPlaces()) {
            if (place.updatedAt().after(latestUpdatedAt)) {
                latestUpdatedAt = place.updatedAt();
            }
        }

        try {
            List<Place> places = networkDataSource.getPlaces(latestUpdatedAt);

            if (!places.isEmpty()) {
                dbDataSource.batchInsert(places);
                memoryDataSource.setPlaces(dbDataSource.getPlaces());
            }

            return places;
        } catch (IOException e) {
            FirebaseCrash.report(e);
            Timber.e(e, "Couldn't fetch new places");
            return Collections.EMPTY_LIST;
        }
    }
}