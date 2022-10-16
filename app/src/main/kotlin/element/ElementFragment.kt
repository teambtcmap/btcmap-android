package element

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import conf.ConfRepo
import db.Element
import elements.ElementsRepo
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import map.MapMarkersRepo
import map.getErrorColor
import map.getOnSurfaceColor
import org.btcmap.R
import org.btcmap.databinding.FragmentElementBinding
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import search.SearchResultModel
import java.time.ZonedDateTime

class ElementFragment : Fragment() {

    private val elementsRepo: ElementsRepo by inject()

    private val confRepo: ConfRepo by inject()

    private val tagsJsonFormatter by lazy { Json { prettyPrint = true } }

    private val resultModel: SearchResultModel by sharedViewModel()

    private var elementId = ""

    private var _binding: FragmentElementBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentElementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (arguments != null) {
            ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { appBar, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
                appBar.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    topMargin = insets.top
                }
                val navBarsInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
                binding.scrollView.setPadding(0, 0, 0, navBarsInsets.bottom)
                WindowInsetsCompat.CONSUMED
            }

            val elementId = ElementFragmentArgs.fromBundle(requireArguments()).elementId
            val element = runBlocking { elementsRepo.selectById(elementId)!! }
            setElement(element)

            binding.toolbar.setNavigationIcon(R.drawable.baseline_arrow_back_24)

            binding.toolbar.setNavigationOnClickListener {
                findNavController().popBackStack()
            }

            binding.mapContainer.isVisible = true

            binding.mapContainer.setOnClickListener {
                resultModel.element.update { element }
                findNavController().navigate(R.id.action_elementFragment_to_mapFragment)
            }

            binding.map.post {
                val mapController = binding.map.controller
                mapController.setZoom(16.toDouble())
                val startPoint = GeoPoint(element.lat, element.lon)
                mapController.setCenter(startPoint)
                mapController.zoomTo(19.0)

                val markersRepo = MapMarkersRepo(requireContext(), confRepo)
                val marker = Marker(binding.map)
                marker.position = GeoPoint(element.lat, element.lon)
                marker.icon = markersRepo.getMarker(element)
                binding.map.overlays.add(marker)
            }
        }

        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_view_on_osm -> {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse(
                        "https://www.openstreetmap.org/${
                            elementId.replace(
                                ":", "/"
                            )
                        }"
                    )
                    startActivity(intent)
                }

                R.id.action_edit_on_osm -> {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse(
                        "https://www.openstreetmap.org/edit?${
                            elementId.replace(
                                ":", "="
                            )
                        }"
                    )
                    startActivity(intent)
                }

                R.id.action_supertagger_manual -> {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data =
                        Uri.parse("https://github.com/teambtcmap/btcmap-data/wiki/Tagging-Instructions")
                    startActivity(intent)
                }
            }

            true
        }

        binding.tags.isVisible = confRepo.conf.value.showTags
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setElement(element: Element) {
        elementId = element.id

        val tags = element.tags()
        binding.toolbar.title = tags["name"]?.jsonPrimitive?.content ?: "Unnamed"

        val surveyDate = tags["survey:date"]?.jsonPrimitive?.content ?: ""

        if (surveyDate.isNotBlank()) {
            runCatching {
                val date = DateUtils.getRelativeDateTimeString(
                    requireContext(),
                    ZonedDateTime.parse(surveyDate + "T00:00:00Z").toEpochSecond() * 1000,
                    DateUtils.SECOND_IN_MILLIS,
                    DateUtils.WEEK_IN_MILLIS,
                    0,
                ).split(",").first()

                binding.lastVerified.text = date
                binding.lastVerified.setTextColor(requireContext().getOnSurfaceColor())
            }.onFailure {
                binding.lastVerified.text = surveyDate
                binding.lastVerified.setTextColor(requireContext().getOnSurfaceColor())
            }
        } else {
            binding.lastVerified.text = getString(R.string.not_verified_by_supertaggers)
            binding.lastVerified.setTextColor(requireContext().getErrorColor())
        }

        val address = buildString {
            if (tags.containsKey("addr:housenumber")) {
                append(tags["addr:housenumber"]!!.jsonPrimitive.content)
            }

            if (tags.containsKey("addr:street")) {
                append(" ")
                append(tags["addr:street"]!!.jsonPrimitive.content)
            }

            if (tags.containsKey("addr:city")) {
                append(", ")
                append(tags["addr:city"]!!.jsonPrimitive.content)
            }

            if (tags.containsKey("addr:postcode")) {
                append(", ")
                append(tags["addr:postcode"]!!.jsonPrimitive.content)
            }
        }.trim(',', ' ')

        binding.address.isVisible = address.isNotBlank()
        binding.address.text = address

        val phone = tags["phone"]?.jsonPrimitive?.content
        binding.phone.text = phone
        binding.phone.isVisible = phone != null

        val website = tags["website"]?.jsonPrimitive?.content
        binding.website.text = website
        binding.website.isVisible = website != null

        val facebook = tags["contact:facebook"]?.jsonPrimitive?.content
        binding.facebook.text = facebook
        binding.facebook.isVisible = facebook != null

        val openingHours = tags["opening_hours"]?.jsonPrimitive?.content
        binding.openingHours.text = openingHours
        binding.openingHours.isVisible = openingHours != null

        binding.tags.text = tagsJsonFormatter.encodeToString(JsonObject.serializer(), tags)
    }
}