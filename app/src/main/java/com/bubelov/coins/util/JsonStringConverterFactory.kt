/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <https://unlicense.org>
 */

package com.bubelov.coins.util

import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type
import okhttp3.RequestBody
import okio.Buffer
import java.io.IOException

class JsonStringConverterFactory(private val delegateFactory: Converter.Factory) : Converter.Factory() {
    override fun stringConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<*, String>? {
        for (annotation in annotations) {
            if (annotation is Json) {
                val delegate = delegateFactory.requestBodyConverter(
                    type,
                    annotations,
                    arrayOfNulls(0),
                    retrofit
                ) as Converter<Any, RequestBody>

                if (delegate != null) {
                    return DelegateToStringConverter<Any>(delegate)
                }
            }
        }

        return null
    }

    internal class DelegateToStringConverter<T>(private val delegate: Converter<Any, RequestBody>) :
        Converter<T, String> {

        @Throws(IOException::class)
        override fun convert(value: T): String {
            val buffer = Buffer()
            delegate.convert(value).writeTo(buffer)
            return buffer.readUtf8()
        }
    }
}