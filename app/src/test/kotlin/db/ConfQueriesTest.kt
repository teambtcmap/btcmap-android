package db

import kotlin.test.Test
import kotlin.test.assertEquals

class ConfQueriesTest {

    @Test
    fun insert() {
        val db = testDb()
        val conf = Conf(lastSyncDate = null)
        db.confQueries.insert(conf)
        assertEquals(conf, db.confQueries.selectAll().executeAsOne())
    }

    @Test
    fun selectAll() {
        insert()
    }

    @Test
    fun deleteAll() {
        val db = testDb()
        val conf = Conf(lastSyncDate = null)
        db.confQueries.insert(conf)
        db.confQueries.deleteAll()
    }
}