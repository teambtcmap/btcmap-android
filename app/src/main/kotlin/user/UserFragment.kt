package user

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import events.EventsAdapter
import events.EventsRepo
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.btcmap.R
import org.btcmap.databinding.FragmentUserBinding
import org.koin.android.ext.android.inject
import users.UsersRepo
import java.time.ZonedDateTime

class UserFragment : Fragment() {

    private val usersRepo: UsersRepo by inject()

    private val eventsRepo: EventsRepo by inject()

    private var _binding: FragmentUserBinding? = null
    private val binding get() = _binding!!

    private val adapter = EventsAdapter(object : EventsAdapter.Listener {
        override fun onItemClick(item: EventsAdapter.Item) {
            findNavController().navigate(
                UserFragmentDirections.actionUserFragmentToElementFragment(
                    item.elementId,
                ),
            )
        }

        override fun onShowMoreClick() {}
    })

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
            usersRepo.selectById(
                UserFragmentArgs.fromBundle(
                    requireArguments()
                ).userId
            )
        } ?: return

        val userOsmJson: JsonObject = Json.decodeFromString(user.osm_json)
        val userName = userOsmJson["display_name"]?.jsonPrimitive?.content ?: return

        binding.toolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.action_view_on_osm) {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("https://www.openstreetmap.org/user/${userName}")
                startActivity(intent)
            }

            true
        }

        WindowCompat.getInsetsController(
            requireActivity().window,
            requireActivity().window.decorView,
        ).isAppearanceLightStatusBars =
            when (requireContext().resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_NO -> true
                else -> false
            }

        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                binding.toolbar.title = userName.ifBlank { getString(R.string.unnamed_user) }

                val items = eventsRepo.selectEventsByUserIdAsListItems(
                    UserFragmentArgs.fromBundle(
                        requireArguments()
                    ).userId
                ).map {
                    EventsAdapter.Item(
                        date = ZonedDateTime.parse(it.event_date),
                        type = it.event_type,
                        elementId = it.element_id,
                        elementName = it.element_name ?: getString(R.string.unnamed_place),
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