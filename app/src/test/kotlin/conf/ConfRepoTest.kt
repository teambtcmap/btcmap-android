package conf

import db.Conf
import db.testDb
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class ConfRepoTest {

    @Test
    fun load() = runBlocking {
        val db = testDb()
        val repo = ConfRepo(db)
        assertEquals(ConfRepo.DEFAULT_CONF, repo.load().first())
        val newConf = Conf(lastSyncDate = ZonedDateTime.now())
        repo.save(newConf)
        assertEquals(newConf, repo.load().first())
    }

    @Test
    fun save() = runBlocking {
        val db = testDb()
        val repo = ConfRepo(db)
        val lastSyncDate = ZonedDateTime.now()
        repo.save { it.copy(lastSyncDate = lastSyncDate) }
        assertEquals(lastSyncDate, repo.load().first().lastSyncDate)
    }
}