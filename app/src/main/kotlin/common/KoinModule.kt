package common

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.squareup.sqldelight.ColumnAdapter
import db.Database
import map.MapModel
import map.PlacesRepository
import map.PlaceIconsRepository
import search.PlacesSearchResultViewModel
import search.PlacesSearchModel
import com.squareup.sqldelight.android.AndroidSqliteDriver
import db.Place
import location.LocationRepository
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val koinModule = module {

    viewModelOf(::ActivityModel)
    viewModelOf(::MapModel)
    viewModelOf(::PlacesSearchModel)
    viewModelOf(::PlacesSearchResultViewModel)

    singleOf(::PlacesRepository)
    singleOf(::PlaceIconsRepository)
    singleOf(::LocationRepository)

    single {
        val jsonObjectAdapter = object : ColumnAdapter<JsonObject, String> {
            override fun decode(databaseValue: String) =
                if (databaseValue.isEmpty()) {
                    JsonObject()
                } else {
                    Gson().fromJson(databaseValue, JsonObject::class.java)
                }

            override fun encode(value: JsonObject) = value.toString()
        }

        Database(
            AndroidSqliteDriver(
                schema = Database.Schema,
                context = get(),
                name = "btcmap.db"
            ), Place.Adapter(jsonObjectAdapter)
        )
    }
}