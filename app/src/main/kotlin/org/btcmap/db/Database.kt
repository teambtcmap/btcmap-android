package org.btcmap.db

import androidx.core.database.sqlite.transaction
import org.btcmap.db.table.comment.CommentQueries
import org.btcmap.db.table.event.EventQueries
import org.btcmap.db.table.place.PlaceQueries

class Database(private val helper: DbHelper) {
    val place = PlaceQueries(helper.writableDatabase)
    val comment = CommentQueries(helper.writableDatabase)
    val event = EventQueries(helper.writableDatabase)

    fun transaction(block: () -> Unit) {
        helper.writableDatabase.transaction {
            block()
        }
    }
}