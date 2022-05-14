package map

import android.graphics.Bitmap

data class PlaceMarker(
    val placeId: Long,
    val icon: Bitmap,
    val latitude: Double,
    val longitude: Double
)