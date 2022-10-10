package users

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.squareup.picasso.Picasso
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

        @SuppressLint("SetTextI18n")
        fun bind(item: Item, onItemClick: (Item) -> Unit) {
            binding.apply {
                title.text = item.name
                subtitle.text = root.context.resources.getQuantityString(
                    R.plurals.d_changes,
                    item.changes.toInt(),
                    item.changes
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
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } else {
                    tip.isVisible = false
                }

                Picasso.get().load(null as String?).into(avatar)

                avatar.isVisible = item.imgHref.isNotBlank()
                avatarPlaceholder.isVisible = item.imgHref.isBlank()

                if (item.imgHref.isNotBlank()) {
                    Picasso.get().load(item.imgHref).placeholder(R.drawable.baseline_person_24).into(avatar)
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
        val imgHref: String,
    )
}