package app

import android.content.Context
import api.Api
import api.ApiImpl
import area.AreaModel
import area.AreaQueries
import area.AreaResultModel
import area.AreasModel
import area.AreasRepo
import conf.ConfQueries
import conf.ConfRepo
import db.Database
import element.ElementQueries
import element.ElementsRepo
import event.EventQueries
import event.EventsModel
import event.EventsRepo
import io.requery.android.database.sqlite.SQLiteOpenHelper
import issue.IssuesModel
import location.UserLocationRepository
import map.MapModel
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.brotli.BrotliInterceptor
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import reports.ReportQueries
import reports.ReportsModel
import reports.ReportsRepo
import search.SearchModel
import search.SearchResultModel
import sync.BackgroundSyncScheduler
import sync.Sync
import sync.SyncNotificationController
import user.UserQueries
import user.UsersModel
import user.UsersRepo

val appModule = module {
    single {
        OkHttpClient.Builder()
            .addInterceptor(BrotliInterceptor)
            .apply {
                if (get<Context>().isDebuggable()) {
                    addInterceptor {
                        android.util.Log.d("okhttp", it.request().url.toString())
                        it.proceed(it.request())
                    }
                }
            }
            .addInterceptor {
                var res = it.proceed(it.request())

                var retryAttempts = 0

                while (res.code == 429 && retryAttempts < 10) {
                    android.util.Log.w("okhttp", "Got 429, retrying ${it.request().url}")
                    res.close()
                    Thread.sleep(retryAttempts * 1000 + (Math.random() * 1000.0).toLong())
                    res = it.proceed(it.request())
                    retryAttempts++
                }

                res
            }
            .build()
    }

    single {
        ApiImpl(
            baseUrl = "https://api.btcmap.org".toHttpUrl(),
            httpClient = get(),
        )
    }.bind(Api::class)

    singleOf(::Database).bind(SQLiteOpenHelper::class)

    singleOf(::BackgroundSyncScheduler)
    singleOf(::SyncNotificationController)

    singleOf(::AreaQueries)
    singleOf(::AreasRepo)
    viewModelOf(::AreasModel)
    viewModelOf(::AreaModel)
    viewModelOf(::AreaResultModel)

    singleOf(::ElementQueries)
    singleOf(::ElementsRepo)

    singleOf(::ConfQueries)
    singleOf(::ConfRepo)

    singleOf(::EventQueries)
    singleOf(::EventsRepo)
    viewModelOf(::EventsModel)

    singleOf(::UserLocationRepository)

    singleOf(::ReportQueries)
    singleOf(::ReportsRepo)
    viewModelOf(::ReportsModel)

    singleOf(::Sync)

    singleOf(::UserQueries)
    singleOf(::UsersRepo)
    viewModelOf(::UsersModel)

    viewModelOf(::MapModel)

    viewModelOf(::SearchModel)
    viewModelOf(::SearchResultModel)

    viewModelOf(::IssuesModel)
}