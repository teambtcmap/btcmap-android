package org.btcmap.ui.map

import org.btcmap.settings.Settings

internal const val KEY_VISIBLE_MARKER_KINDS = "visible_marker_kinds"

/**
 * The marker kinds the map draws, persisted so the user's toggles survive a
 * restart. Every kind except exchanges is visible when nothing has been stored
 * yet, and an empty stored value means the user hid them all.
 */
var Settings.visibleMarkerKinds: Set<MarkerKind>
    get() {
        val stored = getString(KEY_VISIBLE_MARKER_KINDS, null)
            ?: return DEFAULT_VISIBLE_MARKER_KINDS
        val byName = MarkerKind.entries.associateBy { it.name }
        return stored.split(',').mapNotNull { byName[it] }.toSet()
    }
    set(value) {
        putString(
            KEY_VISIBLE_MARKER_KINDS,
            MarkerKind.entries.filter { it in value }.joinToString(",") { it.name },
        )
    }
