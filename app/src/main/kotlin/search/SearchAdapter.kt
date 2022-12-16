package search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import elements.Element
import icons.iconTypeface
import org.btcmap.databinding.ItemSearchResultBinding

class SearchAdapter(
    private val onItemClick: (Item) -> Unit,
) : ListAdapter<SearchAdapter.Item, SearchAdapter.ItemViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ItemViewHolder {
        val binding = ItemSearchResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )

        binding.icon.typeface = parent.context.iconTypeface()

        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick)
    }

    data class Item(
        val element: Element,
        val icon: String,
        val name: String,
        val distanceToUser: String,
    )

    class ItemViewHolder(
        private val binding: ItemSearchResultBinding,
    ) : RecyclerView.ViewHolder(
        binding.root,
    ) {

        fun bind(item: Item, onItemClick: (Item) -> Unit) {
            binding.apply {
                icon.text = item.icon
                name.text = item.name
                distance.visibility =
                    if (item.distanceToUser.isNotEmpty()) View.VISIBLE else View.GONE
                distance.text = item.distanceToUser
                root.setOnClickListener { onItemClick(item) }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Item>() {

        override fun areItemsTheSame(
            oldItem: Item,
            newItem: Item,
        ): Boolean {
            return newItem.element.id == oldItem.element.id
        }

        override fun areContentsTheSame(
            oldItem: Item,
            newItem: Item,
        ): Boolean {
            return true
        }
    }
}