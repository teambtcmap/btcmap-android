package com.bubelov.coins.ui.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.bubelov.coins.R
import com.bubelov.coins.ui.model.PlacesSearchResult

import kotlinx.android.synthetic.main.list_item_places_search_result.view.*
import java.text.NumberFormat

/**
 * @author Igor Bubelov
 */

class PlacesSearchResultsAdapter(private val itemClick: (PlacesSearchResult) -> Unit) : RecyclerView.Adapter<PlacesSearchResultsAdapter.ViewHolder>() {
    var items = listOf<PlacesSearchResult>()

    private val distanceFormat = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
            = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_places_search_result, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        with(holder.itemView) {
            icon.setImageResource(item.iconResId)
            name.text = item.placeName

            if (item.distance != null) {
                distance.visibility = android.view.View.VISIBLE
                distance.text = resources.getString(R.string.distance_format, distanceFormat.format(item.distance), item.distanceUnits)
            } else {
                distance.visibility = android.view.View.GONE
            }

            setOnClickListener { itemClick(item) }
        }
    }

    override fun getItemCount() = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
}