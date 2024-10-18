package element_comment

import android.app.Application
import api.Api
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime

class ElementCommentRepo(
    private val api: Api,
    private val app: Application,
    private val queries: ElementCommentQueries,
) {

    suspend fun selectByElementId(elementId: Long): List<ElementComment> {
        return withContext(Dispatchers.IO) {
            queries.selectByElementId(elementId)
        }
    }

    suspend fun selectCount(): Long {
        return withContext(Dispatchers.IO) {
            queries.selectCount()
        }
    }

    suspend fun hasBundledElements(): Boolean {
        return withContext(Dispatchers.IO) {
            app.resources.assets.list("")!!.contains("element-comments.json")
        }
    }

    suspend fun fetchBundledElements() {
        withContext(Dispatchers.IO) {
            app.assets.open("element-comments.json").use { bundledElements ->
                queries.insertOrReplace(bundledElements
                    .toElementCommentsJson()
                    .filter { it.deletedAt == null }
                    .map { it.toElementComment() })
            }
        }
    }

    suspend fun sync(): SyncReport {
        return withContext(Dispatchers.IO) {
            val startedAt = ZonedDateTime.now(ZoneOffset.UTC)
            var newItems = 0L
            var updatedItems = 0L
            var deletedItems = 0L
            var maxKnownUpdatedAt = queries.selectMaxUpdatedAt()

            while (true) {
                val delta = api.getElementComments(maxKnownUpdatedAt, BATCH_SIZE)

                if (delta.isEmpty()) {
                    break
                } else {
                    maxKnownUpdatedAt = ZonedDateTime.parse(delta.maxBy { it.updatedAt }.updatedAt)
                }

                delta.forEach {
                    val cached = queries.selectById(it.id)

                    if (it.deletedAt == null) {
                        if (cached == null) {
                            newItems++
                        } else {
                            updatedItems++
                        }

                        queries.insertOrReplace(listOf(it.toElementComment()))
                    } else {
                        if (cached == null) {
                            // Already evicted from cache, nothing to do here
                        } else {
                            queries.deleteById(it.id)
                            deletedItems++
                        }
                    }
                }

                if (delta.size < BATCH_SIZE) {
                    break
                }
            }

            SyncReport(
                duration = Duration.between(startedAt, ZonedDateTime.now(ZoneOffset.UTC)),
                newElementComments = newItems,
                updatedElementComments = updatedItems,
                deletedElementComments = deletedItems,
            )
        }
    }

    data class SyncReport(
        val duration: Duration,
        val newElementComments: Long,
        val updatedElementComments: Long,
        val deletedElementComments: Long,
    )

    companion object {
        private const val BATCH_SIZE = 5000L
    }
}