package org.btcmap.sync

import android.util.Log
import org.btcmap.api.CommentApi
import org.btcmap.api.CommentApi.GetCommentsItem
import org.btcmap.db.table.comment.Comment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.btcmap.db.Database
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime

object CommentSync {

    private const val BATCH_SIZE = 1_000L

    data class Report(
        val duration: Duration,
        val rowsAffected: Long,
    )

    suspend fun run(db: Database): Report {
        return withContext(Dispatchers.IO) {
            val startedAt = ZonedDateTime.now(ZoneOffset.UTC)
            var rowsAffected = 0L
            var maxKnownUpdatedAt = db.comment.selectMaxUpdatedAt()

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
                    db.comment.insert(newOrChanged.map { it.toComment() })

                    deleted.forEach {
                        db.comment.deleteById(it.id)
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