package map

import android.graphics.drawable.Drawable
import db.Place

data class PlaceWithMarker(
    val place: Place,
    val marker: Drawable,
)