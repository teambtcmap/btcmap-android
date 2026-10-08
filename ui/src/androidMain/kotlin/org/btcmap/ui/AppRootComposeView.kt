package org.btcmap.ui

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView
import org.btcmap.db.table.event.Event

/**
 * Hosts the shared [AppRoot] inside the Android Views hierarchy: the app sets
 * the services, the platform actions, the labels and the start route, and the
 * root owns the navigation from there.
 *
 * A host embeds one of these and lets the root run the screens; it is the single
 * Compose entry point that replaces the per-screen `*ComposeView` hosts and the
 * fragment back stack.
 */
class AppRootComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {

    var services: AppServices? by mutableStateOf(null)

    var platform: AppPlatform? by mutableStateOf(null)

    var labels: AppLabels? by mutableStateOf(null)

    /** Called when a screen changes the UI language, so the host can rebuild [labels]. */
    var onLanguageChanged: () -> Unit = {}

    /** The route the root opens first. Set before the view is attached. */
    var startRoute: AppRoute = AppRoute.Map

    /** Called when the root is back at [startRoute] and the user goes back again. */
    var onExit: () -> Unit = {}

    /** A place a deep link wants the map to open. */
    var openPlaceId: Long? by mutableStateOf(null)

    /** An event a deep link wants the root to open. */
    var pendingEvent: Event? by mutableStateOf(null)

    private var navBack: (() -> Boolean)? = null

    /** Pops the root's stack for a system back press; false when it is empty. */
    fun back(): Boolean = navBack?.invoke() ?: false

    @Composable
    override fun Content() {
        val services = this.services ?: return
        val platform = this.platform ?: return
        val labels = this.labels ?: return

        AppRoot(
            services = services,
            platform = platform,
            labels = labels,
            startRoute = startRoute,
            onExit = onExit,
            openPlaceId = openPlaceId,
            onOpenPlaceConsumed = { openPlaceId = null },
            pendingEvent = pendingEvent,
            onEventConsumed = { pendingEvent = null },
            registerBack = { navBack = it },
            onLanguageChanged = onLanguageChanged,
        )
    }
}
