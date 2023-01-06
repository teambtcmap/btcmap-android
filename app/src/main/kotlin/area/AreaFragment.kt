package area

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import areas.AreaResultModel
import areas.AreasRepo
import elements.ElementsRepo
import elements.bitcoinSurveyDate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import map.getErrorColor
import map.getOnSurfaceColor
import map.name
import map.toBoundingBox
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.btcmap.R
import org.btcmap.databinding.FragmentAreaBinding
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class AreaFragment : Fragment() {

    private val areasRepo: AreasRepo by inject()

    private val elementsRepo: ElementsRepo by inject()

    private val areaResultModel: AreaResultModel by activityViewModel()

    private var _binding: FragmentAreaBinding? = null
    private val binding get() = _binding!!

    private val adapter = AreaAdapter(
        listener = object : AreaAdapter.Listener {
            override fun onMapClick() {
                viewLifecycleOwner.lifecycleScope.launch {
                    val area = areasRepo.selectById(requireArguments().getString("area_id")!!)
                        ?: return@launch
                    areaResultModel.area.update { area }
                    findNavController().navigate(R.id.action_areaFragment_to_mapFragment)
                }
            }

            override fun onElementClick(item: AreaAdapter.Item.Element) {
                findNavController().navigate(
                    R.id.elementFragment,
                    bundleOf("element_id" to item.id),
                )
            }

            override fun onUrlClick(url: HttpUrl) {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url.toString())
                startActivity(intent)
            }
        },
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAreaBinding.inflate(inflater, container, false)
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

        binding.toolbar.apply {
            setNavigationOnClickListener {
                findNavController().popBackStack()
            }

            setOnMenuItemClickListener {
                if (it.itemId == R.id.action_reports) {
                    findNavController().navigate(
                        R.id.reportsFragment,
                        bundleOf("area_id" to requireArguments().getString("area_id")!!),
                    )
                }

                true
            }
        }

        val area = runBlocking {
            areasRepo.selectById(requireArguments().getString("area_id")!!)
        } ?: return

        binding.toolbar.title = area.name()

        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val box = area.toBoundingBox()

                val elements = elementsRepo.selectByBoundingBox(
                    minLat = box.latSouth,
                    maxLat = box.latNorth,
                    minLon = box.lonWest,
                    maxLon = box.lonEast,
                ).map {
                    val status: String
                    val statusColor: Int

                    val surveyDate = it.osmTags.bitcoinSurveyDate()

                    if (surveyDate != null) {
                        val date = DateUtils.getRelativeDateTimeString(
                            requireContext(),
                            surveyDate.toEpochSecond() * 1000,
                            DateUtils.SECOND_IN_MILLIS,
                            DateUtils.WEEK_IN_MILLIS,
                            0,
                        ).split(",").first()

                        status = date
                        statusColor = requireContext().getOnSurfaceColor()
                    } else {
                        status = getString(R.string.not_verified)
                        statusColor = requireContext().getErrorColor()
                    }

                    AreaAdapter.Item.Element(
                        id = it.id,
                        iconId = it.icon.ifBlank { "question_mark" },
                        name = it.osmTags["name"]?.jsonPrimitive?.content
                            ?: if (it.icon == "local_atm") getString(R.string.atm) else getString(
                                R.string.unnamed_place
                            ),
                        status = status,
                        statusColor = statusColor,
                        showCheckmark = surveyDate != null,
                    )
                }.sortedBy { it.name }.toMutableList()

                val boundingBoxPaddingPx = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    16f,
                    resources.displayMetrics,
                ).toInt()

                val map = AreaAdapter.Item.Map(
                    boundingBox = area.toBoundingBox(),
                    boundingBoxPaddingPx = boundingBoxPaddingPx,
                )

                val contact = AreaAdapter.Item.Contact(
                    website = area.tags["contact:website"]?.jsonPrimitive?.content?.toHttpUrlOrNull(),
                    twitter = area.tags["contact:twitter"]?.jsonPrimitive?.content?.toHttpUrlOrNull(),
                    telegram = area.tags["contact:telegram"]?.jsonPrimitive?.content?.toHttpUrlOrNull(),
                    discord = area.tags["contact:discord"]?.jsonPrimitive?.content?.toHttpUrlOrNull(),
                    youtube = area.tags["contact:youtube"]?.jsonPrimitive?.content?.toHttpUrlOrNull(),
                )

                adapter.submitList(listOf(map, contact) + elements)
                binding.toolbar.subtitle = resources.getQuantityString(
                    R.plurals.d_places,
                    elements.size,
                    elements.size,
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}