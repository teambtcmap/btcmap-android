package conf

import db.Conf
import db.testDb
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class ConfRepoTest {

    @Test
    fun getConf() {
        val db = testDb()
        val repo = ConfRepo(db)
        assertEquals(ConfRepo.DEFAULT_CONF, repo.conf.value)
        val newConf = Conf(lastSyncDate = ZonedDateTime.now())
        repo.update { newConf }
        assertEquals(newConf, repo.conf.value)
    }

    @Test
    fun update() {
        val db = testDb()
        val repo = ConfRepo(db)
        val lastSyncDate = ZonedDateTime.now()
        repo.update { it.copy(lastSyncDate = lastSyncDate) }
        assertEquals(lastSyncDate, repo.conf.value.lastSyncDate)
    }
}