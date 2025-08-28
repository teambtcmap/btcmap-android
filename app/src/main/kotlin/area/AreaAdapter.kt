package area

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import icons.iconTypeface
import okhttp3.HttpUrl
import org.btcmap.R
import org.btcmap.databinding.ItemAreaDescriptionBinding
import org.btcmap.databinding.ItemAreaElementBinding
import org.btcmap.databinding.ItemContactBinding
import org.btcmap.databinding.ItemIssuesBinding
import org.btcmap.databinding.ItemMapBinding
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import settings.MapStyle
import settings.uri

class AreaAdapter(
    private val listener: Listener,
) : ListAdapter<AreaAdapter.Item, AreaAdapter.ItemViewHolder>(DiffCallback()) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is Item.Map -> VIEW_TYPE_MAP
            is Item.Description -> VIEW_TYPE_DESCRIPTION
            is Item.Contact -> VIEW_TYPE_CONTACT
            is Item.Issues -> VIEW_TYPE_ISSUES
            is Item.Element -> VIEW_TYPE_ELEMENT
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ItemViewHolder {
        val binding = when (viewType) {
            VIEW_TYPE_MAP -> {
                ItemMapBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            }

            VIEW_TYPE_DESCRIPTION -> {
                ItemAreaDescriptionBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            }

            VIEW_TYPE_CONTACT -> {
                ItemContactBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            }

            VIEW_TYPE_ISSUES -> {
                ItemIssuesBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            }

            VIEW_TYPE_ELEMENT -> {
                ItemAreaElementBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                ).apply {
                    icon.typeface = parent.context.iconTypeface()
                }
            }

            else -> throw Exception()
        }

        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(
            item = getItem(position),
            listener = listener,
        )
    }

    sealed class Item {

        data class Map(
            val geoJson: String,
            val bounds: LatLngBounds,
            val paddingPx: Int,
            val style: MapStyle,
        ) : Item()

        data class Description(
            val text: String,
        ) : Item()

        data class Contact(
            val website: HttpUrl?,
            val twitter: HttpUrl?,
            val telegram: HttpUrl?,
            val discord: HttpUrl?,
            val youtube: HttpUrl?,
        ) : Item()

        data class Issues(
            val count: Int,
        ) : Item()

        data class Element(
            val id: Long,
            val iconId: String,
            val name: String,
            val status: String,
            val colorResId: Int,
            val showCheckmark: Boolean,
            val issues: Int,
        ) : Item()
    }

    class ItemViewHolder(
        private val binding: ViewBinding,
    ) : RecyclerView.ViewHolder(
        binding.root,
    ) {

        fun bind(
            item: Item,
            listener: Listener,
        ) {
            if (item is Item.Map && binding is ItemMapBinding) {
                binding.map.getMapAsync { map ->
                    val source = GeoJsonSource("area", item.geoJson)

                    val layer = FillLayer("layer", "area").withProperties(
                        PropertyFactory.fillColor(Color.parseColor("#88f7931a")),
                        PropertyFactory.fillAntialias(true),
                    )

                    map.setStyle(
                        Style.Builder().fromUri(item.style.uri(binding.root.context))
                            .withSource(source).withLayer(layer)
                    )

                    map.moveCamera(
                        CameraUpdateFactory.newLatLngBounds(
                            item.bounds, item.paddingPx
                        )
                    )

                    map.addOnMapClickListener {
                        listener.onMapClick()
                        true
                    }
                }
            }

            if (item is Item.Description && binding is ItemAreaDescriptionBinding) {
                binding.apply {
                    text.text = item.text
                }
            }

            if (item is Item.Contact && binding is ItemContactBinding) {
                binding.apply {
                    website.isVisible = item.website != null
                    website.setOnClickListener { item.website?.let { listener.onUrlClick(it) } }

                    twitter.isVisible = item.twitter != null
                    twitter.setOnClickListener { item.twitter?.let { listener.onUrlClick(it) } }

                    telegram.isVisible = item.telegram != null
                    telegram.setOnClickListener { item.telegram?.let { listener.onUrlClick(it) } }

                    discord.isVisible = item.discord != null
                    discord.setOnClickListener { item.discord?.let { listener.onUrlClick(it) } }

                    youtube.isVisible = item.youtube != null
                    youtube.setOnClickListener { item.youtube?.let { listener.onUrlClick(it) } }
                }
            }

            if (item is Item.Issues && binding is ItemIssuesBinding) {
                binding.apply {
                    count.text = binding.root.context.getString(R.string.issues_d, item.count)
                    root.setOnClickListener { listener.onIssuesClick() }
                }
            }

            if (item is Item.Element && binding is ItemAreaElementBinding) {
                binding.apply {
                    icon.text = item.iconId
                    title.text = item.name
                    checkmark.isVisible = item.showCheckmark
                    subtitle.text = item.status
                    val attrs =
                        root.context.theme.obtainStyledAttributes(intArrayOf(item.colorResId))
                    subtitle.setTextColor(attrs.getColor(0, 0))
                    root.setOnClickListener { listener.onElementClick(item) }
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Item>() {

        override fun areItemsTheSame(
            oldItem: Item,
            newItem: Item,
        ): Boolean {
            return if (oldItem is Item.Element && newItem is Item.Element) {
                oldItem.id == newItem.id
            } else (oldItem is Item.Map && newItem is Item.Map) || (oldItem is Item.Contact && newItem is Item.Contact) || (oldItem is Item.Issues && newItem is Item.Issues)
        }

        override fun areContentsTheSame(
            oldItem: Item,
            newItem: Item,
        ): Boolean {
            if (oldItem is Item.Map && newItem is Item.Map) {
                return true
            }

            if (oldItem is Item.Contact && newItem is Item.Contact) {
                return true
            }

            if (oldItem is Item.Issues && newItem is Item.Issues) {
                return true
            }

            return newItem == oldItem
        }
    }

    interface Listener {
        fun onMapClick()

        fun onUrlClick(url: HttpUrl)

        fun onIssuesClick()

        fun onElementClick(item: Item.Element)
    }

    companion object {
        const val VIEW_TYPE_MAP = 0
        const val VIEW_TYPE_DESCRIPTION = 1
        const val VIEW_TYPE_CONTACT = 2
        const val VIEW_TYPE_ISSUES = 3
        const val VIEW_TYPE_ELEMENT = 4
    }
}