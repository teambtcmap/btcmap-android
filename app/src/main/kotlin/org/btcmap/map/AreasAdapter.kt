package org.btcmap.map

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import okhttp3.HttpUrl
import org.btcmap.api.GetAreasItem
import org.btcmap.databinding.AreaItemBinding

class AreasAdapter(
    private val apiUrl: HttpUrl,
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

        return ItemViewHolder(apiUrl, binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick)
    }

    class ItemViewHolder(
        private val apiUrl: HttpUrl,
        private val binding: AreaItemBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(area: GetAreasItem, onItemClick: (GetAreasItem) -> Unit) {
            binding.apply {
                icon.load("$apiUrl/v4/areas/${area.id}/image?type=square&w=256&h=256")
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
