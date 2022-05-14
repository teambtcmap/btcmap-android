package settings

import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToOneOrDefault
import db.Conf
import db.ConfQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ConfRepository(
    private val confQueries: ConfQueries,
) {

    fun select() = confQueries.select().asFlow().mapToOneOrDefault(DEFAULT_CONF)

    suspend fun upsert(conf: Conf) {
        withContext(Dispatchers.Default) {
            confQueries.transaction {
                confQueries.delete()
                confQueries.insert(conf)
            }
        }
    }

    companion object {
        val DEFAULT_CONF = Conf(
            distanceUnits = "",
            notificationAreaLat = null,
            notificationAreaLon = null,
            notificationAreaRadiusMeters = null,
        )
    }
}