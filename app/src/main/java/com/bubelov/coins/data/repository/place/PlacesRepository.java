package com.bubelov.coins.data.repository.place;

import com.bubelov.coins.domain.Place;
import com.google.firebase.crash.FirebaseCrash;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

/**
 * @author Igor Bubelov
 */

@Singleton
public class PlacesRepository {
    private final PlacesDataSourceNetwork networkDataSource;

    private final PlacesDataSourceDisk diskDataSource;

    private final PlacesDataSourceMemory memoryDataSource;

    @Inject
    public PlacesRepository(PlacesDataSourceNetwork networkDataSource, PlacesDataSourceDisk diskDataSource, PlacesDataSourceMemory memoryDataSource) {
        this.networkDataSource = networkDataSource;
        this.diskDataSource = diskDataSource;
        this.memoryDataSource = memoryDataSource;
    }

    public Collection<Place> getAll() {
        Collection<Place> places = memoryDataSource.getAll();

        if (!places.isEmpty()) {
            return places;
        }

        places = diskDataSource.getAll();

        if (!places.isEmpty()) {
            memoryDataSource.setData(places);
        }

        return places;
    }

    public Place get(long id) {
        for (Place place : getAll()) {
            if (place.id() == id) {
                return place;
            }
        }

        return null;
    }

    public boolean add(Place place) {
        networkDataSource.add(place);
        diskDataSource.add(place);
        memoryDataSource.add(place);
        return true;
    }

    public boolean update(Place place) {
        networkDataSource.update(place);
        diskDataSource.update(place);
        memoryDataSource.update(place);
        return true;
    }

    public Collection<Place> fetchNewPlaces() {
        Date latestUpdatedAt = new Date(0);

        for (Place place : getAll()) {
            if (place.updatedAt().after(latestUpdatedAt)) {
                latestUpdatedAt = place.updatedAt();
            }
        }

        try {
            Collection<Place> places = networkDataSource.getPlaces(latestUpdatedAt);
            diskDataSource.batchInsert(places);
            memoryDataSource.setData(places);
            return places;
        } catch (IOException e) {
            FirebaseCrash.report(e);
            Timber.e(e, "Couldn't fetch new places");
            return Collections.EMPTY_LIST;
        }
    }
}