package com.bubelov.coins.dagger

import android.arch.persistence.room.Room
import android.content.Context
import com.bubelov.coins.db.Database
import com.bubelov.coins.db.DatabaseConfig
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * @author Igor Bubelov
 */

@Module
class DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(context: Context, databaseConfig: DatabaseConfig) = Room.databaseBuilder(context, Database::class.java, "db.sqlite3").apply {
        if (databaseConfig.canUseMainThread) {
            allowMainThreadQueries()
        }
    }.build()

    @Provides
    @Singleton
    fun provideDatabaseConfig() = DatabaseConfig(canUseMainThread = false)
}