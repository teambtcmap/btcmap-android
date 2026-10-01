package org.btcmap.place

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import org.btcmap.databinding.PlacePhotoPageBinding

/** Swipeable fullscreen pages for the [PlacePhotoViewerDialogFragment]. */
class PlacePhotoPagerAdapter(
    private val urls: List<String>,
) : RecyclerView.Adapter<PlacePhotoPagerAdapter.PageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = PlacePhotoPageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return PageViewHolder(binding)
    }

    override fun getItemCount(): Int = urls.size

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(urls[position])
    }

    class PageViewHolder(
        private val binding: PlacePhotoPageBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        // A page can be recycled while its large image is still fetching, so a
        // late result must not hide the spinner a new page just showed.
        private var boundUrl: String? = null

        fun bind(url: String) {
            boundUrl = url
            binding.loading.isVisible = true
            binding.photo.load(url) {
                listener(
                    onSuccess = { _, _ -> hideLoading(url) },
                    onError = { _, _ -> hideLoading(url) },
                )
            }
        }

        private fun hideLoading(url: String) {
            if (boundUrl == url) {
                binding.loading.isVisible = false
            }
        }
    }
}
