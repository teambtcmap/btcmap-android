package filter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.btcmap.databinding.ItemCategoryBinding

class ElementCategoriesAdapter(
    private val listener: Listener,
) : ListAdapter<ElementCategoriesAdapter.Item, ElementCategoriesAdapter.ItemViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )

        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(getItem(position), listener)
    }

    class ItemViewHolder(
        private val binding: ItemCategoryBinding,
    ) : ViewHolder(
        binding.root,
    ) {

        fun bind(item: Item, listener: Listener) {
            binding.apply {
                name.text = item.text

                enabled.setOnCheckedChangeListener(null)
                enabled.isChecked = item.enabled

                enabled.setOnCheckedChangeListener { _, isChecked ->
                    listener.onItemCheckedChange(item, isChecked)
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
            return newItem.id == oldItem.id
        }
    }

    data class Item(
        val id: String,
        val text: String,
        val enabled: Boolean,
    )

    interface Listener {

        fun onItemCheckedChange(item: Item, checked: Boolean)
    }
}