package com.bubelov.coins.util

import com.google.firebase.crash.FirebaseCrash

import timber.log.Timber

/**
 * @author Igor Bubelov
 */

class ReleaseTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (t != null) {
            FirebaseCrash.log("Tag: " + tag)
            FirebaseCrash.log("Message: " + message)
            FirebaseCrash.report(t)
        }
    }
}