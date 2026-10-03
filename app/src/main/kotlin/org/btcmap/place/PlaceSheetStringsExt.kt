package org.btcmap.place

import android.content.Context
import org.btcmap.R
import org.btcmap.ui.PlaceSheetStrings

/**
 * The Android strings for the shared place sheet and standalone place body, so
 * the map and the place screen resolve the same list in one place.
 */
fun Context.placeSheetStrings(): PlaceSheetStrings = PlaceSheetStrings(
    directions = getString(R.string.directions),
    share = getString(R.string.share),
    viewOnBtcmap = getString(R.string.view_on_btcmap),
    viewOnOsm = getString(R.string.view_on_osm),
    editOnOsm = getString(R.string.edit_on_osm),
    notVerified = getString(R.string.not_verified),
    verificationWarningTitle = getString(R.string.verification_warning_title),
    verificationWarningOutdated = getString(R.string.verification_warning_outdated),
    verificationWarningNotVerified = getString(R.string.verification_warning_not_verified),
    ok = getString(android.R.string.ok),
    companionWarning = { getString(R.string.companion_warning, it) },
    verify = getString(R.string.btn_verify),
    report = getString(R.string.btn_report),
    boost = getString(R.string.boost),
    commentsTitle = { count -> getString(R.string.comments_d, count.toInt()) },
    addComment = getString(R.string.add_comment),
    watch = getString(R.string.watch),
    unwatch = getString(R.string.unwatch),
    addPhoto = getString(R.string.add_photo),
)
