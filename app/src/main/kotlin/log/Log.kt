package log

import android.util.Log

fun Throwable.log() = Log.e("btcmap", null, this)