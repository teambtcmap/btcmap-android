package org.btcmap.util

import android.content.res.AssetManager
import android.graphics.Typeface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.btcmap.App

private const val ICON_FONT_ASSET = "material-symbols-outlined-2026-08-28.ttf"

/**
 * Set once by [initIconTypeface] from [App.onCreate]. The delegate closes over
 * the application's [AssetManager], so the font asset it reads cannot be
 * swapped out after startup.
 */
private lateinit var iconTypefaceDelegate: Lazy<Typeface>

/**
 * The Material Symbols icon font.
 *
 * The font is ~10 MB and building the [Typeface] from it takes a few tens of
 * milliseconds, so it is built on first use instead of in [App.onCreate], where
 * it would sit on the startup critical path. [initIconTypeface] warms it on a
 * background scope so the first real use does not pay for it either.
 */
val iconTypeface: Typeface get() = iconTypefaceDelegate.value

/**
 * Wires the icon font to the application's assets and warms it on [scope].
 * Must be called from [App.onCreate] before any screen can read
 * [iconTypeface].
 */
fun initIconTypeface(app: App, scope: CoroutineScope) {
    iconTypefaceDelegate = lazy { buildIconTypeface(app.assets) }
    scope.launch { iconTypeface }
}

private fun buildIconTypeface(assets: AssetManager): Typeface =
    Typeface.Builder(assets, ICON_FONT_ASSET)
        .setFontVariationSettings("'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24")
        .build()
