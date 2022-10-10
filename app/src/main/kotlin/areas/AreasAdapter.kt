package areas

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.btcmap.databinding.ItemAreaBinding
import java.text.NumberFormat

class AreasAdapter(
    private val onItemClick: (Area) -> Unit,
) : ListAdapter<Area, AreasAdapter.ItemViewHolder>(DiffCallback()) {

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

        @SuppressLint("SetTextI18n")
        fun bind(item: Area, onItemClick: (Area) -> Unit) {
            binding.apply {
                title.text = item.name
                val formatter = NumberFormat.getPercentInstance()
                val upToDatePercent =
                    formatter.format(item.up_to_date_elements.toDouble() / item.elements.toDouble())
                subtitle.text = "${item.elements} places ($upToDatePercent up-to-date)"
                subtitle.isVisible = false
                root.setOnClickListener { onItemClick(item) }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Area>() {

        override fun areItemsTheSame(
            oldItem: Area,
            newItem: Area,
        ): Boolean {
            return newItem.id == oldItem.id
        }

        override fun areContentsTheSame(
            oldItem: Area,
            newItem: Area,
        ): Boolean {
            return newItem == oldItem
        }
    }
}