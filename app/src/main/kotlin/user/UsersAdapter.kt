package user

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import coil.load
import coil.request.ImageRequest
import coil.request.SuccessResult
import okhttp3.HttpUrl
import org.btcmap.R
import org.btcmap.databinding.ItemUserBinding

class UsersAdapter(
    private val onItemClick: (Item) -> Unit,
) : ListAdapter<UsersAdapter.Item, UsersAdapter.ItemViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemUserBinding.inflate(
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
        private val binding: ItemUserBinding,
    ) : ViewHolder(
        binding.root,
    ) {

        fun bind(item: Item, onItemClick: (Item) -> Unit) {
            binding.apply {
                title.text = item.name
                subtitle.text = root.context.resources.getQuantityString(
                    R.plurals.d_changes, item.changes.toInt(), item.changes
                )

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
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    }
                } else {
                    tip.isVisible = false
                }

                avatarPlaceholder.isVisible = true
                avatar.load(null)

                if (item.image != null) {
                    avatar.load(data = item.image.toString()) {
                        listener(object : ImageRequest.Listener {
                            override fun onSuccess(
                                request: ImageRequest,
                                result: SuccessResult,
                            ) {
                                avatarPlaceholder.isVisible = false
                            }
                        }).tag(item.id)
                    }
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
        val id: Long,
        val name: String,
        val changes: Long,
        val tipLnurl: String,
        val image: HttpUrl?,
    )
}