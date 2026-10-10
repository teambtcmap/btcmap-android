package org.btcmap.api

import io.ktor.http.HttpMethod
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.btcmap.util.toJsonObject

data class PlaceBoostQuoteResponse(
    val quote30dSat: Long,
    val quote90dSat: Long,
    val quote365dSat: Long,
)

data class PlaceBoostResponse(
    val invoiceId: String,
    val invoice: String,
)

suspend fun Api.getPlaceBoostQuote(): PlaceBoostQuoteResponse {
    val url = buildUrl("v4", "place-boosts", "quote")

    return call(HttpMethod.Get, url) { body ->
        val parsed = body.toJsonObject()

        PlaceBoostQuoteResponse(
            quote30dSat = parsed.long("quote_30d_sat"),
            quote90dSat = parsed.long("quote_90d_sat"),
            quote365dSat = parsed.long("quote_365d_sat"),
        )
    }
}

suspend fun Api.boostPlace(placeId: Long, days: Long): PlaceBoostResponse {
    val url = buildUrl("v4", "place-boosts")

    val req = buildJsonObject {
        put("place_id", placeId.toString())
        put("days", days)
    }

    return call(HttpMethod.Post, url, body = req) { body ->
        val parsed = body.toJsonObject()

        PlaceBoostResponse(
            invoiceId = parsed.string("invoice_id"),
            invoice = parsed.string("invoice"),
        )
    }
}
