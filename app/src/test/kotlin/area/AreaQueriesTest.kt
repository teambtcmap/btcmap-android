package area

import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import db.Database
import org.json.JSONObject
import org.junit.Test
import java.time.ZonedDateTime

class AreaQueriesTest {

    @Test
    fun insert() {
//        val queries = AreaQueries(
//            BundledSQLiteDriver().open(":memory:").apply { execSQL(Database.CREATE_AREA_TABLE) })
//        val area = Area(id = 1, tags = JSONObject(), updatedAt = ZonedDateTime.now())
//        queries.insertOrReplace(area)
//        assert(queries.selectById(area.id) == area)
    }
}