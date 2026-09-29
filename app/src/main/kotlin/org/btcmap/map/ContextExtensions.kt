package org.btcmap.map

import android.content.Context
import com.google.android.material.R
import com.google.android.material.color.MaterialColors

fun Context.getOnSurfaceColor(): Int {
    return MaterialColors.getColor(this, R.attr.colorOnSurface, 0)
}

fun Context.getErrorColor(): Int {
    return MaterialColors.getColor(this, android.R.attr.colorError, 0)
}
