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
    val url = url.newBuilder().addPathSegments("v4/place-issues").apply {
        addQueryParameter("area_id", areaId.toString())
        addQueryParameter("limit", limit.toString())
    }.build()

    return call(Request.Builder().url(url).build()) { it.toGetPlaceIssuesResponse() }
}

private fun InputStream.toGetPlaceIssuesResponse(): GetPlaceIssuesResponse {
    val body = toJsonObject()

    val issues = body.getAsJsonArray("requested_issues")?.map { element ->
        val item = element.asJsonObject
        GetPlaceIssuesItem(
            elementOsmType = item.get("element_osm_type").asString,
            elementOsmId = item.get("element_osm_id").asLong,
            elementName = item.get("element_name").asString,
            issueCode = item.get("issue_code").asString,
        )
    } ?: emptyList()

    return GetPlaceIssuesResponse(
        totalIssues = body.get("total_issues").asInt,
        requestedIssues = issues,
    )
}
