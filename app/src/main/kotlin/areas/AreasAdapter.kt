package areas

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.load
import coil.request.ImageRequest
import coil.request.SuccessResult
import org.btcmap.databinding.ItemAreaBinding

class AreasAdapter(
    private val onItemClick: (Item) -> Unit,
) : ListAdapter<AreasAdapter.Item, AreasAdapter.ItemViewHolder>(DiffCallback()) {

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

        fun bind(item: Item, onItemClick: (Item) -> Unit) {
            binding.apply {
                title.text = item.name
                subtitle.text = item.distance
                root.setOnClickListener { onItemClick(item) }

                iconPlaceholder.isVisible = true
                icon.load(null)

                if (item.iconUrl.isNotBlank()) {
                    if (item.iconUrl.endsWith(".svg")) {
                        val imageLoader = ImageLoader.Builder(root.context)
                            .components { add(SvgDecoder.Factory()) }.build()

                        val imageRequest =
                            ImageRequest.Builder(root.context).data(item.iconUrl).target(icon)
                                .listener(object : ImageRequest.Listener {
                                    override fun onSuccess(
                                        request: ImageRequest,
                                        result: SuccessResult,
                                    ) {
                                        iconPlaceholder.isVisible = false
                                    }
                                }).tag(item.id).build()

                        imageLoader.enqueue(imageRequest)
                    } else {
                        icon.load(data = item.iconUrl) {
                            listener(object : ImageRequest.Listener {
                                override fun onSuccess(
                                    request: ImageRequest,
                                    result: SuccessResult,
                                ) {
                                    iconPlaceholder.isVisible = false
                                }
                            }).tag(item.id)
                        }
                    }
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Item>() {

        override fun areItemsTheSame(
            oldItem: Item,
            newItem: Item,
        ): Boolean {
            return newItem.id == oldItem.id
        }

        override fun areContentsTheSame(
            oldItem: Item,
            newItem: Item,
        ): Boolean {
            return newItem == oldItem
        }
    }

    data class Item(
        val id: String,
        val iconUrl: String,
        val name: String,
        val distance: String,
    )
}