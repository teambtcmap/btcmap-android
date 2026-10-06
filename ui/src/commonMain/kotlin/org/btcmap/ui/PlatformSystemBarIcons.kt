package org.btcmap.ui

import androidx.compose.runtime.Composable

/**
 * Tints the platform's system-bar icons for the screen behind them, so they keep
 * contrast: [light] is true when that background is dark and the icons must draw
 * light.
 *
 * The app draws edge to edge, so on Android the status and navigation bars are
 * transparent and overlay the current screen. Every screen but the map paints
 * the app theme's background, whose tone already matches the icons the platform
 * picked; the map paints a style that can be light in a dark app, so it is the
 * one screen that has to set this itself.
 *
 * A no-op on platforms without system bars.
 */
@Composable
expect fun PlatformSystemBarIcons(light: Boolean)
