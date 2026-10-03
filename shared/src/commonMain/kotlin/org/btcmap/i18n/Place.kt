package org.btcmap.i18n

import kotlinx.serialization.json.JsonObject
import org.btcmap.db.table.place.Place
import org.btcmap.db.table.place.SearchPlace
import java.util.Locale

/**
 * The place's name in the device language, or an empty string when it has none.
 *
 * Unlike an area, no English fallback happens here: the API has already
 * resolved a place's synced `name` to English (or the base tag), so the cached
 * `name` *is* the fallback and only the current language is looked up.
 */
fun Place.getLocalizedName(): String = placeLocalizedName(name, localizedName)

/** [getLocalizedName] for a search row, which omits fields search does not use. */
fun SearchPlace.getLocalizedName(): String = placeLocalizedName(name, localizedName)

/**
 * Every name the place can be found by: its synced `name` plus each
 * `name:<lang>` translation. Search matches against all of them, so a query in
 * any cached language finds the place even when the device language differs.
 */
fun Place.getSearchableNames(): List<String> =
    placeSearchableNames(name, localizedName)

/** [getSearchableNames] for a search row. */
fun SearchPlace.getSearchableNames(): List<String> =
    placeSearchableNames(name, localizedName)

private fun placeLocalizedName(name: String?, localizedName: JsonObject?): String =
    localizedName?.stringAt(Locale.getDefault().language) ?: name.orEmpty()

private fun placeSearchableNames(name: String?, localizedName: JsonObject?): List<String> =
    listOfNotNull(name) + localizedName.localizedValues()
