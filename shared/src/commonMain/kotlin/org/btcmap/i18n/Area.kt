package org.btcmap.i18n

import com.google.gson.JsonObject
import org.btcmap.db.table.area.Area
import org.btcmap.db.table.area.SearchArea
import java.util.Locale

/**
 * The area's name in the device language.
 *
 * The fallback mirrors the server's `Area::localized_tag`: the current language
 * wins, then English, then the area's base name. The English step is deliberate
 * and differs from a place's `getLocalizedName`: a place's synced `name` is
 * already resolved to English or the base tag by the API, while the areas delta
 * caches the bare base tag, so the client re-applies the English step here. It
 * also keeps a cached area reading the same as one fetched from the single-area
 * endpoint, which localizes server-side with the same chain.
 */
fun Area.getLocalizedName(): String = areaLocalizedName(name, localizedName)

/** [getLocalizedName] for a search row, which omits fields search does not use. */
fun SearchArea.getLocalizedName(): String = areaLocalizedName(name, localizedName)

/** The area's description in the device language, with the same fallback. */
fun Area.getLocalizedDescription(): String? =
    localizedDescription.translated(Locale.getDefault().language) ?: description

/**
 * Every name the area can be found by: its base `name` tag plus each
 * `name:<lang>` translation. Search matches against all of them, so a query in
 * any cached language finds the area even when the device language differs.
 */
fun Area.getSearchableNames(): List<String> =
    areaSearchableNames(name, localizedName)

/** [getSearchableNames] for a search row. */
fun SearchArea.getSearchableNames(): List<String> =
    areaSearchableNames(name, localizedName)

private fun areaLocalizedName(name: String, localizedName: JsonObject?): String =
    localizedName.translated(Locale.getDefault().language) ?: name

private fun areaSearchableNames(name: String, localizedName: JsonObject?): List<String> =
    listOf(name) + localizedName.localizedValues()
