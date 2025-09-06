package api

import json.toJsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import settings.apiUrl
import settings.prefs

object InvoiceApi {

    data class Invoice(
        val uuid: String,
        val status: String,
    )

    suspend fun getInvoice(uuid: String): Invoice {
        val url = prefs.apiUrl
            .newBuilder().apply {
                addPathSegment("v4")
                addPathSegment("invoices")
                addPathSegment(uuid)
            }.build()

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