package map

import android.content.Context
import android.content.res.Configuration
import com.google.android.material.R

fun Context.markerBackgroundColor(): Int {
    return if (isNightModeEnabled()) {
        getPrimaryContainerColor()
    } else {
        getPrimaryColor()
    }
}

fun Context.onMarkerBackgroundColor(): Int {
    return if (isNightModeEnabled()) {
        getOnPrimaryContainerColor()
    } else {
        getOnPrimaryColor()
    }
}

fun Context.getPrimaryColor(): Int {
    val attrs = theme.obtainStyledAttributes(intArrayOf(R.attr.colorPrimarySurface))
    return attrs.getColor(0, 0)
}

fun Context.getSurfaceColor(): Int {
    val attrs = theme.obtainStyledAttributes(intArrayOf(R.attr.colorSurface))
    return attrs.getColor(0, 0)
}

fun Context.getOnSurfaceColor(): Int {
    val attrs = theme.obtainStyledAttributes(intArrayOf(R.attr.colorOnSurface))
    return attrs.getColor(0, 0)
}

fun Context.getPrimaryContainerColor(): Int {
    val attrs = theme.obtainStyledAttributes(intArrayOf(R.attr.colorPrimaryContainer))
    return attrs.getColor(0, 0)
}

fun Context.getOnPrimaryColor(): Int {
    val attrs = theme.obtainStyledAttributes(intArrayOf(R.attr.colorOnPrimary))
    return attrs.getColor(0, 0)
}

fun Context.getOnPrimaryContainerColor(): Int {
    val attrs = theme.obtainStyledAttributes(intArrayOf(R.attr.colorOnPrimaryContainer))
    return attrs.getColor(0, 0)
}

fun Context.getOnErrorColor(): Int {
    val attrs = theme.obtainStyledAttributes(intArrayOf(R.attr.colorOnError))
    return attrs.getColor(0, 0)
}

fun Context.getErrorColor(): Int {
    val attrs = theme.obtainStyledAttributes(intArrayOf(android.R.attr.colorError))
    return attrs.getColor(0, 0)
}

fun Context.getOnErrorContainerColor(): Int {
    val attrs = theme.obtainStyledAttributes(intArrayOf(R.attr.colorOnErrorContainer))
    return attrs.getColor(0, 0)
}

fun Context.isNightModeEnabled(): Boolean {
    return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}