package org.btcmap.api

import okhttp3.Request
import org.btcmap.util.toJsonObject
import java.io.InputStream

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
    val url = pathBuilder("v4", "place-issues").apply {
        addQueryParameter("area_id", areaId.toString())
        addQueryParameter("limit", limit.toString())
    }.build()

    return call(Request.Builder().url(url).build()) { it.toGetPlaceIssuesResponse() }
}

private fun InputStream.toGetPlaceIssuesResponse(): GetPlaceIssuesResponse {
    val body = toJsonObject()

    val issues = body.arrayOrNull("requested_issues")?.map { element ->
        val item = element.asJsonObject
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
