package org.btcmap.auth

import okhttp3.Request

object PublicRequest

fun Request.Builder.withoutAuth(): Request.Builder = apply {
    tag(PublicRequest::class.java, PublicRequest)
}
