package map

import android.content.res.Configuration
import android.graphics.Color
import org.locationtech.jts.geom.Polygon
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView

fun MapView.showPolygons(polygons: List<Polygon>, paddingPx: Int) {
    post {
        polygons.forEach { poly ->
            val osmPoly = org.osmdroid.views.overlay.Polygon(this)
            osmPoly.fillColor = Color.parseColor("#88f7931a")
            osmPoly.strokeWidth = 3f
            osmPoly.strokeColor = Color.parseColor("#f7931a")
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

fun MapView.enableDarkModeIfNecessary() {
    val nightMode =
        resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

    if (nightMode) {
        setTileSource(
            object : OnlineTileSourceBase(
                "stadia_alidade_smooth_dark",
                0,
                20,
                256,
                "png",
                arrayOf("https://api.btcmap.org/tiles")
            ) {
                override fun getTileURLString(pMapTileIndex: Long): String {
                    val zoom = MapTileIndex.getZoom(pMapTileIndex)
                    val x = MapTileIndex.getX(pMapTileIndex)
                    val y = MapTileIndex.getY(pMapTileIndex)
                    return "$baseUrl?theme=alidade_smooth_dark&zoom=$zoom&x=$x&y=$y"
                }
            }
        )
    }
}