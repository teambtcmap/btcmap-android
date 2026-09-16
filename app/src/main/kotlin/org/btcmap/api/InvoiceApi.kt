package org.btcmap.api

import okhttp3.Request
import org.btcmap.util.toJsonObject

data class Invoice(
    val id: String,
    val status: String,
)

val Invoice.paid: Boolean
    get() = status == "paid"

suspend fun Api.getInvoice(id: String): Invoice {
    val url = url.newBuilder().addPathSegments("v4/invoices/$id").build()

    return call(Request.Builder().url(url).build()) { stream ->
        val body = stream.toJsonObject()

        Invoice(
            id = body.string("id"),
            status = body.string("status"),
        )
    }
}
