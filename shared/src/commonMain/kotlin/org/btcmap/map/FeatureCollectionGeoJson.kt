package org.btcmap.map

private const val FEATURES_PREFIX = "\"features\":["

/**
 * Merges [FeatureCollection] JSON strings into one collection, preserving their
 * order. The map draws every visible object kind from one clustered source, so a
 * cluster can span merchants, events, exchanges and notes instead of only one
 * kind.
 *
 * The inputs are the strings the app's own `to*GeoJson` functions build, which
 * share one fixed envelope; an empty or unrecognised collection contributes no
 * features.
 */
fun mergeFeatureCollections(collections: Iterable<String>): String {
    val features = collections.mapNotNull { json ->
        if (json == EMPTY_GEOJSON) return@mapNotNull null
        val start = json.indexOf(FEATURES_PREFIX)
        val end = json.lastIndexOf(']')
        if (start < 0 || end <= start) return@mapNotNull null
        json.substring(start + FEATURES_PREFIX.length, end).takeIf { it.isNotEmpty() }
    }
    if (features.isEmpty()) return EMPTY_GEOJSON
    return buildString {
        append("{\"type\":\"FeatureCollection\",\"features\":[")
        features.forEachIndexed { index, feature ->
            if (index > 0) append(',')
            append(feature)
        }
        append("]}")
    }
}
