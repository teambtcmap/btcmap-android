package com.bubelov.coins.ui.viewmodel

import android.arch.lifecycle.ViewModel
import com.bubelov.coins.dagger.Injector
import com.bubelov.coins.db.sync.DatabaseSync
import com.bubelov.coins.repository.place.PlacesRepository
import com.bubelov.coins.repository.synclogs.SyncLogsRepository
import com.bubelov.coins.util.PlaceNotificationManager
import javax.inject.Inject

/**
 * @author Igor Bubelov
 */

class SettingsViewModel : ViewModel() {
    @Inject lateinit var placesRepository: PlacesRepository

    @Inject lateinit var syncLogsRepository: SyncLogsRepository

    @Inject lateinit var placeNotificationsManager: PlaceNotificationManager

    @Inject lateinit var databaseSync: DatabaseSync

    init { Injector.appComponent.inject(this) }
}