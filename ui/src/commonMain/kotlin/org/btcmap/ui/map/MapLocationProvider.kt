package org.btcmap.ui.map

import androidx.compose.runtime.Composable
import org.maplibre.compose.location.LocationProvider

/**
 * The platform's own location provider, wrapped where the platform needs it.
 *
 * Android and the non-Linux desktops use the default provider as-is. Linux
 * desktop wraps it so the location button works around the XDG portal's
 * permission probe (see the JVM actual).
 */
@Composable
internal expect fun rememberLocationProvider(): LocationProvider
