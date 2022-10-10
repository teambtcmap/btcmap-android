package events

import android.content.res.Configuration
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
import db.Database
import db.User
import element.tags
import elements.ElementsRepo
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.btcmap.databinding.FragmentElementEventsBinding
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import search.SearchResultModel
import users.UsersRepo
import java.time.ZonedDateTime
import java.util.regex.Pattern

class EventsFragment : Fragment() {

    private val db: Database by inject()

    private val eventsRepo: EventsRepo by inject()

    private val elementsRepo: ElementsRepo by inject()

    private val usersRepo: UsersRepo by inject()

    private val resultModel: SearchResultModel by sharedViewModel()

    private var _binding: FragmentElementEventsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentElementEventsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { toolbar, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            toolbar.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topMargin = insets.top
            }
            val navBarsInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            binding.list.setPadding(0, 0, 0, navBarsInsets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        WindowCompat.getInsetsController(
            requireActivity().window,
            requireActivity().window.decorView,
        ).isAppearanceLightStatusBars =
            when (requireContext().resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_NO -> true
                else -> false
            }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                binding.list.layoutManager = LinearLayoutManager(requireContext())
                val adapter = EventsAdapter {
                    resultModel.element.value =
                        db.elementQueries.selectById(it.elementId).executeAsOneOrNull()
                    findNavController().popBackStack()
                }
                binding.list.adapter = adapter
                adapter.submitList(eventsRepo.selectAll().map {
                    val element = elementsRepo.selectById(it.element_id) ?: return@map null
                    val user = usersRepo.selectById(it.user_id) ?: return@map null
                    val userOsmJson: JsonObject = Json.decodeFromString(user.osm_json)

                    EventsAdapter.Item(
                        date = ZonedDateTime.parse(it.date),
                        type = it.type,
                        elementId = it.element_id,
                        elementName = element.tags()["name"]?.jsonPrimitive?.content
                            ?: "Unnamed place",
                        username = userOsmJson["display_name"]?.jsonPrimitive?.content ?: "",
                        tipLnurl = user.lnurl(),
                    )
                }.filterNotNull())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun User.lnurl(): String {
        val osmJson: JsonObject = Json.decodeFromString(osm_json)
        val description = osmJson["description"]?.jsonPrimitive?.content ?: ""
        val pattern = Pattern.compile("\\(lightning:[^)]*\\)", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(description)
        val matchFound: Boolean = matcher.find()

        return if (matchFound) {
            matcher.group().trim('(', ')')
        } else {
            ""
        }
    }
}