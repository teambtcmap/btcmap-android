package search

import android.graphics.Bitmap
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import db.Place
import org.btcmap.databinding.RowPlacesSearchResultBinding

class PlacesSearchAdapter(
    private val onItemClick: (Item) -> Unit,
) : ListAdapter<PlacesSearchAdapter.Item, PlacesSearchAdapter.ItemViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ItemViewHolder {
        val binding = RowPlacesSearchResultBinding.inflate(
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
        val place: Place,
        val icon: Bitmap,
        val name: String,
        val distanceToUser: String,
    )

    class ItemViewHolder(
        private val binding: RowPlacesSearchResultBinding,
    ) : RecyclerView.ViewHolder(
        binding.root,
    ) {

        fun bind(item: Item, onItemClick: (Item) -> Unit) {
            binding.apply {
                icon.setImageBitmap(item.icon)
                name.text = item.name
                distance.visibility = if (item.distanceToUser.isNotEmpty()) View.VISIBLE else View.GONE
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
            return newItem.place.id == oldItem.place.id
        }

        override fun areContentsTheSame(
            oldItem: Item,
            newItem: Item,
        ): Boolean {
            return true
        }
    }
}