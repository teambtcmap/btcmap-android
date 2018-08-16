package com.bubelov.coins.util

import android.arch.lifecycle.AndroidViewModel
import com.bubelov.coins.di.AppComponent
import com.bubelov.coins.di.Injector

/**
 * @author Igor Bubelov
 */

fun AndroidViewModel.appComponent(): AppComponent = Injector.appComponent