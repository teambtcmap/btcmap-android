package com.bubelov.coins.ui.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import com.bubelov.coins.R
import com.bubelov.coins.ui.model.PlacesSearchResult

/**
 * @author Igor Bubelov
 */

class PlacesSearchResultsAdapter(var items: List<PlacesSearchResult>, val callback: PlacesSearchResultsAdapter.Callback) : RecyclerView.Adapter<PlacesSearchResultsAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_places_search_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.icon.setImageResource(item.iconResId)
        holder.name.text = item.placeName

        if (item.distance != null) {
            holder.distance.visibility = View.VISIBLE
            val resources = holder.itemView.resources
            holder.distance.text = resources.getString(R.string.distance_format, item.distance, item.distanceUnits)
        } else {
            holder.distance.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { callback.onClick(item) }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var icon: ImageView = itemView.findViewById(R.id.icon) as ImageView
        var name: TextView = itemView.findViewById(R.id.name) as TextView
        var distance: TextView = itemView.findViewById(R.id.distance) as TextView
    }

    interface Callback {
        fun onClick(item: PlacesSearchResult)
    }
}