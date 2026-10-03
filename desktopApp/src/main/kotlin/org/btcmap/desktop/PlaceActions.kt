package org.btcmap.desktop

import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI
import org.btcmap.db.table.place.Place
import org.btcmap.place.btcmapUrl
import org.btcmap.place.osmEditUrl
import org.btcmap.place.osmMapUrl
import org.btcmap.place.osmUrl
import org.btcmap.ui.PlaceAction

/**
 * Handles the place sheet's actions the desktop can serve on its own: the ones
 * that open or copy a link. The rest need the app's forms or a signed-in session
 * (verify, report, boost, comment, bookmark, add photo) and are left for the
 * desktop slices that bring those screens.
 *
 * Every action is logged, so a headless run records what the sheet asked for.
 */
fun handlePlaceAction(place: Place, action: PlaceAction) {
    when (action) {
        PlaceAction.Directions -> openUrl(place.osmMapUrl())
        PlaceAction.Share -> copyToClipboard(place.btcmapUrl())
        PlaceAction.ViewOnBtcmap -> openUrl(place.btcmapUrl())
        PlaceAction.ViewOnOsm -> place.osmUrl()?.let(::openUrl)
        PlaceAction.EditOnOsm -> place.osmEditUrl()?.let(::openUrl)
        PlaceAction.Website -> place.website?.let { openUrl(it.toString()) }
        PlaceAction.Email -> place.email?.let { openUrl("mailto:$it") }
        PlaceAction.Telegram -> place.telegram?.let { openUrl(it.toString()) }
        PlaceAction.Line -> place.line?.let { openUrl(it.toString()) }
        PlaceAction.Twitter -> place.twitter?.let { openUrl(it.toString()) }
        PlaceAction.Facebook -> place.facebook?.let { openUrl(it.toString()) }
        PlaceAction.Instagram -> place.instagram?.let { openUrl(it.toString()) }
        else -> println("desktop: place action $action is not wired yet")
    }
}

internal fun openUrl(url: String) {
    println("desktop: opening $url")
    try {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(url))
            return
        }
    } catch (t: Throwable) {
        // No handler for the scheme (a `lightning:` URI with no wallet, say);
        // fall through to xdg-open.
    }
    try {
        ProcessBuilder("xdg-open", url).start()
    } catch (t: Throwable) {
        println("desktop: could not open $url: ${t.message}")
    }
}

internal fun copyToClipboard(text: String) {
    println("desktop: copying $text")
    try {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    } catch (t: Throwable) {
        println("desktop: could not copy $text: ${t.message}")
    }
}
