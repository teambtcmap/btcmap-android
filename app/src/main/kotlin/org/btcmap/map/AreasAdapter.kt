package org.btcmap.map

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import okhttp3.HttpUrl
import java.text.NumberFormat
import org.btcmap.area.preloadAreaHeader
import org.btcmap.databinding.AreaItemBinding

class AreasAdapter(
    private val apiUrl: HttpUrl,
    private val onItemClick: (MapArea) -> Unit,
) : ListAdapter<MapArea, AreasAdapter.ItemViewHolder>(DiffCallback()) {

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

        // Identifies the area currently bound to the holder. Coil delivers its
        // result asynchronously, so a result for a recycled binding must not
        // hide the spinner the new binding just showed.
        private var boundAreaId: Long? = null

        fun bind(area: MapArea, onItemClick: (MapArea) -> Unit) {
            boundAreaId = area.id
            binding.apply {
                loading.isVisible = true
                icon.load("$apiUrl/v4/areas/${area.id}/image?type=square&w=256&h=256") {
                    listener(
                        onSuccess = { _, _ -> hideLoading(area.id) },
                        onError = { _, _ -> hideLoading(area.id) },
                    )
                }
                root.context.preloadAreaHeader(area.headerImageUrl)
                val count = area.upcomingEventsCount
                badge.isVisible = count > 0
                if (count > 0) {
                    badge.text = NumberFormat.getIntegerInstance().format(count)
                }
                root.setOnClickListener { onItemClick(area) }
            }
        }

        /**
         * Hides the spinner once the chip's image has loaded or failed. A
         * failed load leaves the orange background rather than an empty circle.
         */
        private fun hideLoading(areaId: Long) {
            if (boundAreaId == areaId) {
                binding.loading.isVisible = false
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<MapArea>() {

        override fun areItemsTheSame(oldItem: MapArea, newItem: MapArea): Boolean {
            return newItem.id == oldItem.id
        }

        override fun areContentsTheSame(oldItem: MapArea, newItem: MapArea): Boolean {
            return newItem == oldItem
        }
    }
}
