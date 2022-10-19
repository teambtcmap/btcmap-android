package map

import android.graphics.drawable.Drawable
import db.View_element_map_pin

data class ElementWithMarker(
    val element: View_element_map_pin,
    val marker: Drawable,
)