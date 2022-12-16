package conf

import db.inMemoryDatabase
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZonedDateTime

class ConfRepoTest {

    @Test
    fun getConf() {
        val repo = ConfRepo(ConfQueries(inMemoryDatabase()))
        assertEquals(ConfRepo.DEFAULT_CONF, repo.conf.value)
        val newConf = repo.conf.value.copy(lastSyncDate = ZonedDateTime.now())
        repo.update { newConf }
        assertEquals(newConf, repo.conf.value)
    }

    @Test
    fun update() {
        val repo = ConfRepo(ConfQueries(inMemoryDatabase()))
        val lastSyncDate = ZonedDateTime.now()
        repo.update { it.copy(lastSyncDate = lastSyncDate) }
        assertEquals(lastSyncDate, repo.conf.value.lastSyncDate)
    }
}