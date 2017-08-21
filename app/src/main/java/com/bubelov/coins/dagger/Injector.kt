package com.bubelov.coins.dagger

import android.content.Context

/**
 * @author Igor Bubelov
 */

object Injector {
    lateinit var appComponent: AppComponent
    private set

    fun init(context: Context) {
        appComponent = DaggerAppComponent.builder().context(context).build()
    }
}