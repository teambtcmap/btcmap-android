package area

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.btcmap.databinding.ItemAreaElementBinding

class AreaElementsAdapter(
    private val onItemClick: (Item) -> Unit,
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
        holder.bind(getItem(position), onItemClick)
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

        fun bind(item: Item, onItemClick: (Item) -> Unit) {
            binding.apply {
                icon.setImageDrawable(item.icon)
                title.text = item.name
                subtitle.text = item.status
                subtitle.setTextColor(item.statusColor)
                root.setOnClickListener { onItemClick(item) }
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
}