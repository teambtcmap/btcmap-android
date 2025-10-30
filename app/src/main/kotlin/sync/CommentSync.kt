package sync

import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.core.database.sqlite.transaction
import api.CommentApi
import api.CommentApi.GetCommentsItem
import db.table.comment.Comment
import db.table.comment.CommentQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime

object CommentSync {

    private const val BATCH_SIZE = 1_000L

    data class Report(
        val duration: Duration,
        val rowsAffected: Long,
    )

    suspend fun run(db: SQLiteDatabase): Report {
        return withContext(Dispatchers.IO) {
            val startedAt = ZonedDateTime.now(ZoneOffset.UTC)
            var rowsAffected = 0L
            var maxKnownUpdatedAt = CommentQueries.selectMaxUpdatedAt(db)

            while (true) {
                val delta = try {
                    CommentApi.getComments(maxKnownUpdatedAt, BATCH_SIZE)
                } catch (t: Throwable) {
                    Log.e(null, null, t)
                    return@withContext Report(
                        duration = Duration.between(startedAt, ZonedDateTime.now(ZoneOffset.UTC)),
                        rowsAffected = rowsAffected,
                    )
                }

                if (delta.isEmpty()) {
                    break
                } else {
                    maxKnownUpdatedAt = ZonedDateTime.parse(delta.maxBy { it.updatedAt }.updatedAt)
                }

                val newOrChanged = delta.filter { it.deletedAt == null }
                val deleted = delta.filter { it.deletedAt != null }

                db.transaction {
                    CommentQueries.insert(newOrChanged.map { it.toComment() }, db)

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

    private fun GetCommentsItem.toComment(): Comment {
        return Comment(
            id = id,
            placeId = elementId!!,
            comment = comment!!,
            createdAt = ZonedDateTime.parse(createdAt!!),
            updatedAt = ZonedDateTime.parse(updatedAt),
        )
    }
}