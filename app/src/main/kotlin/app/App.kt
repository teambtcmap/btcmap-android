package app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import areas.AreaQueries
import areas.AreaResultModel
import areas.AreasModel
import areas.AreasRepo
import conf.ConfQueries
import conf.ConfRepo
import db.persistentDatabase
import elements.ElementQueries
import elements.ElementsRepo
import events.EventQueries
import events.EventsModel
import events.EventsRepo
import kotlinx.serialization.json.Json
import location.UserLocationRepository
import map.MapModel
import okhttp3.OkHttpClient
import okhttp3.brotli.BrotliInterceptor
import org.btcmap.BuildConfig
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.bind
import org.koin.dsl.module
import reports.ReportQueries
import reports.ReportsModel
import reports.ReportsRepo
import search.SearchModel
import search.SearchResultModel
import sync.Sync
import users.UserQueries
import users.UsersModel
import users.UsersRepo

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            if (BuildConfig.DEBUG) androidLogger(Level.DEBUG)
            androidContext(this@App)

            modules(module {
                single { persistentDatabase(this@App) }
                single { OkHttpClient.Builder().addInterceptor(BrotliInterceptor).build() }
                single { Json { ignoreUnknownKeys = true } }

                // Previously generated
                single { AreaQueries(get()) }
                single { AreasRepo(get(), get(), get()) }
                single { ConfQueries(get()) }
                single { ConfRepo(get()) }
                single { ElementQueries(get()) }
                single { ElementsRepo(get(), get(), get(), get()) }
                single { EventQueries(get()) }
                single { EventsRepo(get(), get(), get()) }
                single { UserLocationRepository(get()) }
                single { ReportQueries(get()) }
                single { ReportsRepo(get(), get(), get()) }
                single { Sync(get(), get(), get(), get(), get(), get()) }
                single { UserQueries(get()) }
                single { UsersRepo(get(), get(), get()) }
                viewModel { AreaResultModel() }
                viewModel {
                    AreasModel(get(), get())
                } bind (AndroidViewModel::class)
                viewModel {
                    EventsModel(get(), get(), get())
                } bind (AndroidViewModel::class)
                viewModel { MapModel(get(), get(), get(), get()) }
                viewModel { ReportsModel(get()) }
                viewModel { SearchModel(get(), get()) }
                viewModel { SearchResultModel() }
                viewModel {
                    UsersModel(get(), get())
                } bind (AndroidViewModel::class)
            })
        }
    }
}