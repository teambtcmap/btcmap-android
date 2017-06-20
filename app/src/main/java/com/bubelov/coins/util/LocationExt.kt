package com.bubelov.coins.util

import android.location.Location
import com.google.android.gms.maps.model.LatLng

/**
 * @author Igor Bubelov
 */

fun Location.toLatLng(): LatLng = LatLng(latitude, longitude)