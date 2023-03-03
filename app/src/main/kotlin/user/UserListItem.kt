package user

import okhttp3.HttpUrl

data class UserListItem(
    val id: Long,
    val image: HttpUrl?,
    val name: String,
    val tips: String,
    val changes: Long,
)
