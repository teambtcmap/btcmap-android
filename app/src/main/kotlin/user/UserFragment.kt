package user

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import event.EventsAdapter
import event.EventsRepo
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.btcmap.R
import org.btcmap.databinding.FragmentUserBinding
import org.koin.android.ext.android.inject

class UserFragment : Fragment() {

    private val usersRepo: UsersRepo by inject()

    private val eventsRepo: EventsRepo by inject()

    private var _binding: FragmentUserBinding? = null
    private val binding get() = _binding!!

    private val adapter = EventsAdapter(object : EventsAdapter.Listener {
        override fun onItemClick(item: EventsAdapter.Item) {
            findNavController().navigate(
                R.id.elementFragment,
                bundleOf("element_id" to item.elementId),
            )
        }

        override fun onShowMoreClick() {}
    }).apply {
        canLoadMore = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { appBar, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            appBar.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topMargin = insets.top
            }
            val navBarsInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            binding.list.setPadding(0, 0, 0, navBarsInsets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        val user = runBlocking {
            usersRepo.selectById(requireArguments().getLong("user_id"))
        } ?: return

        val userName = user.osmData.optString("display_name")

        binding.toolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.action_view_on_osm) {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("https://www.openstreetmap.org/user/${userName}")
                startActivity(intent)
            }

            true
        }

        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                binding.toolbar.title = userName.ifBlank { getString(R.string.unnamed_user) }

                val items = eventsRepo.selectByUserIdAsListItems(
                    requireArguments().getLong("user_id"),
                ).map {
                    EventsAdapter.Item(
                        date = it.eventDate,
                        type = it.eventType,
                        elementId = it.elementId,
                        elementName = it.elementName.ifBlank { getString(R.string.unnamed) },
                        username = "",
                        tipLnurl = "",
                    )
                }
                adapter.submitList(items)
                binding.toolbar.subtitle = resources.getQuantityString(
                    R.plurals.d_changes,
                    items.size,
                    items.size,
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}