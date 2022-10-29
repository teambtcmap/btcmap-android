package map

import android.graphics.drawable.Drawable
import db.View_element_map_cluster

data class ElementWithMarker(
    val element: View_element_map_cluster,
    val marker: Drawable,
)