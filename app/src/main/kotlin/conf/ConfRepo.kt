package conf

import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToOneOrDefault
import db.Conf
import db.Database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

@Single
class ConfRepo(
    private val db: Database,
) {

    companion object {
        val DEFAULT_CONF = Conf(lastSyncDate = null)
    }

    fun load(): Flow<Conf> {
        return db.confQueries.selectAll().asFlow().mapToOneOrDefault(DEFAULT_CONF)
    }

    suspend fun save(applyChanges: (Conf) -> Conf) {
        save(applyChanges(db.confQueries.selectAll().executeAsOneOrNull() ?: DEFAULT_CONF))
    }

    suspend fun save(conf: Conf) {
        withContext(Dispatchers.Default) {
            db.transaction {
                db.confQueries.deleteAll()
                db.confQueries.insert(conf)
            }
        }
    }
}