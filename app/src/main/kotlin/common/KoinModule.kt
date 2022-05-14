package common

import android.content.Context
import android.location.LocationManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.squareup.sqldelight.ColumnAdapter
import db.Database
import map.MapModel
import settings.ConfRepository
import map.PlacesRepository
import map.PlaceIconsRepository
import search.PlacesSearchResultViewModel
import search.PlacesSearchModel
import settings.SettingsViewModel
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver
import db.Place
import location.LocationRepository
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import java.time.LocalDateTime

val koinModule = module {

    viewModelOf(::ActivityModel)
    viewModelOf(::MapModel)
    viewModelOf(::PlacesSearchModel)
    viewModelOf(::PlacesSearchResultViewModel)
    viewModelOf(::SettingsViewModel)

    singleOf(::PlacesRepository)
    singleOf(::PlaceIconsRepository)
    singleOf(::LocationRepository)
    singleOf(::ConfRepository)

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

        Database(get(), Place.Adapter(jsonObjectAdapter))
    }
    single { get<Database>().placeQueries }
    single { get<Database>().confQueries }

    single { get<Context>().resources }
    single { get<Context>().assets }
    single { get<Context>().getSystemService(Context.LOCATION_SERVICE) as LocationManager }

    single<SqlDriver> {
        AndroidSqliteDriver(
            schema = Database.Schema,
            context = get(),
            name = "btcmap.db"
        )
    }

    single {
        GsonBuilder()
            .registerTypeAdapter(LocalDateTime::class.java, DateTimeAdapter())
            .create()
    }
}