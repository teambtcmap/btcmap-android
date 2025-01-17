package app

import android.content.Context
import android.content.pm.ApplicationInfo

fun Context.isDebuggable(): Boolean {
    return applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
}