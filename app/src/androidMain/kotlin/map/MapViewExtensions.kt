package map

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import org.locationtech.jts.geom.Polygon
import org.maplibre.android.maps.MapLibreMap
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

fun MapView.showPolygons(polygons: List<Polygon>, paddingPx: Int) {
    post {
        polygons.forEach { poly ->
            val osmPoly = org.osmdroid.views.overlay.Polygon(this)
            osmPoly.fillPaint.color = Color.parseColor("#88f7931a")
            osmPoly.outlinePaint.strokeWidth = 3f
            osmPoly.outlinePaint.color = Color.parseColor("#f7931a")
            osmPoly.points = poly.coordinates.map { GeoPoint(it.y, it.x) }
            overlays.add(osmPoly)
            invalidate()
        }

        zoomToBoundingBox(
            boundingBox(polygons),
            false,
            paddingPx,
        )
    }
}

fun MapLibreMap.initStyle(context: Context) {
    val nightMode =
        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

    if (nightMode) {
        setStyle("https://tiles.openfreemap.org/styles/positron")
    } else {
        setStyle("https://tiles.openfreemap.org/styles/bright")
    }
}