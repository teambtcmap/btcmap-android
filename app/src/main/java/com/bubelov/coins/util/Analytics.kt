/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <https://unlicense.org>
 */

package com.bubelov.coins.util

import android.os.Bundle
import android.text.TextUtils

import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

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

    fun logEvent(name: String, value: String) {
        logEvent(name, Bundle().apply { putString(FirebaseAnalytics.Param.VALUE, value) })
    }

    fun logEvent(name: String, bundle: Bundle? = null) {
        firebaseAnalytics.logEvent(name, bundle)
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