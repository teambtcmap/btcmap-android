package com.bubelov.coins.ui.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.bubelov.coins.R
import com.bubelov.coins.ui.model.PlacesSearchResult

import kotlinx.android.synthetic.main.list_item_places_search_result.view.*

/**
 * @author Igor Bubelov
 */

class PlacesSearchResultsAdapter(val itemClick: (PlacesSearchResult) -> Unit) : RecyclerView.Adapter<PlacesSearchResultsAdapter.ViewHolder>() {
    var items = listOf<PlacesSearchResult>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
            = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_places_search_result, parent, false), itemClick)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    class ViewHolder(view: View, val itemClick: (PlacesSearchResult) -> Unit) : RecyclerView.ViewHolder(view) {
        fun bind(item: PlacesSearchResult) {
            with(item) {
                itemView.icon.setImageResource(iconResId)

                if (distance != null) {
                    itemView.distance.visibility = View.VISIBLE
                    itemView.distance.text = itemView.resources.getString(R.string.distance_format, distance, distanceUnits)
                } else {
                    itemView.distance.visibility = View.GONE
                }

                itemView.setOnClickListener { itemClick(this) }
            }
        }
    }
}