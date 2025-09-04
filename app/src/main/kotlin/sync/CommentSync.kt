package sync

import android.database.sqlite.SQLiteDatabase
import androidx.core.database.sqlite.transaction
import api.ApiImpl
import db.table.comment.CommentQueries
import element_comment.toElementComment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime

object CommentSync {
    private const val BATCH_SIZE = 1_000L

    suspend fun run(db: SQLiteDatabase): Report {
        return withContext(Dispatchers.IO) {
            val startedAt = ZonedDateTime.now(ZoneOffset.UTC)
            var rowsAffected = 0L
            var maxKnownUpdatedAt = CommentQueries.selectMaxUpdatedAt(db)

            while (true) {
                val delta = ApiImpl().getElementComments(maxKnownUpdatedAt, BATCH_SIZE)

                if (delta.isEmpty()) {
                    break
                } else {
                    maxKnownUpdatedAt = ZonedDateTime.parse(delta.maxBy { it.updatedAt }.updatedAt)
                }

                val newOrChanged = delta.filter { it.deletedAt == null }
                val deleted = delta.filter { it.deletedAt != null }

                db.transaction {
                    CommentQueries.insert(newOrChanged.map { it.toElementComment() }, db)

                    deleted.forEach {
                        CommentQueries.deleteById(it.id, db)
                    }
                }

                rowsAffected += delta.size

                if (delta.size < BATCH_SIZE) {
                    break
                }
            }

            Report(
                duration = Duration.between(startedAt, ZonedDateTime.now(ZoneOffset.UTC)),
                rowsAffected = rowsAffected,
            )
        }
    }

    data class Report(
        val duration: Duration,
        val rowsAffected: Long,
    )
}