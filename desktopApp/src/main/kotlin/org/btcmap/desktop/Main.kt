package org.btcmap.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.btcmap.stats.StatsEntry
import org.btcmap.stats.StatsSection
import org.btcmap.ui.AppTheme
import org.btcmap.ui.StatsScreen

/**
 * The desktop entry point. The first stage renders the shared stats screen, so
 * the module and the shared UI run end to end on the JVM; navigation, the
 * database and the map follow.
 */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "BTC Map",
        state = rememberWindowState(size = DpSize(480.dp, 720.dp)),
    ) {
        AppTheme {
            StatsScreen(
                sections = listOf(
                    StatsSection(
                        key = "welcome",
                        title = "BTC Map desktop",
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
