package org.btcmap.ui

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.btcmap.stats.StatsEntry
import org.btcmap.stats.StatsSection

/**
 * The Compose Multiplatform desktop entry point.
 *
 * It renders the shared [StatsScreen] with sample data, so the shared UI can be
 * run and iterated on without an Android device while the desktop app is being
 * built out.
 */
fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "BTC Map") {
        AppTheme {
            StatsScreen(
                sections = listOf(
                    StatsSection(
                        key = "welcome",
                        title = "Compose Multiplatform",
                        entries = listOf(
                            StatsEntry("Rendering", "the shared :ui module"),
                            StatsEntry("Targets", "Android and desktop"),
                        ),
                    ),
                ),
            )
        }
    }
}
