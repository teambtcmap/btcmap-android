package search

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.btcmap.databinding.RowPlacesSearchResultBinding

class PlacesSearchResultsAdapter(
    private val itemClick: (PlacesSearchRow) -> Unit
) : RecyclerView.Adapter<PlacesSearchResultsAdapter.ViewHolder>() {

    private val items = mutableListOf<PlacesSearchRow>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RowPlacesSearchResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )

        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.binding.apply {
            icon.setImageBitmap(item.icon)
            name.text = item.name
            distance.visibility = if (item.distanceToUser.isNotEmpty()) View.VISIBLE else View.GONE
            distance.text = item.distanceToUser
            root.setOnClickListener { itemClick(item) }
        }
    }

    override fun getItemCount() = items.size

    fun swapItems(newItems: Collection<PlacesSearchRow>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: RowPlacesSearchResultBinding) : RecyclerView.ViewHolder(binding.root)
}