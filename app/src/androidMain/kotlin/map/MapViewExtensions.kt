package map

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import org.locationtech.jts.geom.Polygon
import org.maplibre.android.annotations.PolygonOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap

fun MapLibreMap.showPolygons(polygons: List<Polygon>, paddingPx: Int) {
    polygons.forEach { poly ->
        val librePoly = PolygonOptions().addAll(poly.coordinates.map { LatLng(it.y, it.x) })
            .fillColor(Color.parseColor("#88f7931a")).strokeColor(Color.parseColor("#f7931a"))
        addPolygon(librePoly)
    }
    val allPoints = polygons.flatMap { it.coordinates.toList() }.map { LatLng(it.y, it.x) }
    moveCamera(CameraUpdateFactory.newLatLngBounds(LatLngBounds.fromLatLngs(allPoints), paddingPx))
}

fun MapLibreMap.initStyle(context: Context) {
    val nightMode =
        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

    if (nightMode) {
        setStyle("asset://dark.json")
    } else {
        setStyle("asset://light.json")
    }
}