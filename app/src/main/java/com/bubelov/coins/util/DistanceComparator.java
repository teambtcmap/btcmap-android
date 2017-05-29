package com.bubelov.coins.util;

import android.location.Location;

import com.bubelov.coins.model.Place;
import com.google.android.gms.maps.model.LatLng;

import java.util.Comparator;

/**
 * @author Igor Bubelov
 */

public class DistanceComparator implements Comparator<Place> {
    private Location target;

    public DistanceComparator(Location target) {
        this.target = target;
    }

    @Override
    public int compare(Place place1, Place place2) {
        Float distance1 = DistanceUtils.getDistance(new LatLng(place1.getLatitude(), place1.getLongitude()), target);
        Float distance2 = DistanceUtils.getDistance(new LatLng(place2.getLatitude(), place2.getLongitude()), target);
        return distance1.compareTo(distance2);
    }
}
