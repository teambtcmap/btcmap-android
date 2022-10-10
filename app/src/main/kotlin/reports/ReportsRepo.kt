package reports

import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.squareup.sqldelight.runtime.coroutines.mapToOne
import db.Report
import db.Database
import http.await
import kotlinx.coroutines.flow.first
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Single

@Single
class ReportsRepo(
    private val db: Database,
) {

    suspend fun getDailyReports(): List<Report> {
        if (db.reportQueries.selectCount().asFlow().mapToOne().first() == 0L) {
            sync()
        }

        return db.reportQueries.selectAll().asFlow().mapToList().first()
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun sync() {
        val url = "https://api.btcmap.org/v2/reports"
        val request = OkHttpClient().newCall(Request.Builder().url(url).build())
        val response = runCatching { request.await() }.getOrNull() ?: return
        val json = Json { ignoreUnknownKeys = true }

        val reports = runCatching {
            json
                .decodeFromStream(
                    ListSerializer(ReportJson.serializer()),
                    response.body!!.byteStream(),
                )
                .sortedBy { it.date }
                .toMutableList()
        }.getOrNull() ?: return

        db.transaction {
            db.reportQueries.deleteAll()

            reports.forEach {
                db.reportQueries.insertOrReplace(
                    Report(
                        area_id = "",
                        date = it.date,
                        total_elements = it.total_elements,
                        total_elements_onchain = it.total_elements_onchain,
                        total_elements_lightning = it.total_elements_lightning,
                        total_elements_lightning_contactless = it.total_elements_lightning_contactless,
                        up_to_date_elements = it.up_to_date_elements,
                        outdated_elements = it.outdated_elements,
                        legacy_elements = it.legacy_elements,
                        elements_created = it.elements_created,
                        elements_updated = it.elements_updated,
                        elements_deleted = it.elements_deleted,
                        created_at = it.created_at,
                        updated_at = it.updated_at,
                        deleted_at = it.deleted_at
                    )
                )
            }
        }
    }

    @Serializable
    private data class ReportJson(
        val area_id: String,
        val date: String,
        val total_elements: Long,
        val total_elements_onchain: Long,
        val total_elements_lightning: Long,
        val total_elements_lightning_contactless: Long,
        val up_to_date_elements: Long,
        val outdated_elements: Long,
        val legacy_elements: Long,
        val elements_created: Long,
        val elements_updated: Long,
        val elements_deleted: Long,
        val created_at: String,
        val updated_at: String,
        val deleted_at: String,
    )
}