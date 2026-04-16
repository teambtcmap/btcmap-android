package org.btcmap.activity

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.btcmap.R
import org.btcmap.api.ActivityFeedItem
import org.btcmap.databinding.ActivityFeedAdapterItemBinding
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

class ActivityFeedAdapter(
    private val onItemClick: (ActivityFeedItem) -> Unit,
) : ListAdapter<ActivityFeedItem, ActivityFeedAdapter.ItemViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ItemViewHolder {
        val binding = ActivityFeedAdapterItemBinding.inflate(
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
        private val binding: ActivityFeedAdapterItemBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ActivityFeedItem, onItemClick: (ActivityFeedItem) -> Unit) {
            binding.apply {
                placeName.text = item.placeName
                userName.text = when (item.type) {
                    "place_boosted" -> item.durationDays?.let { "boosted for $it days" } ?: ""
                    "place_commented" -> item.comment ?: ""
                    else -> item.osmUserName?.let { "by $it" } ?: ""
                }
                date.text = getRelativeTime(item.date)

                icon.setImageResource(
                    when (item.type) {
                        "place_added" -> R.drawable.icon_add_location
                        "place_updated" -> R.drawable.icon_edit
                        "place_boosted" -> R.drawable.icon_rocket_launch
                        "place_commented" -> R.drawable.icon_comment
                        else -> R.drawable.icon_place
                    }
                )

                root.setOnClickListener { onItemClick(item) }
            }
        }

        private fun getRelativeTime(dateString: String): String {
            val date = ZonedDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME)
            val now = ZonedDateTime.now()
            val diffMillis = now.toInstant().toEpochMilli() - date.toInstant().toEpochMilli()

            val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
            val hours = TimeUnit.MILLISECONDS.toHours(diffMillis)
            val days = TimeUnit.MILLISECONDS.toDays(diffMillis)

            return when {
                minutes < 1 -> "just now"
                minutes < 60 -> "$minutes min ago"
                hours < 24 -> "$hours hours ago"
                days < 7 -> "$days days ago"
                else -> date.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ActivityFeedItem>() {

        override fun areItemsTheSame(oldItem: ActivityFeedItem, newItem: ActivityFeedItem): Boolean {
            return newItem.placeId == oldItem.placeId && newItem.date == oldItem.date
        }

        override fun areContentsTheSame(oldItem: ActivityFeedItem, newItem: ActivityFeedItem): Boolean {
            return newItem == oldItem
        }
    }
}