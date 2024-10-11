package map

import android.graphics.Color
import org.locationtech.jts.geom.Polygon
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