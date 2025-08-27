package settings

import android.content.SharedPreferences
import androidx.core.content.edit
import org.maplibre.android.geometry.LatLngBounds

var SharedPreferences.mapViewport: LatLngBounds
    get() {
        return LatLngBounds.from(
            latNorth = getFloat("latNorth", 12.116667f + 0.04f).toDouble(),
            lonEast = getFloat("lonEast", -68.93333f + 0.04f + 0.03f).toDouble(),
            latSouth = getFloat("latSouth", 12.116667f - 0.04f).toDouble(),
            lonWest = getFloat("lonWest", -68.93333f - 0.04f + 0.03f).toDouble(),
        )
    }
    set(value) {
        edit {
            putFloat("latNorth", value.latitudeNorth.toFloat())
            putFloat("lonEast", value.longitudeEast.toFloat())
            putFloat("latSouth", value.latitudeSouth.toFloat())
            putFloat("lonWest", value.longitudeWest.toFloat())
        }
    }
