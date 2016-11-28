package com.bubelov.coins.util;

import android.location.Location;

import com.bubelov.coins.model.Place;

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
        Float distance1 = DistanceUtils.getDistance(place1.getPosition(), target);
        Float distance2 = DistanceUtils.getDistance(place2.getPosition(), target);
        return distance1.compareTo(distance2);
    }
}
