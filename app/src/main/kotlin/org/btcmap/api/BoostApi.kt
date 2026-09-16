package org.btcmap.api

import com.google.gson.JsonObject
import okhttp3.Request
import org.btcmap.util.toJsonObject

data class PlaceBoostQuoteResponse(
    val quote30dsat: Long,
    val quote90dsat: Long,
    val quote365dsat: Long,
)

data class PlaceBoostResponse(
    val invoiceId: String,
    val invoice: String,
)

suspend fun Api.getPlaceBoostQuote(): PlaceBoostQuoteResponse {
    val url = buildUrl("v4", "place-boosts", "quote")

    return call(Request.Builder().url(url).build()) { stream ->
        val body = stream.toJsonObject()

        PlaceBoostQuoteResponse(
            quote30dsat = body.long("quote_30d_sat"),
            quote90dsat = body.long("quote_90d_sat"),
            quote365dsat = body.long("quote_365d_sat"),
        )
    }
}

suspend fun Api.boostPlace(placeId: Long, days: Long): PlaceBoostResponse {
    val url = buildUrl("v4", "place-boosts")

    val req = JsonObject().apply {
        addProperty("place_id", placeId.toString())
        addProperty("days", days)
    }

    return call(
        Request.Builder()
            .post(jsonBody(req))
            .url(url)
            .build()
    ) { stream ->
        val body = stream.toJsonObject()

        PlaceBoostResponse(
            invoiceId = body.string("invoice_id"),
            invoice = body.string("invoice"),
        )
    }
}
