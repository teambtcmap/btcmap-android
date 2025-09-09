package api

import json.toJsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.coroutines.executeAsync

object InvoiceApi {

    private const val ENDPOINT = "invoices"

    data class Invoice(
        val uuid: String,
        val status: String,
    )

    val Invoice.paid: Boolean
        get() = status == "paid"

    suspend fun getInvoice(uuid: String): Invoice {
        val url = apiUrl(ENDPOINT).addPathSegment(uuid).build()

        val res = apiHttpClient().newCall(Request.Builder().url(url).build()).executeAsync()

        if (!res.isSuccessful) {
            throw Exception("Unexpected HTTP response code: ${res.code}")
        }

        return withContext(Dispatchers.IO) {
            res.body.byteStream().use {
                val body = it.toJsonObject()

                Invoice(
                    uuid = body.getString("uuid"),
                    status = body.getString("status"),
                )
            }
        }
    }
}