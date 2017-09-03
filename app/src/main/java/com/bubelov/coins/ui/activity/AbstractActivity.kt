package com.bubelov.coins.ui.activity

import android.arch.lifecycle.LifecycleActivity
import android.os.Bundle

import com.bubelov.coins.util.ThemeUtils

/**
 * @author Igor Bubelov
 */

abstract class AbstractActivity : LifecycleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.ensureRuntimeTheme(this)
        super.onCreate(savedInstanceState)
    }
}