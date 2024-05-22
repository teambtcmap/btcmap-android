package user

import android.content.Context
import api.Api
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime

class UsersRepo(
    private val api: Api,
    private val queries: UserQueries,
    private val context: Context,
) {

    suspend fun selectAll(): List<UserListItem> {
        return withContext(Dispatchers.IO) {
            queries.selectAll()
        }
    }

    suspend fun selectById(id: Long): User? {
        return withContext(Dispatchers.IO) {
            queries.selectById(id)
        }
    }

    suspend fun selectCount(): Long {
        return withContext(Dispatchers.IO) {
            queries.selectCount()
        }
    }

    suspend fun hasBundledUsers(): Boolean {
        return withContext(Dispatchers.IO) {
            context.resources.assets.list("")!!.contains("users.json")
        }
    }

    suspend fun fetchBundledUsers() {
        withContext(Dispatchers.IO) {
            context.assets.open("users.json").use { bundledUsers ->
                queries.insertOrReplace(
                    bundledUsers
                        .toUsersJson()
                        .filter { it.deletedAt == null }
                        .map { it.toUser() }
                )
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
                val delta = api.getUsers(maxKnownUpdatedAt, BATCH_SIZE)

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

                        queries.insertOrReplace(listOf(it.toUser()))
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
                newUsers = newItems,
                updatedUsers = updatedItems,
                deletedUsers = deletedItems,
            )
        }
    }

    data class SyncReport(
        val duration: Duration,
        val newUsers: Long,
        val updatedUsers: Long,
        val deletedUsers: Long,
    )

    companion object {
        private const val BATCH_SIZE = 500L
    }
}