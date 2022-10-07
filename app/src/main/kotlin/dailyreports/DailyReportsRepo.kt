package dailyreports

import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.squareup.sqldelight.runtime.coroutines.mapToOne
import db.Daily_report
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
class DailyReportsRepo(
    private val db: Database,
) {
    suspend fun getDailyReports(): List<Daily_report> {
        if (db.daily_reportQueries.selectCount().asFlow().mapToOne().first() == 0L) {
            sync()
        }

        return db.daily_reportQueries.selectAll().asFlow().mapToList().first()
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun sync() {
        val url = "https://api.btcmap.org/daily_reports"
        val request = OkHttpClient().newCall(Request.Builder().url(url).build())
        val response = runCatching { request.await() }.getOrNull() ?: return
        val json = Json { ignoreUnknownKeys = true }

        val reports = runCatching {
            json
                .decodeFromStream(
                    ListSerializer(DailyReportJson.serializer()),
                    response.body!!.byteStream(),
                )
                .sortedBy { it.date }
                .toMutableList()
        }.getOrNull() ?: return

        db.transaction {
            db.daily_reportQueries.deleteAll()

            reports.forEach {
                db.daily_reportQueries.insertOrReplace(
                    Daily_report(
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
                    )
                )
            }
        }
    }

    @Serializable
    private data class DailyReportJson(
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
    )
}