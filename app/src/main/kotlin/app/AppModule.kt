package app

import android.content.Context
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import api.Api
import api.ApiImpl
import area.AreaModel
import area.AreaQueries
import area.AreaResultModel
import area.AreasModel
import area.AreasRepo
import conf.ConfQueries
import conf.ConfRepo
import db.DB_FILE_NAME
import db.Database
import delivery.DeliveryModel
import element.ElementQueries
import element.ElementsRepo
import event.EventQueries
import event.EventsModel
import event.EventsRepo
import io.requery.android.database.sqlite.SQLiteOpenHelper
import issue.IssuesModel
import location.UserLocationRepository
import map.MapModel
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
    singleOf(::Database).bind(SQLiteOpenHelper::class)
    single {
        val context = get<Context>()
        BundledSQLiteDriver().open(context.getDatabasePath(DB_FILE_NAME).absolutePath)
            .apply { execSQL(Database.CREATE_AREA_TABLE) }
    }

    single { ApiImpl() }.bind(Api::class)

    singleOf(::Sync)

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

    singleOf(::UserQueries)
    singleOf(::UsersRepo)
    viewModelOf(::UsersModel)

    viewModelOf(::MapModel)

    viewModelOf(::SearchModel)
    viewModelOf(::SearchResultModel)

    viewModelOf(::IssuesModel)

    viewModelOf(::DeliveryModel)
}