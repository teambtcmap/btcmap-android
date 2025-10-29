package search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import icons.iconTypeface
import org.btcmap.databinding.ItemSearchResultBinding

class SearchAdapter(
    private val onItemClick: (SearchAdapterItem) -> Unit,
) : ListAdapter<SearchAdapterItem, SearchAdapter.ItemViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ItemViewHolder {
        val binding = ItemSearchResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        binding.icon.typeface = iconTypeface
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick)
    }

    class ItemViewHolder(
        private val binding: ItemSearchResultBinding,
    ) : RecyclerView.ViewHolder(
        binding.root,
    ) {

        fun bind(item: SearchAdapterItem, onItemClick: (SearchAdapterItem) -> Unit) {
            binding.apply {
                icon.text = item.icon
                name.text = item.name
                distance.text = item.distanceToUser
                root.setOnClickListener { onItemClick(item) }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<SearchAdapterItem>() {

        override fun areItemsTheSame(
            oldItem: SearchAdapterItem,
            newItem: SearchAdapterItem,
        ): Boolean {
            return newItem == oldItem
        }

        override fun areContentsTheSame(
            oldItem: SearchAdapterItem,
            newItem: SearchAdapterItem,
        ): Boolean {
            return newItem == oldItem
        }
    }
}