package com.bubelov.coins.ui.model

import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

/**
 * @author Igor Bubelov
 */

data class PlaceMarker internal constructor(val placeId: Long, val icon: BitmapDescriptor, val latitude: Double, val longitude: Double) : ClusterItem {
    override fun getPosition(): LatLng {
        return LatLng(latitude, longitude)
    }

    override fun getTitle(): String {
        return ""
    }

    override fun getSnippet(): String {
        return ""
    }
}