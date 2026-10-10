package org.btcmap.api

import io.ktor.http.HttpMethod
import kotlinx.serialization.json.jsonObject
import org.btcmap.util.toJsonObject

data class GetPlaceIssuesItem(
    val elementOsmType: String,
    val elementOsmId: Long,
    val elementName: String,
    val issueCode: String,
)

data class GetPlaceIssuesResponse(
    val totalIssues: Int,
    val requestedIssues: List<GetPlaceIssuesItem>,
)

suspend fun Api.getPlaceIssues(areaId: Long, limit: Long = 50): GetPlaceIssuesResponse {
    val url = buildUrl("v4", "place-issues") {
        parameters.append("area_id", areaId.toString())
        parameters.append("limit", limit.toString())
    }

    return call(HttpMethod.Get, url) { it.toGetPlaceIssuesResponse() }
}

private fun String.toGetPlaceIssuesResponse(): GetPlaceIssuesResponse {
    val body = toJsonObject()

    val issues = body.arrayOrNull("requested_issues")?.map { element ->
        val item = element.jsonObject
        GetPlaceIssuesItem(
            elementOsmType = item.string("element_osm_type"),
            elementOsmId = item.long("element_osm_id"),
            elementName = item.string("element_name"),
            issueCode = item.string("issue_code"),
        )
    } ?: emptyList()

    return GetPlaceIssuesResponse(
        totalIssues = body.int("total_issues"),
        requestedIssues = issues,
    )
}
