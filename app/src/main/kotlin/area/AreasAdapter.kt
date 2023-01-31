package area

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import coil.load
import coil.size.ViewSizeResolver
import org.btcmap.R
import org.btcmap.databinding.ItemAreaBinding

class AreasAdapter(
    private val onItemClick: (Item) -> Unit,
) : ListAdapter<AreasAdapter.Item, AreasAdapter.ItemViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemAreaBinding.inflate(
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
        private val binding: ItemAreaBinding,
    ) : ViewHolder(
        binding.root,
    ) {

        fun bind(item: Item, onItemClick: (Item) -> Unit) {
            binding.apply {
                title.text = item.name
                subtitle.text = item.distance
                root.setOnClickListener { onItemClick(item) }

                icon.load(item.iconUrl) {
                    placeholder(R.drawable.item_area_placeholder)
                    error(R.drawable.item_area_placeholder)
                    size(ViewSizeResolver(icon))
                    crossfade(true)
                }
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

    data class Item(
        val id: String,
        val iconUrl: String,
        val name: String,
        val distance: String,
    )
}