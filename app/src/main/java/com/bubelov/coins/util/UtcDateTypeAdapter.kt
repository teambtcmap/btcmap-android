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

import com.google.gson.JsonParseException
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

import java.text.ParseException
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

class UtcDateTypeAdapter : TypeAdapter<Date>() {
    override fun write(out: JsonWriter, date: Date?) {
        if (date == null) {
            out.nullValue()
        } else {
            out.value(format(date))
        }
    }

    override fun read(`in`: JsonReader): Date? {
        return try {
            when (`in`.peek()) {
                JsonToken.NULL -> {
                    `in`.nextNull()
                    null
                }
                else -> parse(`in`.nextString())
            }
        } catch (e: ParseException) {
            throw JsonParseException(e)
        }

    }

    fun format(date: Date): String {
        val calendar = GregorianCalendar(UTC_TIME_ZONE, Locale.US)
        calendar.time = date

        val formatted = StringBuilder()

        padInt(formatted, calendar.get(Calendar.YEAR), "yyyy".length)
        formatted.append('-')
        padInt(formatted, calendar.get(Calendar.MONTH) + 1, "MM".length)
        formatted.append('-')
        padInt(formatted, calendar.get(Calendar.DAY_OF_MONTH), "dd".length)
        formatted.append('T')
        padInt(formatted, calendar.get(Calendar.HOUR_OF_DAY), "hh".length)
        formatted.append(':')
        padInt(formatted, calendar.get(Calendar.MINUTE), "mm".length)
        formatted.append(':')
        padInt(formatted, calendar.get(Calendar.SECOND), "ss".length)
        formatted.append('.')
        padInt(formatted, calendar.get(Calendar.MILLISECOND), "sss".length)
        formatted.append('Z')

        return formatted.toString()
    }

    private fun padInt(buffer: StringBuilder, value: Int, length: Int) {
        val strValue = Integer.toString(value)

        for (i in length - strValue.length downTo 1) {
            buffer.append('0')
        }

        buffer.append(strValue)
    }

    fun parse(date: String): Date {
        try {
            var offset = 0

            val year = parseInt(date, offset, offset + 4)
            offset += 4
            offset += 1

            val month = parseInt(date, offset, offset + 2)
            offset += 2
            offset += 1

            val day = parseInt(date, offset, offset + 2)
            offset += 2

            var hour = 0
            var minutes = 0
            var seconds = 0
            var milliseconds = 0

            if (checkOffset(date, offset, 'T')) {
                offset += 1
                hour = parseInt(date, offset, offset + 2)
                offset += 2
                offset += 1

                minutes = parseInt(date, offset, offset + 2)
                offset += 2
                offset += 1

                if (date.length > offset) {
                    if (date[offset] != 'Z') {
                        seconds = parseInt(date, offset, offset + 2)
                        offset += 2

                        if (checkOffset(date, offset, '.')) {
                            offset += 1
                            milliseconds = parseInt(date, offset, offset + 3)
                            offset += 3
                        }
                    }
                }
            }

            if (date.length <= offset) {
                throw IllegalArgumentException("No time zone indicator")
            }

            if (date[offset] != 'Z') {
                throw IndexOutOfBoundsException("Invalid time zone indicator")
            }

            val calendar = GregorianCalendar(UTC_TIME_ZONE)
            calendar.isLenient = false
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month - 1)
            calendar.set(Calendar.DAY_OF_MONTH, day)
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minutes)
            calendar.set(Calendar.SECOND, seconds)
            calendar.set(Calendar.MILLISECOND, milliseconds)

            return calendar.time
        } catch (e: Exception) {
            throw JsonParseException("Couldn't parse date " + date + ": " + e.message)
        }
    }

    private fun checkOffset(value: String, offset: Int, expected: Char): Boolean {
        return offset < value.length && value[offset] == expected
    }

    private fun parseInt(value: String, beginIndex: Int, endIndex: Int): Int {
        if (beginIndex < 0 || endIndex > value.length || beginIndex > endIndex) {
            throw NumberFormatException(value)
        }

        var i = beginIndex
        var result = 0
        var digit: Int

        if (i < endIndex) {
            digit = Character.digit(value[i++], 10)

            if (digit < 0) {
                throw NumberFormatException("Invalid number: " + value)
            }

            result = -digit
        }

        while (i < endIndex) {
            digit = Character.digit(value[i++], 10)

            if (digit < 0) {
                throw NumberFormatException("Invalid number: " + value)
            }

            result *= 10
            result -= digit
        }

        return -result
    }

    companion object {
        private val UTC_TIME_ZONE = TimeZone.getTimeZone("UTC")
    }
}