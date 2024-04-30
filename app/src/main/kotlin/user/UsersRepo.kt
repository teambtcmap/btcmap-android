package user

import android.content.Context
import api.Api
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
        val startMillis = System.currentTimeMillis()
        var count = 0L

        while (true) {
            val users = api.getUsers(queries.selectMaxUpdatedAt(), BATCH_SIZE)
            count += users.size
            queries.insertOrReplace(users.map { it.toUser() })

            if (users.size < BATCH_SIZE) {
                break
            }
        }

        return SyncReport(
            timeMillis = System.currentTimeMillis() - startMillis,
            createdOrUpdatedUsers = count,
        )
    }

    data class SyncReport(
        val timeMillis: Long,
        val createdOrUpdatedUsers: Long,
    )

    companion object {
        private const val BATCH_SIZE = 500L
    }
}