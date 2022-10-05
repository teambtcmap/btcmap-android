package elementevents

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.btcmap.databinding.ItemElementEventBinding

class ElementEventsAdapter(
    private val onItemClick: (ElementEvent) -> Unit,
) : ListAdapter<ElementEvent, ElementEventsAdapter.ItemViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemElementEventBinding.inflate(
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
        private val binding: ItemElementEventBinding,
    ) : ViewHolder(
        binding.root,
    ) {

        @SuppressLint("SetTextI18n")
        fun bind(item: ElementEvent, onItemClick: (ElementEvent) -> Unit) {
            binding.apply {
                title.text = "${item.elementName} was ${item.eventType}ed by ${item.user}"
                subtitle.text = item.date
                root.setOnClickListener { onItemClick(item) }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ElementEvent>() {

        override fun areItemsTheSame(
            oldItem: ElementEvent,
            newItem: ElementEvent,
        ): Boolean {
            return newItem == oldItem
        }

        override fun areContentsTheSame(
            oldItem: ElementEvent,
            newItem: ElementEvent,
        ): Boolean {
            return newItem == oldItem
        }
    }
}