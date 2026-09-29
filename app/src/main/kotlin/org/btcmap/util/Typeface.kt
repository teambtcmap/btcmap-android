package org.btcmap.util

import android.content.res.AssetManager
import android.graphics.Typeface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.btcmap.App

private lateinit var assets: AssetManager

/**
 * The Material Symbols icon font.
 *
 * The font is ~10 MB and building the [Typeface] from it takes a few tens of
 * milliseconds, so it is built on first use instead of in [App.onCreate], where
 * it would sit on the startup critical path. [init] warms it on a background
 * scope so the first real use does not pay for it either.
 */
val iconTypeface: Typeface by lazy {
    Typeface.Builder(assets, "material-symbols-outlined-2026-08-28.ttf")
        .setFontVariationSettings("'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24")
        .build()
}

fun init(app: App, scope: CoroutineScope) {
    assets = app.assets
    scope.launch { iconTypeface }
}
