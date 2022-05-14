package search

import android.graphics.Bitmap
import db.Place

data class PlacesSearchRow(
    val place: Place,
    val icon: Bitmap,
    val name: String,
    val distanceToUser: String,
)