package com.bubelov.coins.di

import com.bubelov.coins.db.sync.DatabaseSyncService
import com.bubelov.coins.db.sync.DatabaseSyncServiceModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * @author Igor Bubelov
 */

@Module
abstract class ServiceBuilder {
    @ContributesAndroidInjector(modules = arrayOf(DatabaseSyncServiceModule::class))
    abstract fun contributeDatabaseSyncServiceInjector(): DatabaseSyncService
}