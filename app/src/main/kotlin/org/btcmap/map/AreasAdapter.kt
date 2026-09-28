package org.btcmap.map

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import okhttp3.HttpUrl
import java.text.NumberFormat
import org.btcmap.area.preloadAreaHeader
import org.btcmap.databinding.AreaItemBinding
import org.btcmap.settings.buttonBackgroundColor
import org.btcmap.settings.buttonIconColor
import org.btcmap.settings.prefs

class AreasAdapter(
    private val apiUrl: HttpUrl,
    private val onItemClick: (MapArea) -> Unit,
) : ListAdapter<MapArea, AreasAdapter.ItemViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ItemViewHolder {
        val binding = AreaItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )

        return ItemViewHolder(apiUrl, binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick)
    }

    /**
     * Re-runs every chip's image request, so a chip that fell back to its
     * initials while offline can pick up its image once the network is back.
     * A coarse change notification is enough: the rows keep their data and Coil
     * serves an image it already holds from its cache.
     */
    fun refreshImages() {
        notifyItemRangeChanged(0, itemCount)
    }

    class ItemViewHolder(
        private val apiUrl: HttpUrl,
        private val binding: AreaItemBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(area: MapArea, onItemClick: (MapArea) -> Unit) {
            binding.apply {
                // The initials show through until the image is set over them,
                // and are left visible when the request fails. They use the same
                // colors as the map's buttons so the fallback matches them.
                val context = root.context
                initials.backgroundTintList =
                    ColorStateList.valueOf(prefs.buttonBackgroundColor(context))
                initials.setTextColor(prefs.buttonIconColor(context))
                initials.text = areaInitials(area)
                icon.load("$apiUrl/v4/areas/${area.id}/image?type=square&w=256&h=256")
                root.context.preloadAreaHeader(area.headerImageUrl)
                val count = area.upcomingEventsCount
                badge.isVisible = count > 0
                if (count > 0) {
                    badge.text = NumberFormat.getIntegerInstance().format(count)
                }
                root.setOnClickListener { onItemClick(area) }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<MapArea>() {

        override fun areItemsTheSame(oldItem: MapArea, newItem: MapArea): Boolean {
            return newItem.id == oldItem.id
        }

        override fun areContentsTheSame(oldItem: MapArea, newItem: MapArea): Boolean {
            return newItem == oldItem
        }
    }
}

/**
 * The letters shown on a chip until its image loads, or in its place when the
 * image cannot load. A country shows its two-letter alias, the ISO code the
 * country is known by; a community shows the initials of up to two words of its
 * name, or the first two letters of a single-word name. The community's name is
 * already localized, so the fallback reads in the same language as the screen.
 */
internal fun areaInitials(area: MapArea): String {
    if (area.type == "country") return area.urlAlias.take(2).uppercase()

    val words = area.name.trim()
        .split(Regex("\\s+"))
        .filter { it.firstOrNull()?.isLetterOrDigit() == true }
    return when (words.size) {
        0 -> ""
        1 -> words[0].take(2).uppercase()
        else -> words.take(2).map { it.first() }.joinToString("").uppercase()
    }
}
