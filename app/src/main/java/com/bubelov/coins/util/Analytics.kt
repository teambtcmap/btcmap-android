package com.bubelov.coins.util

import android.os.Bundle
import android.text.TextUtils

import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Igor Bubelov
 */

@Singleton
class Analytics @Inject constructor(private val firebaseAnalytics: FirebaseAnalytics) {
    fun logSelectContent(itemId: String, itemName: String?, contentType: String) {
        logContentEvent(FirebaseAnalytics.Event.SELECT_CONTENT, itemId, itemName, contentType)
    }

    fun logViewContent(itemId: String, itemName: String?, contentType: String) {
        logContentEvent(FirebaseAnalytics.Event.VIEW_ITEM, itemId, itemName, contentType)
    }

    fun logShareContent(itemId: String, itemName: String?, contentType: String) {
        logContentEvent(FirebaseAnalytics.Event.SHARE, itemId, itemName, contentType)
    }

    private fun logContentEvent(eventType: String, itemId: String, itemName: String?, contentType: String) {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, itemId)

        if (TextUtils.isEmpty(itemName)) {
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, itemName)
        }

        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, contentType)
        firebaseAnalytics.logEvent(eventType, bundle)
    }
}