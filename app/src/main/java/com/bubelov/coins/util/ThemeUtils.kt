package com.bubelov.coins.util

import android.content.Context
import android.support.annotation.UiThread
import android.util.TypedValue

import com.bubelov.coins.R

/**
 * @author Igor Bubelov
 */

object ThemeUtils {
    private val TYPED_VALUE = TypedValue()

    @UiThread
    fun ensureRuntimeTheme(context: Context) {
        context.theme.resolveAttribute(R.attr.runtimeTheme, TYPED_VALUE, true)

        if (TYPED_VALUE.resourceId != 0) {
            context.setTheme(TYPED_VALUE.resourceId)
        }
    }
}