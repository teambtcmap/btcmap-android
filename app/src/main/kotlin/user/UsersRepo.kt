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

    suspend fun selectAll() = queries.selectAll()

    suspend fun selectById(id: Long) = queries.selectById(id)

    suspend fun selectCount() = queries.selectCount()

    suspend fun hasBundledUsers(): Boolean {
        return withContext(Dispatchers.IO) {
            context.resources.assets.list("")!!.contains("users.json")
        }
    }

    suspend fun fetchBundledUsers() {
        withContext(Dispatchers.IO) {
            context.assets.open("users.json").use { bundledUsers ->
                val users = bundledUsers.toUsersJson().map { it.toUser() }
                queries.insertOrReplace(users)
            }
        }
    }

    suspend fun sync(): SyncReport {
        val startedAt = ZonedDateTime.now(ZoneOffset.UTC)
        var newUsers = 0L
        var updatedUsers = 0L
        val maxUpdatedAtBeforeSync = queries.selectMaxUpdatedAt()

        while (true) {
            val users = api.getUsers(queries.selectMaxUpdatedAt(), BATCH_SIZE).map { it.toUser() }

            users.forEach {
                if (maxUpdatedAtBeforeSync == null
                    || it.createdAt.isAfter(maxUpdatedAtBeforeSync)
                ) {
                    newUsers += 1
                } else {
                    updatedUsers += 1
                }
            }

            queries.insertOrReplace(users)

            if (users.size < BATCH_SIZE) {
                break
            }
        }

        return SyncReport(
            duration = Duration.between(startedAt, ZonedDateTime.now(ZoneOffset.UTC)),
            newUsers = newUsers,
            updatedUsers = updatedUsers,
        )
    }

    data class SyncReport(
        val duration: Duration,
        val newUsers: Long,
        val updatedUsers: Long,
    )

    companion object {
        private const val BATCH_SIZE = 500L
    }
}