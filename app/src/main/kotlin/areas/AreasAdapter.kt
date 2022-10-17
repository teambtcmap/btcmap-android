package areas

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import org.btcmap.databinding.ItemAreaBinding
import java.lang.Exception

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

                iconPlaceholder.isVisible = true
                Picasso.get().load(null as String?).into(icon)

                Picasso.get().load("https://data.btcmap.org/areas/icons/${item.id}.jpg")
                    .into(icon, object : Callback {
                        override fun onSuccess() {
                            iconPlaceholder.isVisible = false
                        }

                        override fun onError(e: Exception?) {
                            iconPlaceholder.isVisible = true
                        }
                    })
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
        val name: String,
        val distance: String,
    )
}