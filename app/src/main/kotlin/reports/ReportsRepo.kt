package reports

import http.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeToSequence
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDate
import java.time.ZonedDateTime

class ReportsRepo(
    private val queries: ReportQueries,
    private val httpClient: OkHttpClient,
    private val json: Json,
) {

    suspend fun selectByAreaId(areaId: String) = queries.selectByAreaId(areaId)

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun sync(): Result<SyncReport> {
        return runCatching {
            val startMillis = System.currentTimeMillis()

            val maxUpdatedAt = queries.selectMaxUpdatedAt()

            val url = HttpUrl.Builder().apply {
                scheme("https")
                host("api.btcmap.org")
                addPathSegment("v2")
                addPathSegment("reports")

                if (maxUpdatedAt != null) {
                    addQueryParameter("updated_since", maxUpdatedAt.toString())
                }
            }.build()

            val response = httpClient
                .newCall(Request.Builder().url(url).build())
                .await()

            if (!response.isSuccessful) {
                throw Exception("Unexpected HTTP response code: ${response.code}")
            }

            response.body!!.byteStream().use { responseBody ->
                var count = 0L

                withContext(Dispatchers.IO) {
                    json.decodeToSequence(
                        stream = responseBody,
                        deserializer = ReportJson.serializer(),
                    ).chunked(1_000).forEach { chunk ->
                        queries.insertOrReplace(chunk.map { it.toReport() })
                        count += chunk.size
                    }
                }

                SyncReport(
                    timeMillis = System.currentTimeMillis() - startMillis,
                    createdOrUpdatedReports = count,
                )
            }
        }
    }

    @Serializable
    data class ReportJson(
        val area_id: String,
        val date: String,
        val tags: JsonObject,
        val created_at: String,
        val updated_at: String,
        val deleted_at: String,
    )

    private fun ReportJson.toReport(): Report {
        return Report(
            areaId = area_id,
            date = LocalDate.parse(date),
            tags = tags,
            createdAt = ZonedDateTime.parse(created_at),
            updatedAt = ZonedDateTime.parse(updated_at),
            deletedAt = if (deleted_at.isNotEmpty()) ZonedDateTime.parse(deleted_at) else null,
        )
    }

    data class SyncReport(
        val timeMillis: Long,
        val createdOrUpdatedReports: Long,
    )
}