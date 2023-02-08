package app

import area.AreaQueries
import area.AreaResultModel
import area.AreasModel
import area.AreaModel
import area.AreasRepo
import conf.ConfQueries
import conf.ConfRepo
import element.ElementQueries
import element.ElementsRepo
import events.EventQueries
import events.EventsModel
import events.EventsRepo
import kotlinx.serialization.json.Json
import location.UserLocationRepository
import map.MapModel
import okhttp3.OkHttpClient
import okhttp3.brotli.BrotliInterceptor
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
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
import filter.FilterElementsModel
import filter.FilterResultModel

val appModule = module {
    single { OkHttpClient.Builder().addInterceptor(BrotliInterceptor).build() }
    single { Json { ignoreUnknownKeys = true } }

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

    viewModelOf(::FilterElementsModel)
    viewModelOf(::FilterResultModel)
}