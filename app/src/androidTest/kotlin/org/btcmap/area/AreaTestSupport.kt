package org.btcmap.area

import android.os.Bundle
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.RecordedRequest
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.btcmap.Activity
import org.btcmap.App
import org.btcmap.R
import org.btcmap.db.table.area.Area
import org.btcmap.db.table.event.Event
import org.btcmap.util.AppTestCase
import org.junit.Rule
import java.time.ZonedDateTime

internal const val AREA_TAG = "area"

internal const val OFFLINE_STYLE_URI = "asset://map-styles/test/style.json"

internal const val EMPTY_ISSUES_JSON = """{"total_issues":0,"requested_issues":[]}"""

abstract class AreaScreenTest : AppTestCase() {

    protected val app = ApplicationProvider.getApplicationContext<App>()

    protected fun withArea(
        areaId: Long = 1,
        addToBackStack: Boolean = false,
        block: (ActivityScenario<Activity>, AreaFragment) -> Unit,
    ) {
        app.mapStyleUriForTesting = OFFLINE_STYLE_URI
        try {
            ActivityScenario.launch(Activity::class.java).use { scenario ->
                lateinit var fragment: AreaFragment
                scenario.onActivity { activity ->
                    fragment = AreaFragment().apply {
                        arguments = Bundle().apply { putLong(ARG_AREA_ID, areaId) }
                    }
                    activity.supportFragmentManager.commit {
                        setReorderingAllowed(true)
                        replace(R.id.fragmentContainerView, fragment, AREA_TAG)
                        if (addToBackStack) addToBackStack(null)
                    }
                    activity.supportFragmentManager.executePendingTransactions()
                }
                block(scenario, fragment)
            }
        } finally {
            app.mapStyleUriForTesting = null
        }
    }
}

internal fun areaDispatcher(
    issuesBody: String = EMPTY_ISSUES_JSON,
    issuesCode: Int = 200,
): Dispatcher {
    return object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            val path = request.url.encodedPath
            return when {
                path.startsWith("/v4/place-issues") -> jsonResponse(issuesBody, issuesCode)
                else -> jsonResponse("[]")
            }
        }
    }
}

internal fun jsonResponse(body: String, code: Int = 200): MockResponse {
    return MockResponse.Builder()
        .code(code)
        .addHeader("Content-Type", "application/json")
        .body(body)
        .build()
}

// The area's GeoJSON is a square around the Paris coordinates [event] uses by
// default, so a seeded event is linked to the seeded area the same way the
// server links them (bbox pre-filter plus point-in-polygon).
private const val AREA_BBOX_WEST = 2.0
private const val AREA_BBOX_SOUTH = 48.0
private const val AREA_BBOX_EAST = 3.0
private const val AREA_BBOX_NORTH = 49.0

internal fun areaPolygon(): String {
    return """
        {
            "type": "Polygon",
            "coordinates": [[
                [$AREA_BBOX_WEST, $AREA_BBOX_SOUTH],
                [$AREA_BBOX_EAST, $AREA_BBOX_SOUTH],
                [$AREA_BBOX_EAST, $AREA_BBOX_NORTH],
                [$AREA_BBOX_WEST, $AREA_BBOX_NORTH],
                [$AREA_BBOX_WEST, $AREA_BBOX_SOUTH]
            ]]
        }
    """.trimIndent()
}

internal fun area(
    id: Long = 1,
    name: String = "Grand Paris",
    description: String? = null,
    icon: String? = null,
    iconWide: String? = null,
    websiteUrl: String = "https://btcmap.org/community/grand-paris",
    bboxWest: Double? = AREA_BBOX_WEST,
    bboxSouth: Double? = AREA_BBOX_SOUTH,
    bboxEast: Double? = AREA_BBOX_EAST,
    bboxNorth: Double? = AREA_BBOX_NORTH,
    geoJson: String? = areaPolygon(),
): Area {
    return Area(
        id = id,
        name = name,
        type = "community",
        urlAlias = "grand-paris",
        icon = icon,
        iconWide = iconWide,
        websiteUrl = websiteUrl,
        description = description,
        bboxWest = bboxWest,
        bboxSouth = bboxSouth,
        bboxEast = bboxEast,
        bboxNorth = bboxNorth,
        geoJson = geoJson,
        updatedAt = ZonedDateTime.parse("2024-01-01T00:00:00Z"),
    )
}

internal fun event(
    id: Long,
    name: String,
    startsAt: String,
    lat: Double = 48.8566,
    lon: Double = 2.3522,
    endsAt: String? = null,
): Event {
    return Event(
        id = id,
        areaId = null,
        lat = lat,
        lon = lon,
        name = name,
        website = "https://example.com/events/$id".toHttpUrlOrNull(),
        startsAt = ZonedDateTime.parse(startsAt),
        endsAt = endsAt?.let { ZonedDateTime.parse(it) },
        updatedAt = ZonedDateTime.parse("2024-01-01T00:00:00Z"),
    )
}

internal fun issuesJson(total: Int, vararg issues: String): String {
    return """
        {
            "total_issues": $total,
            "requested_issues": [${issues.joinToString(",")}]
        }
    """.trimIndent()
}

internal fun issueJson(
    osmType: String,
    osmId: Long,
    name: String,
    code: String,
): String {
    return """
        {
            "element_osm_type": "$osmType",
            "element_osm_id": $osmId,
            "element_name": "$name",
            "issue_code": "$code"
        }
    """.trimIndent()
}
