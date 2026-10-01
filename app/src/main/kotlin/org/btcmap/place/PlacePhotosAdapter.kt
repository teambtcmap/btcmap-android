package org.btcmap.place

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import org.btcmap.databinding.PlacePhotoItemBinding

/** The horizontal thumbnail strip of a place's photos. */
class PlacePhotosAdapter(
    private val onPhotoClick: (index: Int) -> Unit,
) : ListAdapter<PlacePhoto, PlacePhotosAdapter.ItemViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = PlacePhotoItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return ItemViewHolder(binding, onPhotoClick)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    class ItemViewHolder(
        private val binding: PlacePhotoItemBinding,
        private val onPhotoClick: (index: Int) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        // Identifies the photo currently bound to the holder. Coil delivers its
        // result asynchronously, so a result for a recycled binding must not
        // hide the spinner the new binding just showed.
        private var boundPhotoId: Long? = null

        fun bind(photo: PlacePhoto, position: Int) {
            boundPhotoId = photo.id
            binding.loading.isVisible = true
            binding.photo.load(photo.thumbnailUrl) {
                listener(
                    onSuccess = { _, _ -> hideLoading(photo.id) },
                    onError = { _, _ -> hideLoading(photo.id) },
                )
            }
            binding.root.setOnClickListener { onPhotoClick(position) }
        }

        private fun hideLoading(photoId: Long) {
            if (boundPhotoId == photoId) {
                binding.loading.isVisible = false
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<PlacePhoto>() {

        override fun areItemsTheSame(oldItem: PlacePhoto, newItem: PlacePhoto): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PlacePhoto, newItem: PlacePhoto): Boolean {
            return oldItem == newItem
        }
    }
}
