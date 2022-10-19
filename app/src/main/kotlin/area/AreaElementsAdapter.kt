package area

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import map.DisabledMapView
import org.btcmap.databinding.ItemAreaElementBinding
import org.osmdroid.util.BoundingBox

class AreaElementsAdapter(
    private val boundingBox: BoundingBox,
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
        holder.bind(
            item = getItem(position),
            first = position == 0,
            boundingBox = boundingBox,
            boundingBoxPaddingPx = boundingBoxPaddingPx,
            listener = listener,
        )
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
            boundingBox: BoundingBox,
            boundingBoxPaddingPx: Int,
            listener: Listener,
        ) {
            binding.apply {
                mapContainer.isVisible = first

                if (mapContainer.isVisible) {
                    val map = DisabledMapView(root.context, null)
                    mapContainer.addView(map)

                    map.post {
                        map.zoomToBoundingBox(
                            boundingBox,
                            false,
                            boundingBoxPaddingPx,
                        )
                    }

                    mapContainer.setOnClickListener { listener.onMapClick() }
                } else {
                    mapContainer.removeAllViews()
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