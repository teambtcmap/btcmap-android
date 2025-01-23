package app

import android.content.Context
import android.content.pm.ApplicationInfo

fun Context.isDebuggable(): Boolean {
    return applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
}

fun Context.dpToPx(dp: Int): Int {
    return (dp * resources.displayMetrics.density).toInt()
}