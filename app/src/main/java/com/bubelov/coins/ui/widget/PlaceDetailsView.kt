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
import com.bubelov.coins.util.Analytics
import com.bubelov.coins.util.openUrl

import kotlinx.android.synthetic.main.widget_place_details.view.*
import org.jetbrains.anko.share

/**
 * @author Igor Bubelov
 */

class PlaceDetailsView(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {
    init {
        View.inflate(context, R.layout.widget_place_details, this)

        toolbar.setNavigationOnClickListener { callback?.onDismissed() }
        toolbar.inflateMenu(R.menu.place_details)

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_share -> {
                    context.share(resources.getString(R.string.share_place_message_text, String.format("https://www.google.com/maps/@%s,%s,19z?hl=en", place!!.latitude, place!!.longitude)), resources.getString(R.string.share_place_message_title))
                    Analytics.logShareContentEvent(place!!.id.toString(), place!!.name, "place")
                    true
                } else -> false
            }
        }

        edit.setOnClickListener { callback?.onEditPlaceClick(place!!) }
    }

    internal var place: Place? = null

    var fullScreen: Boolean = false
        set(value) {
            field = value

            if (value) {
                header_switcher.displayedChild = 1
            } else {
                header_switcher.displayedChild = 0
            }
        }

    val headerHeight: Int
        get() = header_switcher.height

    var callback: Callback? = null

    fun setPlace(place: Place) {
        this.place = place

        if (TextUtils.isEmpty(place.name)) {
            name.setText(R.string.name_unknown)
            toolbar.setTitle(R.string.name_unknown)
        } else {
            name.text = place.name
            toolbar.title = place.name
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
            website.setOnClickListener { context.openUrl(place.website) }
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
    }
}