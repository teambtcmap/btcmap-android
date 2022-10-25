package element

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.format.DateUtils
import android.text.style.URLSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import conf.ConfRepo
import db.Element
import elements.ElementsRepo
import icons.toIconResId
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import map.MapMarkersRepo
import map.getErrorColor
import map.getOnSurfaceColor
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
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
                marker.icon = markersRepo.getMarker(element.icon_id.toIconResId())
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

                R.id.action_edit_tags -> {
                    if (confRepo.conf.value.osmLogin.isBlank()) {
                        findNavController().navigate(R.id.loginFragment)
                        return@setOnMenuItemClickListener true
                    }

                    if (arguments == null) {
                        findNavController().navigate(
                            R.id.tagsFragment,
                            bundleOf(Pair("element_id", elementId))
                        )
                    } else {
                        findNavController().navigate(
                            ElementFragmentDirections.actionElementFragmentToTagsFragment(
                                elementId
                            )
                        )
                    }
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

        val tags = element.osm_json["tags"]!!.jsonObject
        binding.toolbar.title = tags["name"]?.jsonPrimitive?.content ?: "Unnamed"

        val surveyDate = tags["survey:date"]?.jsonPrimitive?.content
            ?: tags["check_date"]?.jsonPrimitive?.content ?: ""

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

        val website = tags["website"]?.jsonPrimitive?.content ?: ""
        binding.website.text = website
            .replace("https://www.", "")
            .replace("http://www.", "")
            .replace("https://", "")
            .replace("http://", "")
            .trim('/')
        binding.website.isVisible = website.isNotBlank() && website.toHttpUrlOrNull() != null

        val twitter = tags["contact:twitter"]?.jsonPrimitive?.content
        binding.twitter.text = twitter?.replace("https://twitter.com/", "")?.trim('@')
        binding.twitter.styleAsLink()
        binding.twitter.isVisible = twitter != null

        binding.twitter.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://twitter.com/${binding.twitter.text}")
            startActivity(intent)
        }

        var facebookUrl = tags["contact:facebook"]?.jsonPrimitive?.content ?: ""
        var facebookUsername = ""

        if (facebookUrl.isNotBlank() && !facebookUrl.startsWith("https")) {
            facebookUsername = facebookUrl
            facebookUrl = "https://www.facebook.com/$facebookUrl"
        }

        if (facebookUsername.isNotBlank()) {
            binding.facebook.text = facebookUsername
        } else {
            binding.facebook.text = facebookUrl
                .replace("https://www.facebook.com/", "")
                .replace("https://facebook.com/", "")
        }

        binding.facebook.styleAsLink()
        binding.facebook.isVisible =
            (facebookUrl.isNotBlank() && facebookUrl.toHttpUrlOrNull() != null) || facebookUsername.isNotBlank()

        binding.facebook.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(facebookUrl)
            startActivity(intent)
        }

        val instagram = tags["contact:instagram"]?.jsonPrimitive?.content ?: ""
        binding.instagram.text = instagram
            .replace("https://www.instagram.com/", "")
            .replace("https://instagram.com/", "")
            .trim('@', '/')
        binding.instagram.styleAsLink()
        binding.instagram.isVisible = instagram.isNotBlank()

        binding.instagram.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://www.instagram.com/${binding.instagram.text}")
            startActivity(intent)
        }

        val openingHours = tags["opening_hours"]?.jsonPrimitive?.content
        binding.openingHours.text = openingHours
        binding.openingHours.isVisible = openingHours != null

        binding.tags.text = tagsJsonFormatter.encodeToString(JsonObject.serializer(), tags)

        binding.verifyOrReport.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data =
                Uri.parse("https://btcmap.org/report-outdated-info?&name=${element.osm_json["tags"]!!.jsonObject["name"]?.jsonPrimitive?.content ?: ""}&lat=${element.lat}&long=${element.lon}&${element.osm_json["type"]!!.jsonPrimitive.content}=${element.osm_json["id"]!!.jsonPrimitive.content}")
            startActivity(intent)
        }
    }

    fun TextView.styleAsLink() {
        setText(
            SpannableString(text).apply {
                setSpan(
                    URLSpan(""),
                    0,
                    length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            },
            TextView.BufferType.SPANNABLE,
        )
    }
}