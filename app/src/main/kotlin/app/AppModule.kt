package app

import android.content.Context
import api.Api
import api.ApiImpl
import db.Database
import element.ElementQueries
import element.ElementsRepo
import map.MapModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import search.SearchModel
import search.SearchResultModel
import user.UserQueries
import user.UsersModel
import user.UsersRepo
import element_comment.ElementCommentQueries
import element_comment.ElementCommentRepo

val appModule = module {
    single { Database(get<Context>().getDatabasePath("btcmap-2025-08-24.db").absolutePath).conn }

    single { ApiImpl() }.bind(Api::class)

    singleOf(::ElementQueries)
    singleOf(::ElementsRepo)

    singleOf(::ElementCommentQueries)
    singleOf(::ElementCommentRepo)

    singleOf(::UserQueries)
    singleOf(::UsersRepo)
    viewModelOf(::UsersModel)

    viewModelOf(::MapModel)

    viewModelOf(::SearchModel)
    viewModelOf(::SearchResultModel)
}