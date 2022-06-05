package db

import conf.ConfRepo
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.test.Test
import kotlin.test.assertEquals

class ConfQueriesTest {

    @Test
    fun insert() {
        testDb().confQueries.apply {
            val conf = ConfRepo.DEFAULT_CONF
            insert(conf)
            assertEquals(conf, selectAll().executeAsOne())
        }
    }

    @Test
    fun selectAll() {
        testDb().confQueries.apply {
            val rows = (0..Random.nextInt(6)).map { ConfRepo.DEFAULT_CONF }
            rows.forEach { insert(it) }
            assertEquals(rows, selectAll().executeAsList())
        }
    }

    @Test
    fun deleteAll() {
        testDb().confQueries.apply {
            repeat(Random.nextInt(1..5)) { insert(ConfRepo.DEFAULT_CONF) }
            assert(selectAll().executeAsList().isNotEmpty())
            deleteAll()
            assert(selectAll().executeAsList().isEmpty())
        }
    }
}