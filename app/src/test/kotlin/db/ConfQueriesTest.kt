package db

import conf.ConfRepo
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random
import kotlin.random.nextInt

class ConfQueriesTest {

    @Test
    fun insert() {
        testDb().confQueries.apply {
            val conf = ConfRepo.DEFAULT_CONF
            insert(conf)
            assertEquals(conf, select().executeAsOne())
        }
    }

    @Test
    fun selectAll() {
        testDb().confQueries.apply {
            val rows = (0..Random.nextInt(6)).map { ConfRepo.DEFAULT_CONF }
            rows.forEach { insert(it) }
            assertEquals(rows, select().executeAsList())
        }
    }

    @Test
    fun deleteAll() {
        testDb().confQueries.apply {
            repeat(Random.nextInt(1..5)) { insert(ConfRepo.DEFAULT_CONF) }
            assert(select().executeAsList().isNotEmpty())
            delete()
            assert(select().executeAsList().isEmpty())
        }
    }
}