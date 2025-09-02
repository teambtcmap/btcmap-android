package app

import api.Api
import api.ApiImpl
import element.ElementQueries
import element.ElementsRepo
import map.MapModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import search.SearchModel
import search.SearchResultModel
import element_comment.ElementCommentQueries
import element_comment.ElementCommentRepo

val appModule = module {
    single { ApiImpl() }.bind(Api::class)

    singleOf(::ElementQueries)
    singleOf(::ElementsRepo)

    singleOf(::ElementCommentQueries)
    singleOf(::ElementCommentRepo)

    viewModelOf(::MapModel)

    viewModelOf(::SearchModel)
    viewModelOf(::SearchResultModel)
}