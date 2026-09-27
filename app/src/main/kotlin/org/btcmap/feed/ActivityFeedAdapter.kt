package org.btcmap.feed

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
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
            val context = binding.root.context
            binding.apply {
                placeName.text = item.placeName.orEmpty()
                userName.text = when (item.type) {
                    ActivityFeedItem.TYPE_PLACE_BOOSTED -> item.durationDays?.let {
                        context.resources.getQuantityString(
                            R.plurals.activity_boosted_for_days,
                            it.toInt(),
                            it.toInt(),
                        )
                    } ?: ""
                    ActivityFeedItem.TYPE_PLACE_COMMENTED -> item.comment ?: ""
                    ActivityFeedItem.TYPE_PLACE_ADDED ->
                        userNameText(context, item, R.string.activity_added_by)
                    ActivityFeedItem.TYPE_PLACE_UPDATED ->
                        userNameText(context, item, R.string.activity_updated_by)
                    ActivityFeedItem.TYPE_PLACE_DELETED ->
                        userNameText(context, item, R.string.activity_deleted_by)
                    else -> userNameText(context, item, R.string.activity_by_user)
                }
                date.text = getRelativeTime(context, item.date)

                icon.setImageResource(
                    when (item.type) {
                        ActivityFeedItem.TYPE_PLACE_ADDED -> R.drawable.icon_add_location
                        ActivityFeedItem.TYPE_PLACE_UPDATED -> R.drawable.icon_edit
                        ActivityFeedItem.TYPE_PLACE_BOOSTED -> R.drawable.icon_rocket_launch
                        ActivityFeedItem.TYPE_PLACE_COMMENTED -> R.drawable.icon_comment
                        ActivityFeedItem.TYPE_PLACE_DELETED -> R.drawable.icon_delete
                        else -> R.drawable.icon_place
                    }
                )

                root.setOnClickListener { onItemClick(item) }
            }
        }

        private fun userNameText(
            context: Context,
            item: ActivityFeedItem,
            @StringRes resId: Int,
        ): String {
            return item.osmUserName?.let { context.getString(resId, it) } ?: ""
        }

        private fun getRelativeTime(context: Context, dateString: String): String {
            val date = ZonedDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME)
            val now = ZonedDateTime.now()
            val diffMillis = now.toInstant().toEpochMilli() - date.toInstant().toEpochMilli()

            val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
            val hours = TimeUnit.MILLISECONDS.toHours(diffMillis)
            val days = TimeUnit.MILLISECONDS.toDays(diffMillis)

            return when {
                minutes < 1 -> context.getString(R.string.activity_just_now)
                minutes < 60 -> context.resources.getQuantityString(
                    R.plurals.activity_minutes_ago,
                    minutes.toInt(),
                    minutes.toInt(),
                )
                hours < 24 -> context.resources.getQuantityString(
                    R.plurals.activity_hours_ago,
                    hours.toInt(),
                    hours.toInt(),
                )
                days < 7 -> context.resources.getQuantityString(
                    R.plurals.activity_days_ago,
                    days.toInt(),
                    days.toInt(),
                )
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