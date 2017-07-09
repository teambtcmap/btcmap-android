package com.bubelov.coins.ui.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.bubelov.coins.R
import com.bubelov.coins.ui.model.ExchangeRateQuery

import java.util.Locale

import kotlinx.android.synthetic.main.list_item_exchange_rate.view.*

/**
 * @author Igor Bubelov
 */

class ExchangeRatesAdapter : RecyclerView.Adapter<ExchangeRatesAdapter.ViewHolder>() {
    var items: List<ExchangeRateQuery> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_exchange_rate, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: ExchangeRateQuery) {
            when(item) {
                is ExchangeRateQuery.Loading -> {
                    itemView.first_letter.text = item.source.substring(0, 1)
                    itemView.exchange_name.text = item.source
                    itemView.price.text = "fetching..."
                }
                is ExchangeRateQuery.ExchangeRate -> {
                    itemView.first_letter.text = item.source.substring(0, 1)
                    itemView.exchange_name.text = item.source
                    itemView.price.text = String.format(Locale.US, "$%.2f", item.rate)
                }
                is ExchangeRateQuery.Error -> {
                    itemView.first_letter.text = item.source.substring(0, 1)
                    itemView.exchange_name.text = item.source
                    itemView.price.text = "error"
                }
            }
        }
    }
}