package comment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.btcmap.databinding.CommentsAdapterItemBinding

class CommentsAdapter :
    ListAdapter<CommentsAdapterItem, CommentsAdapter.ItemViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = CommentsAdapterItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ItemViewHolder(
        private val binding: CommentsAdapterItemBinding,
    ) : ViewHolder(
        binding.root,
    ) {
        fun bind(item: CommentsAdapterItem) {
            binding.message.text = item.comment
            binding.date.text = item.localizedDate
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<CommentsAdapterItem>() {

        override fun areItemsTheSame(
            oldItem: CommentsAdapterItem,
            newItem: CommentsAdapterItem,
        ): Boolean {
            return newItem == oldItem
        }

        override fun areContentsTheSame(
            oldItem: CommentsAdapterItem,
            newItem: CommentsAdapterItem,
        ): Boolean {
            return newItem == oldItem
        }
    }
}