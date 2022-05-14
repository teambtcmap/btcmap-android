package common

import android.app.Application
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.squareup.sqldelight.ColumnAdapter
import com.squareup.sqldelight.android.AndroidSqliteDriver
import db.Database
import db.Place
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import map.MapModel
import org.koin.android.ext.koin.androidLogger
import search.PlacesSearchModel
import search.PlacesSearchResultViewModel
import map.PlacesRepository
import map.PlaceIconsRepository
import location.LocationRepository

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(listOf(module {
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
                            name = "btcmap.db",
                        ), Place.Adapter(jsonObjectAdapter)
                    )
                }
            }))
        }
    }
}