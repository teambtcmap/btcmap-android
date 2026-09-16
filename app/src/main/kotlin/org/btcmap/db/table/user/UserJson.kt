package org.btcmap.db.table.user

import com.google.gson.Gson

internal object UserJson {
    private val gson = Gson()

    fun rolesToJson(roles: List<String>): String = gson.toJson(roles)

    fun savedItemsToJson(items: List<SavedItem>): String = gson.toJson(items)

    fun rolesFromJson(json: String): List<String> =
        gson.fromJson(json, Array<String>::class.java)?.toList() ?: emptyList()

    fun savedItemsFromJson(json: String): List<SavedItem> =
        gson.fromJson(json, Array<SavedItem>::class.java)?.toList() ?: emptyList()
}
