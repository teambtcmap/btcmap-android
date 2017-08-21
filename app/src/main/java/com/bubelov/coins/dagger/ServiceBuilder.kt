package com.bubelov.coins.dagger

import com.bubelov.coins.database.sync.DatabaseSyncService
import com.bubelov.coins.database.sync.DatabaseSyncServiceModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * @author Igor Bubelov
 */

@Module
abstract class ServiceBuilder {
    @ContributesAndroidInjector(modules = arrayOf(DatabaseSyncServiceModule::class))
    abstract fun bindDatabaseSyncService(): DatabaseSyncService
}