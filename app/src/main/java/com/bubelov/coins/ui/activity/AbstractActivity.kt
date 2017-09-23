package com.bubelov.coins.ui.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity

import com.bubelov.coins.util.ThemeUtils

/**
 * @author Igor Bubelov
 */

abstract class AbstractActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.ensureRuntimeTheme(this)
        super.onCreate(savedInstanceState)
    }
}