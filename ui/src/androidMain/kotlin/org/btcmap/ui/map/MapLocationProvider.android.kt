package org.btcmap.ui.map

import androidx.compose.runtime.Composable
import org.maplibre.compose.location.LocationProvider
import org.maplibre.compose.location.rememberDefaultLocationProvider

/** Android's own location provider: the framework or fused provider. */
@Composable
internal actual fun rememberLocationProvider(): LocationProvider = rememberDefaultLocationProvider()
