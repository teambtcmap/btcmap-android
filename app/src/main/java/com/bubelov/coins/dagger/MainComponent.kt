package com.bubelov.coins.dagger

import android.content.Context

import com.bubelov.coins.repository.area.NotificationAreaRepository
import com.bubelov.coins.repository.currency.CurrenciesRepository
import com.bubelov.coins.repository.notification.PlaceNotificationsRepository
import com.bubelov.coins.repository.place.PlacesRepository
import com.bubelov.coins.repository.placecategory.PlaceCategoriesDataSourceApi
import com.bubelov.coins.repository.placecategory.PlaceCategoriesDataSourceDb
import com.bubelov.coins.repository.placecategory.PlaceCategoriesDataSourceMemory
import com.bubelov.coins.repository.placecategory.PlaceCategoriesRepository
import com.bubelov.coins.repository.rate.ExchangeRatesRepository
import com.bubelov.coins.ui.activity.EditPlaceActivity
import com.bubelov.coins.ui.activity.NotificationAreaActivity
import com.bubelov.coins.ui.activity.ProfileActivity
import com.bubelov.coins.ui.fragment.SettingsFragment
import com.bubelov.coins.ui.fragment.SignInFragment
import com.bubelov.coins.ui.fragment.SignUpFragment
import com.bubelov.coins.database.sync.DatabaseSync
import com.bubelov.coins.ui.viewmodel.MainViewModel
import com.bubelov.coins.util.PlaceNotificationManager
import com.google.firebase.analytics.FirebaseAnalytics

import javax.inject.Singleton

import dagger.Component

/**
 * @author Igor Bubelov
 */

@Singleton
@Component(modules = arrayOf(MainModule::class))
interface MainComponent {
    fun context(): Context

    fun databaseSync(): DatabaseSync

    fun analytics(): FirebaseAnalytics

    fun notificationManager(): PlaceNotificationManager

    fun notificationAreaRepository(): NotificationAreaRepository
    fun placesRepository(): PlacesRepository
    fun currenciesRepository(): CurrenciesRepository
    fun exchangeRatesRepository(): ExchangeRatesRepository
    fun placeNotificationsRepository(): PlaceNotificationsRepository

    fun placeCategoriesRepository(): PlaceCategoriesRepository
    fun placeCategoriesDataSourceNetwork(): PlaceCategoriesDataSourceApi
    fun placeCategoriesDataSourceDb(): PlaceCategoriesDataSourceDb
    fun placeCategoriesDataSourceMemory(): PlaceCategoriesDataSourceMemory

    fun inject(target: EditPlaceActivity)
    fun inject(target: NotificationAreaActivity)
    fun inject(target: ProfileActivity)

    fun inject(target: SignInFragment)
    fun inject(target: SignUpFragment)
    fun inject(target: SettingsFragment)

    fun inject(target: MainViewModel)

    fun inject(sync: DatabaseSync)
}