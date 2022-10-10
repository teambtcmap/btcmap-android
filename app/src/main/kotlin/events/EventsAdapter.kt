package events

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.btcmap.R
import org.btcmap.databinding.ItemElementEventBinding
import java.time.ZonedDateTime

class EventsAdapter(
    private val onItemClick: (Item) -> Unit,
) : ListAdapter<EventsAdapter.Item, EventsAdapter.ItemViewHolder>(DiffCallback()) {

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
        fun bind(item: Item, onItemClick: (Item) -> Unit) {
            binding.apply {
                when (item.type) {
                    "create" -> icon.setImageResource(R.drawable.baseline_add_location_alt_24)
                    "update" -> icon.setImageResource(R.drawable.baseline_edit_24)
                    "delete" -> icon.setImageResource(R.drawable.baseline_delete_24)
                }

                title.text = item.elementName

                var subtitleText = DateUtils.getRelativeDateTimeString(
                    root.context,
                    item.date.toEpochSecond() * 1000,
                    DateUtils.SECOND_IN_MILLIS,
                    DateUtils.WEEK_IN_MILLIS,
                    0
                ).split(",").first()

                if (item.username.isNotBlank()) {
                    subtitleText += " by ${item.username}"
                }

                subtitle.text = subtitleText

                if (item.tipLnurl.isNotBlank()) {
                    tip.isVisible = true

                    tip.setOnClickListener {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = Uri.parse(item.tipLnurl)
                        runCatching {
                            root.context.startActivity(intent)
                        }.onFailure {
                            Toast.makeText(
                                root.context,
                                R.string.you_dont_have_a_compatible_wallet,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } else {
                    tip.isVisible = false
                }

                root.setOnClickListener { onItemClick(item) }
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
        val date: ZonedDateTime,
        val type: String,
        val elementId: String,
        val elementName: String,
        val username: String,
        val tipLnurl: String,
    )
}