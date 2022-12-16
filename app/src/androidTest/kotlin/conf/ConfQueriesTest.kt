package conf

import db.inMemoryDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ConfQueriesTest {

    lateinit var queries: ConfQueries

    @Before
    fun beforeEach() {
        queries = ConfQueries(inMemoryDatabase())
    }

    @Test
    fun insertOrReplace() = runBlocking {
        queries.insertOrReplace(ConfRepo.DEFAULT_CONF)
        assertEquals(ConfRepo.DEFAULT_CONF, queries.select())
    }

    @Test
    fun select() = runBlocking {
        assertEquals(null, queries.select())
        queries.insertOrReplace(ConfRepo.DEFAULT_CONF)
        assertEquals(ConfRepo.DEFAULT_CONF, queries.select())
    }
}