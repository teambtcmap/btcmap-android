package map

import android.content.Context
import com.google.android.material.R

fun Context.getOnSurfaceColor(): Int {
    val attrs = theme.obtainStyledAttributes(intArrayOf(R.attr.colorOnSurface))
    return attrs.getColor(0, 0)
}

fun Context.getPrimaryContainerColor(): Int {
    val attrs = theme.obtainStyledAttributes(intArrayOf(R.attr.colorPrimaryContainer))
    return attrs.getColor(0, 0)
}

fun Context.getOnPrimaryContainerColor(): Int {
    val attrs = theme.obtainStyledAttributes(intArrayOf(R.attr.colorOnPrimaryContainer))
    return attrs.getColor(0, 0)
}

fun Context.getErrorColor(): Int {
    val attrs = theme.obtainStyledAttributes(intArrayOf(R.attr.colorError))
    return attrs.getColor(0, 0)
}