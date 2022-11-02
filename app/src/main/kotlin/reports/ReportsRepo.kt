package reports

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import db.Report
import db.Database
import http.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromStream
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Single

@Single
class ReportsRepo(
    private val db: Database,
) {

    suspend fun selectByAreaId(areaId: String): List<Report> {
        return db.reportQueries.selectByAreaId(areaId).asFlow().mapToList(Dispatchers.IO).first()
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun sync() {
        val url = "https://api.btcmap.org/v2/reports"
        val request = OkHttpClient().newCall(Request.Builder().url(url).build())
        val response = runCatching { request.await() }.getOrNull() ?: return
        val json = Json { ignoreUnknownKeys = true }

        val reports = runCatching {
            withContext(Dispatchers.IO) {
                json.decodeFromStream(
                    ListSerializer(ReportJson.serializer()),
                    response.body!!.byteStream(),
                )
            }
        }.getOrNull() ?: return

        withContext(Dispatchers.IO) {
            db.transaction {
                db.reportQueries.deleteAll()

                reports.forEach {
                    db.reportQueries.insertOrReplace(
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
}