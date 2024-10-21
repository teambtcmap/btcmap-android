package element

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import element_comment.ElementComment
import org.btcmap.databinding.ItemCommentBinding
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class CommentsAdapter :
    ListAdapter<ElementComment, CommentsAdapter.ItemViewHolder>(DiffCallback()) {

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
        fun bind(item: ElementComment) {
            binding.apply {
                message.text = item.comment
                val dateFormat = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                date.text = dateFormat.format(OffsetDateTime.parse(item.createdAt))
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ElementComment>() {

        override fun areItemsTheSame(
            oldItem: ElementComment,
            newItem: ElementComment,
        ): Boolean {
            return newItem == oldItem
        }

        override fun areContentsTheSame(
            oldItem: ElementComment,
            newItem: ElementComment,
        ): Boolean {
            return newItem == oldItem
        }
    }
}