package com.bubelov.coins.dagger

import android.arch.persistence.room.Room
import android.content.Context
import com.bubelov.coins.database.Database
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
    fun provideDatabase(context: Context) = Room.databaseBuilder(context, Database::class.java, "data").build()
}