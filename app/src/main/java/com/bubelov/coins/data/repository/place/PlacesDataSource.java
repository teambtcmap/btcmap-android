package com.bubelov.coins.data.repository.place;

import com.bubelov.coins.domain.Place;

/**
 * @author Igor Bubelov
 */

interface PlacesDataSource {
    Place get(long id);

    void add(Place place);

    void update(Place place);
}