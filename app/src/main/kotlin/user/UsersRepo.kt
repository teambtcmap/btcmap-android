package user

import api.Api

class UsersRepo(
    private val api: Api,
    private val queries: UserQueries,
) {

    suspend fun selectAll() = queries.selectAll()

    suspend fun selectById(id: Long) = queries.selectById(id)

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