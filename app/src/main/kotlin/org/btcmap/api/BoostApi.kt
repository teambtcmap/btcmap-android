package org.btcmap.api

import com.google.gson.JsonObject
import okhttp3.Request
import org.btcmap.auth.withoutAuth
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

    return call(Request.Builder().withoutAuth().url(url).build()) { stream ->
        val body = stream.toJsonObject()

        PlaceBoostQuoteResponse(
            quote30dSat = body.long("quote_30d_sat"),
            quote90dSat = body.long("quote_90d_sat"),
            quote365dSat = body.long("quote_365d_sat"),
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
            .withoutAuth()
            .build()
    ) { stream ->
        val body = stream.toJsonObject()

        PlaceBoostResponse(
            invoiceId = body.string("invoice_id"),
            invoice = body.string("invoice"),
        )
    }
}
