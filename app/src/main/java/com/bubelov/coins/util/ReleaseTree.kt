package com.bubelov.coins.util

import com.crashlytics.android.Crashlytics
import timber.log.Timber

/**
 * @author Igor Bubelov
 */

class ReleaseTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (t != null) {
            Crashlytics.log("Tag: $tag")
            Crashlytics.log("Message: $message")
            Crashlytics.logException(t)
        }
    }
}