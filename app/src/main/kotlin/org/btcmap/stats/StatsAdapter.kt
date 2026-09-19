package org.btcmap.stats

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.btcmap.databinding.StatsRowItemBinding
import org.btcmap.databinding.StatsSectionItemBinding
import org.btcmap.util.iconTypeface

class StatsAdapter : ListAdapter<StatsSection, StatsAdapter.SectionViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        val binding = StatsSectionItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return SectionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SectionViewHolder(
        private val binding: StatsSectionItemBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        private val inflater = LayoutInflater.from(binding.root.context)

        init {
            binding.icon.typeface = iconTypeface
        }

        fun bind(section: StatsSection) {
            binding.icon.isVisible = section.icon != null
            section.icon?.let { binding.icon.text = it }
            binding.title.text = section.title

            binding.rows.removeAllViews()
            section.entries.forEachIndexed { index, entry ->
                val row = StatsRowItemBinding.inflate(inflater, binding.rows, false)
                row.divider.isVisible = index > 0
                row.label.text = entry.label
                row.value.text = entry.value
                binding.rows.addView(row.root)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<StatsSection>() {
        override fun areItemsTheSame(oldItem: StatsSection, newItem: StatsSection): Boolean {
            return oldItem.key == newItem.key
        }

        override fun areContentsTheSame(oldItem: StatsSection, newItem: StatsSection): Boolean {
            return oldItem == newItem
        }
    }
}
