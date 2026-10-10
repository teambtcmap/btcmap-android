package org.btcmap.db.table.user

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Long,
    val name: String,
    val roles: List<String>,
    val savedPlaces: List<SavedItem>,
    val savedAreas: List<SavedItem>,
    /**
     * The area ids the account is restricted to, or empty when unrestricted.
     * The default keeps pre-geofence cached profiles decodable.
     */
    val geofence: List<Long> = emptyList(),
)
