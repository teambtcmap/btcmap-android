package org.btcmap.http

import okhttp3.Interceptor
import okhttp3.Response

object RateLimitingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var res = chain.proceed(chain.request())

        var retryAttempts = 0

        while (res.code == 429 && retryAttempts < 10) {
            res.close()
            Thread.sleep(retryAttempts * 1000 + (Math.random() * 1000.0).toLong())
            res = chain.proceed(chain.request())
            retryAttempts++
        }

        return res
    }
}