package org.btcmap.webspike

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.Buffer

/**
 * The shapes the app parses, exercising the kotlinx.serialization replacement
 * for Gson's typed mapper.
 */
@Serializable
data class SpikePlace(
    val id: Long,
    val name: String,
    @SerialName("updated_at") val updatedAt: String,
)

private val json = Json { ignoreUnknownKeys = true }

/** Typed JSON, replacing `Gson().fromJson`. */
fun parsePlace(body: String): SpikePlace = json.decodeFromString(body)

/** JSON tree, replacing Gson's `JsonParser`/`JsonObject`. */
fun placeName(body: String): String? =
    Json.parseToJsonElement(body).jsonObject["name"]?.jsonPrimitive?.contentOrNull

/** URL building, replacing OkHttp's `HttpUrl.Builder`. */
fun placesUrl(base: String): String =
    URLBuilder(base).apply { appendPathSegments("v4", "places") }.build().toString()

/** Date/time, replacing `java.time`. */
fun epochSeconds(iso: String): Long =
    LocalDateTime.parse(iso).toInstant(TimeZone.of("UTC")).epochSeconds

/** Base64, replacing `java.util.Base64`. */
@OptIn(ExperimentalEncodingApi::class)
fun encode(text: String): String = Base64.encode(text.encodeToByteArray())

/** Buffers, replacing `java.io` streams. */
fun roundTrip(text: String): String = Buffer().apply { writeUtf8(text) }.readUtf8()

/** HTTP, replacing OkHttp's client. */
suspend fun fetch(client: HttpClient, url: String): String = client.get(url).bodyAsText()
