package area

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import db.Area
import org.btcmap.databinding.ItemAreaElementBinding
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint

class AreaElementsAdapter(
    private val area: Area,
    private val boundingBoxPaddingPx: Int,
    private val listener: Listener,
) : ListAdapter<AreaElementsAdapter.Item, AreaElementsAdapter.ItemViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ItemViewHolder {
        val binding = ItemAreaElementBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )

        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(getItem(position), position == 0, area, boundingBoxPaddingPx, listener)
    }

    data class Item(
        val id: String,
        val icon: Drawable,
        val name: String,
        val status: String,
        val statusColor: Int,
    )

    class ItemViewHolder(
        private val binding: ItemAreaElementBinding,
    ) : RecyclerView.ViewHolder(
        binding.root,
    ) {

        fun bind(
            item: Item,
            first: Boolean,
            area: Area,
            boundingBoxPaddingPx: Int,
            listener: Listener,
        ) {
            binding.apply {
                mapContainer.isVisible = first

                if (mapContainer.isVisible) {
                    val boundingBox = BoundingBox.fromGeoPoints(
                        mutableListOf(
                            GeoPoint(area.min_lat, area.min_lon),
                            GeoPoint(area.max_lat, area.max_lon),
                        )
                    )

                    map.post {
                        binding.map.zoomToBoundingBox(boundingBox, false, boundingBoxPaddingPx)
                    }

                    mapContainer.setOnClickListener { listener.onMapClick() }
                }

                icon.setImageDrawable(item.icon)
                title.text = item.name
                subtitle.text = item.status
                subtitle.setTextColor(item.statusColor)
                root.setOnClickListener { listener.onItemClick(item) }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Item>() {

        override fun areItemsTheSame(
            oldItem: Item,
            newItem: Item,
        ): Boolean {
            return newItem.id == oldItem.id
        }

        override fun areContentsTheSame(
            oldItem: Item,
            newItem: Item,
        ): Boolean {
            return newItem == oldItem
        }
    }

    interface Listener {

        fun onMapClick()

        fun onItemClick(item: Item)
    }
}