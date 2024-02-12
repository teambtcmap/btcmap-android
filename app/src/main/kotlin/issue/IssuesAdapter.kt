package issue

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.btcmap.R
import org.btcmap.databinding.ItemIssueBinding

class IssuesAdapter(
    private val listener: Listener,
) : ListAdapter<IssuesAdapter.Item, IssuesAdapter.ItemViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemIssueBinding.inflate(
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
        private val binding: ItemIssueBinding,
    ) : ViewHolder(
        binding.root,
    ) {

        fun bind(item: Item, listener: Listener) {
            binding.apply {
                when (item.type) {
                    "not_verified" -> icon.setImageResource(R.drawable.verified)
                    "out_of_date" -> icon.setImageResource(R.drawable.schedule)
                    else -> icon.setImageResource(R.drawable.place)
                }

                title.text = item.description
                subtitle.text = item.elementName

                root.setOnClickListener { listener.onItemClick(item) }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Item>() {

        override fun areItemsTheSame(
            oldItem: Item,
            newItem: Item,
        ): Boolean {
            return newItem == oldItem
        }

        override fun areContentsTheSame(
            oldItem: Item,
            newItem: Item,
        ): Boolean {
            return newItem == oldItem
        }
    }

    data class Item(
        val type: String,
        val severity: Int,
        val description: String,
        val osmUrl: String,
        val elementName: String,
    )

    interface Listener {
        fun onItemClick(item: Item)
    }
}