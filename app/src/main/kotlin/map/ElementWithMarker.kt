package map

import android.graphics.drawable.Drawable
import db.Element

data class ElementWithMarker(
    val element: Element,
    val marker: Drawable,
)