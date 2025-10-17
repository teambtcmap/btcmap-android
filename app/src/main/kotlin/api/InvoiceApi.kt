package api

import http.httpClient
import json.toJsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import settings.apiUrlV4
import settings.prefs

object InvoiceApi {
    private const val ENDPOINT = "invoices"

    data class Invoice(
        val id: String,
        val status: String,
    )

    val Invoice.paid: Boolean
        get() = status == "paid"

    suspend fun getInvoice(id: String): Invoice {
        val url = prefs.apiUrlV4(ENDPOINT, id)
        val res = httpClient.newCall(Request.Builder().url(url).build()).executeAsync()

        if (!res.isSuccessful) {
            throw Exception("Unexpected HTTP response code: ${res.code}")
        }

        return withContext(Dispatchers.IO) {
            res.body.byteStream().use {
                val body = it.toJsonObject()

                Invoice(
                    id = body.getString("id"),
                    status = body.getString("status"),
                )
            }
        }
    }
}