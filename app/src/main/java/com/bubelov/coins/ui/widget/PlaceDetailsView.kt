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

package com.bubelov.coins.ui.widget

import android.content.Context
import android.graphics.Paint
import android.support.v4.content.ContextCompat
import android.text.TextUtils
import android.util.AttributeSet
import android.view.*
import android.widget.FrameLayout
import android.widget.Toast

import com.bubelov.coins.R
import com.bubelov.coins.model.Place
import com.bubelov.coins.util.openUrl

import kotlinx.android.synthetic.main.widget_place_details.view.*
import android.content.Intent

class PlaceDetailsView(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {
    internal lateinit var place: Place

    init {
        View.inflate(context, R.layout.widget_place_details, this)

        place_summary_toolbar.setNavigationOnClickListener { callback?.onDismissed() }
        place_summary_toolbar.inflateMenu(R.menu.place_details)

        place_summary_toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_share -> {
                    val subject = resources.getString(R.string.share_place_message_title)
                    val text = resources.getString(
                        R.string.share_place_message_text,
                        String.format(
                            "https://www.google.com/maps/@%s,%s,19z?hl=en",
                            place.latitude,
                            place.longitude
                        )
                    )

                    val intent = Intent(android.content.Intent.ACTION_SEND)
                    intent.type = "text/plain"
                    intent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject)
                    intent.putExtra(android.content.Intent.EXTRA_TEXT, text)
                    context.startActivity(Intent.createChooser(intent, "Share"))

                    callback?.onShared(place)
                    true
                }
                else -> false
            }
        }

        edit.setOnClickListener { callback?.onEditPlaceClick(place) }
    }

    var fullScreen: Boolean = false
        set(value) {
            field = value

            if (value) {
                map_header_shadow.visibility = View.GONE
                map_header.visibility = View.GONE
                place_summary_toolbar.visibility = View.VISIBLE
            } else {
                map_header_shadow.visibility = View.VISIBLE
                map_header.visibility = View.VISIBLE
                place_summary_toolbar.visibility = View.GONE
            }
        }

    var callback: Callback? = null

    fun setPlace(place: Place) {
        this.place = place

        if (place.openedClaims > 0 && place.closedClaims == 0) {
            check_mark.visibility = VISIBLE
        } else {
            check_mark.visibility = GONE
        }

        if (place.closedClaims > 0) {
            warning.visibility = VISIBLE
        } else {
            warning.visibility = GONE
        }

        if (place.openedClaims > 0) {
            opened_claims.visibility = VISIBLE
            opened_claims.text = resources.getQuantityString(
                R.plurals.confirmed_by_d_users,
                place.openedClaims,
                place.openedClaims
            )
        } else {
            opened_claims.visibility = GONE
        }

        if (place.closedClaims > 0) {
            closed_claims.visibility = VISIBLE
            closed_claims.text = resources.getQuantityString(
                R.plurals.marked_as_closed_by_d_users,
                place.closedClaims,
                place.closedClaims
            )
        } else {
            closed_claims.visibility = GONE
        }

        if (TextUtils.isEmpty(place.name)) {
            name.setText(R.string.name_unknown)
            place_summary_toolbar.setTitle(R.string.name_unknown)
        } else {
            name.text = place.name
            place_summary_toolbar.title = place.name
        }

        if (TextUtils.isEmpty(place.phone)) {
            phone.setText(R.string.not_provided)
        } else {
            phone.text = place.phone
        }

        if (TextUtils.isEmpty(place.website)) {
            website.setText(R.string.not_provided)
            website.setTextColor(ContextCompat.getColor(context, R.color.black))
            website.paintFlags = website!!.paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()
            website.setOnClickListener(null)
        } else {
            website.text = place.website
            website.setTextColor(ContextCompat.getColor(context, R.color.primary_dark))
            website.paintFlags = website.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            website.setOnClickListener {
                if (!context.openUrl(place.website)) {
                    Toast.makeText(context, "Can't open url: ${place.website}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        if (TextUtils.isEmpty(place.description)) {
            description.setText(R.string.not_provided)
        } else {
            description.text = place.description
        }

        if (TextUtils.isEmpty(place.openingHours)) {
            opening_hours.setText(R.string.not_provided)
        } else {
            opening_hours.text = place.openingHours
        }
    }

    interface Callback {
        fun onEditPlaceClick(place: Place)
        fun onDismissed()
        fun onShared(place: Place)
    }
}