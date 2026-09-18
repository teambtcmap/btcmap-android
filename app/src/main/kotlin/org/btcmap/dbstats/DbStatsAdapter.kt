package org.btcmap.dbstats

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.btcmap.databinding.DbStatsEntryItemBinding
import org.btcmap.databinding.DbStatsHeaderItemBinding

class DbStatsAdapter : ListAdapter<DbStatsItem, RecyclerView.ViewHolder>(DiffCallback()) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DbStatsItem.Header -> TYPE_HEADER
            is DbStatsItem.Entry -> TYPE_ENTRY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER ->
                HeaderViewHolder(DbStatsHeaderItemBinding.inflate(inflater, parent, false))

            else ->
                EntryViewHolder(DbStatsEntryItemBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is DbStatsItem.Header -> (holder as HeaderViewHolder).bind(item)
            is DbStatsItem.Entry -> (holder as EntryViewHolder).bind(item)
        }
    }

    private class HeaderViewHolder(
        private val binding: DbStatsHeaderItemBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DbStatsItem.Header) {
            binding.title.text = item.title
        }
    }

    private class EntryViewHolder(
        private val binding: DbStatsEntryItemBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DbStatsItem.Entry) {
            binding.title.text = item.title
            binding.detail.text = item.detail
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<DbStatsItem>() {
        override fun areItemsTheSame(oldItem: DbStatsItem, newItem: DbStatsItem): Boolean {
            return oldItem.key == newItem.key
        }

        override fun areContentsTheSame(oldItem: DbStatsItem, newItem: DbStatsItem): Boolean {
            return oldItem == newItem
        }
    }

    private companion object {
        const val TYPE_HEADER = 0
        const val TYPE_ENTRY = 1
    }
}
