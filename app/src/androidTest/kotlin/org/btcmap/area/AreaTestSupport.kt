package org.btcmap.area

import android.os.Bundle
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.RecordedRequest
import org.btcmap.Activity
import org.btcmap.App
import org.btcmap.R
import org.btcmap.util.ApiRule
import org.btcmap.util.DatabaseRule
import org.btcmap.util.PreferencesRule
import org.junit.Rule

internal const val AREA_TAG = "area"

internal const val OFFLINE_STYLE_URI = "asset://map-styles/test/style.json"

internal const val EMPTY_EVENTS_JSON = "[]"

internal const val EMPTY_ISSUES_JSON = """{"total_issues":0,"requested_issues":[]}"""

abstract class AreaScreenTest {

    @JvmField
    @Rule
    val databaseRule = DatabaseRule()

    @JvmField
    @Rule
    val apiRule = ApiRule()

    @JvmField
    @Rule
    val preferencesRule = PreferencesRule()

    protected val app = ApplicationProvider.getApplicationContext<App>()

    protected fun withArea(
        areaId: String = "1",
        addToBackStack: Boolean = false,
        block: (ActivityScenario<Activity>, AreaFragment) -> Unit,
    ) {
        app.mapStyleUriForTesting = OFFLINE_STYLE_URI
        try {
            ActivityScenario.launch(Activity::class.java).use { scenario ->
                lateinit var fragment: AreaFragment
                scenario.onActivity { activity ->
                    fragment = AreaFragment().apply {
                        arguments = Bundle().apply { putString(ARG_AREA_ID, areaId) }
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
    areaBody: String = areaJson(),
    eventsBody: String = EMPTY_EVENTS_JSON,
    issuesBody: String = EMPTY_ISSUES_JSON,
    areaCode: Int = 200,
    eventsCode: Int = 200,
    issuesCode: Int = 200,
): Dispatcher {
    return object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            val path = request.url.encodedPath
            return when {
                path.endsWith("/events") -> jsonResponse(eventsBody, eventsCode)
                path.startsWith("/v4/place-issues") -> jsonResponse(issuesBody, issuesCode)
                path.startsWith("/v4/areas") -> jsonResponse(areaBody, areaCode)
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

internal fun areaJson(
    id: Long = 1,
    name: String = "Grand Paris",
    icon: String? = null,
    iconWide: String? = null,
    description: String? = null,
): String {
    return """
        {
            "id": $id,
            "name": "$name",
            "type": "community",
            "url_alias": "grand-paris",
            "icon": ${jsonStringOrNull(icon)},
            "icon_wide": ${jsonStringOrNull(iconWide)},
            "website_url": "https://btcmap.org/community/grand-paris",
            "description": ${jsonStringOrNull(description)}
        }
    """.trimIndent()
}

internal fun eventsJson(vararg events: String): String {
    return events.joinToString(prefix = "[", postfix = "]", separator = ",")
}

internal fun eventJson(
    id: Long,
    name: String,
    startsAt: String,
    endsAt: String? = null,
): String {
    return """
        {
            "id": $id,
            "area_id": 1,
            "lat": 48.8566,
            "lon": 2.3522,
            "name": "$name",
            "website": "https://example.com/events/$id",
            "starts_at": "$startsAt",
            "ends_at": ${jsonStringOrNull(endsAt)}
        }
    """.trimIndent()
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

private fun jsonStringOrNull(value: String?): String {
    return if (value == null) "null" else "\"${value.replace("\"", "\\\"")}\""
}
