package org.btcmap.api

import io.ktor.http.URLBuilder
import kotlin.time.Instant

/**
 * Adds the `updated_since` cursor shared by the incremental list endpoints. A
 * null [value] is omitted so callers that seed the first sync (which have no
 * cursor yet) build their URL the same way as callers that always send one.
 */
internal fun URLBuilder.addUpdatedSince(value: Instant?) {
    if (value == null) return

    parameters.append("updated_since", value.toString())
}
