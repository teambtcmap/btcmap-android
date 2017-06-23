package com.bubelov.coins.dagger

import android.content.Context

/**
 * @author Igor Bubelov
 */

object Injector {
    lateinit var mainComponent: MainComponent
    private set

    fun init(context: Context) {
        mainComponent = DaggerMainComponent.builder().mainModule(MainModule(context)).build()
    }
}