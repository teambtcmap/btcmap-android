package reports

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneNotNull
import db.Report
import db.Database
import http.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeToSequence
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Single

@Single
class ReportsRepo(
    private val db: Database,
    private val httpClient: OkHttpClient,
    private val json: Json,
) {

    suspend fun selectByAreaId(areaId: String): List<Report> {
        return db.reportQueries
            .selectByAreaId(areaId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .first()
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun sync(): Result<SyncReport> {
        return runCatching {
            withContext(Dispatchers.IO) {
                val startMillis = System.currentTimeMillis()

                val maxUpdatedAt = db.reportQueries
                    .selectMaxUpdatedAt()
                    .asFlow()
                    .mapToOneNotNull(Dispatchers.IO)
                    .firstOrNull()
                    ?.max

                val url = HttpUrl.Builder().apply {
                    scheme("https")
                    host("api.btcmap.org")
                    addPathSegment("v2")
                    addPathSegment("reports")

                    if (!maxUpdatedAt.isNullOrBlank()) {
                        addQueryParameter("updated_since", maxUpdatedAt)
                    }
                }.build()

                val response = httpClient
                    .newCall(Request.Builder().url(url).build())
                    .await()

                if (!response.isSuccessful) {
                    throw Exception("Unexpected HTTP response code: ${response.code}")
                }

                response.body!!.byteStream().use { inputStream ->
                    val reports = json.decodeToSequence(
                        stream = inputStream,
                        deserializer = ReportJson.serializer(),
                    )

                    val createdOrUpdatedReports = reports
                        .chunked(1_000)
                        .map { db.insertOrReplace(it) }
                        .sum()

                    SyncReport(
                        timeMillis = System.currentTimeMillis() - startMillis,
                        createdOrUpdatedReports = createdOrUpdatedReports,
                    )
                }
            }
        }
    }

    private fun Database.insertOrReplace(reports: List<ReportJson>): Long {
        transaction {
            reports.forEach {
                reportQueries.insertOrReplace(
                    Report(
                        area_id = it.area_id,
                        date = it.date,
                        tags = it.tags,
                        created_at = it.created_at,
                        updated_at = it.updated_at,
                        deleted_at = it.deleted_at
                    )
                )
            }
        }

        return reports.size.toLong()
    }
}

@Serializable
private data class ReportJson(
    val area_id: String,
    val date: String,
    val tags: JsonObject,
    val created_at: String,
    val updated_at: String,
    val deleted_at: String,
)

data class SyncReport(
    val timeMillis: Long,
    val createdOrUpdatedReports: Long,
)