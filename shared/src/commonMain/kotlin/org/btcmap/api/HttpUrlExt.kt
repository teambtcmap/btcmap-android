package org.btcmap.api

import okhttp3.HttpUrl
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Adds the `updated_since` cursor shared by the incremental list endpoints. A
 * null [value] is omitted so callers that seed the first sync (which have no
 * cursor yet) build their URL the same way as callers that always send one.
 */
internal fun HttpUrl.Builder.addUpdatedSince(value: ZonedDateTime?) {
    if (value == null) return

    addQueryParameter("updated_since", value.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
}
