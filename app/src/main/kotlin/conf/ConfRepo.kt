package conf

import db.Conf
import db.Database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

@Single
class ConfRepo(
    private val db: Database,
) {

    private val _conf: MutableStateFlow<Conf> = MutableStateFlow(
        runBlocking { db.confQueries.select().executeAsOneOrNull() ?: DEFAULT_CONF }
    )

    val conf: StateFlow<Conf> = _conf.asStateFlow()

    init {
        conf.onEach {
            withContext(Dispatchers.Default) {
                db.transaction {
                    db.confQueries.delete()
                    db.confQueries.insert(it)
                }
            }
        }.launchIn(GlobalScope)
    }

    fun update(newConf: (Conf) -> Conf) {
        _conf.update { newConf(conf.value) }
    }

    companion object {
        val DEFAULT_CONF = Conf(
            lastSyncDate = null,
            themedPins = true,
            darkMap = false,
            viewport_north_lat = 11.994133785187255,
            viewport_east_lon = 121.95219572432649,
            viewport_south_lat = 11.945223417353624,
            viewport_west_lon = 121.90219745907318,
            showTags = false,
            osmLogin = "",
            osmPassword = "",
        )
    }
}