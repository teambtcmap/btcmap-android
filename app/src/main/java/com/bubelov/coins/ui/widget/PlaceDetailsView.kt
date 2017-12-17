package com.bubelov.coins.ui.widget

import android.content.Context
import android.graphics.Paint
import android.support.v4.content.ContextCompat
import android.text.TextUtils
import android.util.AttributeSet
import android.view.*
import android.widget.FrameLayout

import com.bubelov.coins.R
import com.bubelov.coins.model.Place
import com.bubelov.coins.util.openUrl

import kotlinx.android.synthetic.main.widget_place_details.view.*
import org.jetbrains.anko.share
import org.jetbrains.anko.toast

/**
 * @author Igor Bubelov
 */

class PlaceDetailsView(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {
    internal lateinit var place: Place

    init {
        View.inflate(context, R.layout.widget_place_details, this)

        place_summary_toolbar.setNavigationOnClickListener { callback?.onDismissed() }
        place_summary_toolbar.inflateMenu(R.menu.place_details)

        place_summary_toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_share -> {
                    context.share(resources.getString(R.string.share_place_message_text, String.format("https://www.google.com/maps/@%s,%s,19z?hl=en", place.latitude, place.longitude)), resources.getString(R.string.share_place_message_title))
                    callback?.onShared(place)
                    true
                } else -> false
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
            opened_claims.text = resources.getQuantityString(R.plurals.confirmed_by_d_users, place.openedClaims, place.openedClaims)
        } else {
            opened_claims.visibility = GONE
        }

        if (place.closedClaims > 0) {
            closed_claims.visibility = VISIBLE
            closed_claims.text = resources.getQuantityString(R.plurals.marked_as_closed_by_d_users, place.closedClaims, place.closedClaims)
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
                    context.toast("Can't open url: ${place.website}")
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