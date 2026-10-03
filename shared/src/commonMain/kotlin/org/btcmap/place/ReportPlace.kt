package org.btcmap.place

import org.btcmap.api.Api
import org.btcmap.api.ReportPlaceResponse
import org.btcmap.api.reportPlace

/**
 * The reason a place is being reported, with the value the API expects.
 *
 * This is the ordered set of reasons every host offers; the labels and
 * descriptions stay with the host, because Android resolves them from its
 * string resources.
 */
enum class ReportType(val value: String) {
    Verified("verified"),
    RefusedSats("refused_sats"),
    OutOfBusiness("out_of_business"),
    ;

    companion object {
        /** The type whose [value] is [value], or null when it is not one of them. */
        fun fromValue(value: String?): ReportType? = entries.firstOrNull { it.value == value }
    }
}

/** Mirrors the API's per-report evidence photo cap so the UI can enforce it. */
const val MAX_REPORT_PHOTOS = 5

/**
 * The evidence a place report carries. [note] is trimmed and dropped when blank;
 * [photos] must already be encoded and at most [MAX_REPORT_PHOTOS] of them.
 */
class ReportDraft(
    val type: ReportType,
    val note: String = "",
    val photos: List<ByteArray> = emptyList(),
) {
    /** The note as the API wants it: trimmed, or null when it is empty. */
    val comment: String? get() = note.trim().takeIf { it.isNotEmpty() }
}

/** Submits [draft] as a report for [placeId]. */
suspend fun Api.submitReport(placeId: Long, draft: ReportDraft): ReportPlaceResponse =
    reportPlace(
        placeId = placeId,
        type = draft.type.value,
        comment = draft.comment,
        photos = draft.photos,
    )
