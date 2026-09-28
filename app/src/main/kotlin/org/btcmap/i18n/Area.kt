package org.btcmap.i18n

import org.btcmap.db.table.area.Area
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
fun Area.getLocalizedName(): String =
    localizedName.translated(Locale.getDefault().language) ?: name

/** The area's description in the device language, with the same fallback. */
fun Area.getLocalizedDescription(): String? =
    localizedDescription.translated(Locale.getDefault().language) ?: description
