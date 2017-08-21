package com.bubelov.coins.util

import android.arch.lifecycle.AndroidViewModel
import com.bubelov.coins.dagger.AppComponent
import com.bubelov.coins.dagger.Injector

/**
 * @author Igor Bubelov
 */

fun AndroidViewModel.appComponent(): AppComponent = Injector.appComponent