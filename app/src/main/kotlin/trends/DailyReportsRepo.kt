package trends

import http.await
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Single

@Single
class DailyReportsRepo {

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun getDailyReports(): List<DailyReport> {
        val url = "https://api.btcmap.org/daily_reports"
        val request = OkHttpClient().newCall(Request.Builder().url(url).build())
        val response = runCatching { request.await() }.getOrNull() ?: return emptyList()

        val reports = runCatching {
            Json
                .decodeFromStream(
                    ListSerializer(DailyReport.serializer()),
                    response.body!!.byteStream(),
                )
                .sortedBy { it.date }
                .toMutableList()
        }.getOrNull() ?: return emptyList()

        if (reports.size > 2) {
            reports.removeLast()
        }

        return reports
    }
}