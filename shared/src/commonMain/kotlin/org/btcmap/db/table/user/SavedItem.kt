package org.btcmap.db.table.user

import kotlinx.serialization.Serializable

@Serializable
data class SavedItem(
    val id: Long,
    val name: String,
)
