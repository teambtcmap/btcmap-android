package org.btcmap.auth

import okhttp3.Request

class PublicRequest

fun Request.Builder.withoutAuth(): Request.Builder = apply {
    tag(PublicRequest::class.java, PublicRequest())
}
