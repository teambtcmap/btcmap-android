package org.btcmap.place

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.btcmap.databinding.PlacePhotoAddItemBinding

/** The trailing "add photo" tile in the place gallery. */
class PlacePhotoAddAdapter(
    private val onClick: () -> Unit,
) : RecyclerView.Adapter<PlacePhotoAddAdapter.ItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = PlacePhotoAddItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return ItemViewHolder(binding)
    }

    override fun getItemCount(): Int = 1

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(onClick)
    }

    class ItemViewHolder(
        private val binding: PlacePhotoAddItemBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(onClick: () -> Unit) {
            binding.root.setOnClickListener { onClick() }
        }
    }
}
