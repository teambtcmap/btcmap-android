package org.btcmap.map

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import org.btcmap.GetAreasItem
import org.btcmap.databinding.AreaItemBinding

class AreasAdapter(
    private val onItemClick: (GetAreasItem) -> Unit,
) : ListAdapter<GetAreasItem, AreasAdapter.ItemViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ItemViewHolder {
        val binding = AreaItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )

        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick)
    }

    class ItemViewHolder(
        private val binding: AreaItemBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(area: GetAreasItem, onItemClick: (GetAreasItem) -> Unit) {
            binding.apply {
                icon.load(area.icon)
                root.setOnClickListener { onItemClick(area) }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<GetAreasItem>() {

        override fun areItemsTheSame(oldItem: GetAreasItem, newItem: GetAreasItem): Boolean {
            return newItem.id == oldItem.id
        }

        override fun areContentsTheSame(oldItem: GetAreasItem, newItem: GetAreasItem): Boolean {
            return newItem == oldItem
        }
    }
}
