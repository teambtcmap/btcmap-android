package app

import android.app.Application
import db.inMemoryDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.module

class TestApp : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.DEBUG)

            androidContext(this@TestApp)

            modules(
                appModule,
                module { single { inMemoryDatabase() } }
            )
        }
    }
}