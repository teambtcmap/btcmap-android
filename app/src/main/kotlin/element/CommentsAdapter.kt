package element

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import db.table.comment.Comment
import org.btcmap.databinding.ItemCommentBinding
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class CommentsAdapter :
    ListAdapter<Comment, CommentsAdapter.ItemViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemCommentBinding.inflate(
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
        private val binding: ItemCommentBinding,
    ) : ViewHolder(
        binding.root,
    ) {
        fun bind(item: Comment) {
            binding.apply {
                message.text = item.comment
                val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                date.text = item.createdAt.format(dateFormat)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Comment>() {

        override fun areItemsTheSame(
            oldItem: Comment,
            newItem: Comment,
        ): Boolean {
            return newItem == oldItem
        }

        override fun areContentsTheSame(
            oldItem: Comment,
            newItem: Comment,
        ): Boolean {
            return newItem == oldItem
        }
    }
}