package conf

import db.testDb
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZonedDateTime

class ConfRepoTest {

    @Test
    fun getConf() {
        val db = testDb()
        val repo = ConfRepo(db)
        assertEquals(ConfRepo.DEFAULT_CONF, repo.conf.value)
        val newConf = repo.conf.value.copy(lastSyncDate = ZonedDateTime.now())
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