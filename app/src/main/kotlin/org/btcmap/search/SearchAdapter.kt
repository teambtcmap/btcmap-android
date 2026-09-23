package org.btcmap.search

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import com.google.android.material.color.MaterialColors
import org.btcmap.area.preloadAreaHeader
import org.btcmap.settings.boostedMarkerBackgroundColor
import org.btcmap.settings.prefs
import org.btcmap.util.iconTypeface
import org.btcmap.databinding.SearchAdapterItemBinding

class SearchAdapter(
    private val onItemClick: (SearchAdapterItem) -> Unit,
) : ListAdapter<SearchAdapterItem, SearchAdapter.ItemViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ItemViewHolder {
        val binding = SearchAdapterItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        binding.icon.typeface = iconTypeface
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick)
    }

    class ItemViewHolder(
        private val binding: SearchAdapterItemBinding,
    ) : RecyclerView.ViewHolder(
        binding.root,
    ) {

        // Identifies the area currently bound to the holder. Coil delivers its
        // result asynchronously, so a result for a recycled binding must not
        // hide the spinner the new binding just showed.
        private var boundAreaId: Long? = null

        fun bind(item: SearchAdapterItem, onItemClick: (SearchAdapterItem) -> Unit) {
            val area = item as? SearchAdapterItem.Area
            val isBoosted = (item as? SearchAdapterItem.Place)?.boosted == true
            boundAreaId = area?.areaId

            // The icon is hidden for areas, but tinting it anyway keeps the
            // recycled holder correct when the next row is a merchant.
            tintIcon(isBoosted)

            binding.apply {
                icon.text = item.icon
                name.text = item.name
                distance.text = item.distanceToUser
                boosted.isVisible = isBoosted
                root.setOnClickListener { onItemClick(item) }

                if (area?.iconUrl != null) {
                    loading.isVisible = true
                    icon.isVisible = false
                    iconImage.isVisible = true
                    iconImage.load(area.iconUrl) {
                        listener(
                            onSuccess = { _, _ -> hideLoading(area.areaId) },
                            onError = { _, _ -> hideLoading(area.areaId) },
                        )
                    }
                } else {
                    loading.isVisible = false
                    iconImage.isVisible = false
                    iconImage.setImageDrawable(null)
                    icon.isVisible = true
                }

                // Tapping an area row can open the area screen straight away,
                // so warm its header while the row is on screen even when the
                // row falls back to the placeholder icon.
                root.context.preloadAreaHeader(area?.headerImageUrl)
            }
        }

        /**
         * Tints the icon circle with the configured boosted marker color and a
         * white glyph when [isBoosted], and restores the theme colors otherwise
         * so a recycled holder cannot keep a previous row's boost tint.
         */
        private fun tintIcon(isBoosted: Boolean) {
            if (isBoosted) {
                binding.icon.backgroundTintList =
                    ColorStateList.valueOf(prefs.boostedMarkerBackgroundColor())
                binding.icon.setTextColor(Color.WHITE)
                return
            }

            binding.icon.backgroundTintList = ColorStateList.valueOf(
                MaterialColors.getColor(
                    binding.root,
                    androidx.appcompat.R.attr.colorPrimary,
                    Color.BLACK,
                ),
            )
            binding.icon.setTextColor(
                MaterialColors.getColor(
                    binding.root,
                    com.google.android.material.R.attr.colorOnPrimary,
                    Color.WHITE,
                ),
            )
        }

        /**
         * Hides the spinner once the row's image has loaded or failed, so a
         * failed load leaves the plain placeholder rather than a spinner that
         * never stops.
         */
        private fun hideLoading(areaId: Long) {
            if (boundAreaId == areaId) {
                binding.loading.isVisible = false
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<SearchAdapterItem>() {

        override fun areItemsTheSame(
            oldItem: SearchAdapterItem,
            newItem: SearchAdapterItem,
        ): Boolean {
            return newItem == oldItem
        }

        override fun areContentsTheSame(
            oldItem: SearchAdapterItem,
            newItem: SearchAdapterItem,
        ): Boolean {
            return newItem == oldItem
        }
    }
}
