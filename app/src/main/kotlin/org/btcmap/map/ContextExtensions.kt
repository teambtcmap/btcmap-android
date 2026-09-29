package org.btcmap.map

import android.content.Context
import com.google.android.material.R
import com.google.android.material.color.MaterialColors

fun Context.getOnSurfaceColor(): Int {
    return MaterialColors.getColor(this, R.attr.colorOnSurface, 0)
}

fun Context.getPrimaryContainerColor(): Int {
    return MaterialColors.getColor(this, R.attr.colorPrimaryContainer, 0)
}

fun Context.getTertiaryContainerColor(): Int {
    return MaterialColors.getColor(this, R.attr.colorTertiaryContainer, 0)
}

fun Context.getOnTertiaryContainerColor(): Int {
    return MaterialColors.getColor(this, R.attr.colorOnTertiaryContainer, 0)
}

fun Context.getOnPrimaryContainerColor(): Int {
    return MaterialColors.getColor(this, R.attr.colorOnPrimaryContainer, 0)
}

fun Context.getErrorColor(): Int {
    return MaterialColors.getColor(this, android.R.attr.colorError, 0)
}
