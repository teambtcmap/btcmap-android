package com.bubelov.coins.util;

import android.location.Location;

import com.bubelov.coins.model.Merchant;

import java.util.Comparator;

/**
 * Author: Igor Bubelov
 * Date: 10/16/15 6:39 PM
 */

public class DistanceComparator implements Comparator<Merchant> {
    private Location target;

    public DistanceComparator(Location target) {
        this.target = target;
    }

    @Override
    public int compare(Merchant merchant1, Merchant merchant2) {
        Float distance1 = DistanceUtils.getDistance(merchant1.getPosition(), target);
        Float distance2 = DistanceUtils.getDistance(merchant2.getPosition(), target);
        return distance1.compareTo(distance2);
    }
}
