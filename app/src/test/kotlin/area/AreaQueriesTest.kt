package area

import db.Database
import org.json.JSONObject
import org.junit.Ignore
import org.junit.Test
import java.time.ZonedDateTime

class AreaQueriesTest {

    @Ignore
    @Test
    fun insert() {
        val queries = AreaQueries(Database(":memory:"))
        val area = Area(id = 1, tags = JSONObject(), updatedAt = ZonedDateTime.now())
        queries.insertOrReplace(listOf(area))
        assert(queries.selectById(area.id) == area)
    }
}