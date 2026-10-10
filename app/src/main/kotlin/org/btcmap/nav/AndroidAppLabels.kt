package org.btcmap.nav

import android.content.Context
import android.text.format.DateFormat
import android.text.format.Formatter
import org.btcmap.i18n.Strings
import org.btcmap.settings.mapStyle
import org.btcmap.settings.prefs
import org.btcmap.ui.AppLabels
import org.btcmap.ui.appLabels
import org.btcmap.ui.defaultFormatNumber
import java.time.Instant
import java.util.Date

/**
 * Builds the shared [AppLabels] from the cross-platform string catalog, so the
 * Android app and the desktop render the same text. Only formatting (byte
 * sizes, distances, dates) stays here, because the platforms format differently.
 */
internal fun Context.androidAppLabels(): AppLabels = appLabels(
    strings = Strings.current(),
    currentStyle = prefs.mapStyle,
    formatNumber = ::defaultFormatNumber,
    formatBytes = { Formatter.formatFileSize(this, it) },
    formatFeedDate = { iso ->
        try {
            DateFormat.getMediumDateFormat(this).format(Date.from(Instant.parse(iso)))
        } catch (_: Exception) {
            iso
        }
    },
)
