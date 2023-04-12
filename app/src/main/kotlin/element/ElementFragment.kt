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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import map.MapMarkersRepo
import map.enableDarkModeIfNecessary
import map.getErrorColor
import map.getOnSurfaceColor
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.btcmap.R
import org.btcmap.databinding.FragmentElementBinding
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import search.SearchResultModel

class ElementFragment : Fragment() {

    private val elementsRepo: ElementsRepo by inject()

    private val confRepo: ConfRepo by inject()

    private val tagsJsonFormatter by lazy { Json { prettyPrint = true } }

    private val resultModel: SearchResultModel by activityViewModel()

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

            val elementId = requireArguments().getString("element_id")!!
            val element = runBlocking { elementsRepo.selectById(elementId)!! }
            setElement(element)

            binding.toolbar.setNavigationIcon(R.drawable.arrow_back)

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
                marker.icon = markersRepo.getMarker(
                    element.tags["icon:android"]?.jsonPrimitive?.content ?: "question_mark"
                )
                binding.map.overlays.add(marker)
                binding.map.enableDarkModeIfNecessary()
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

                    findNavController().navigate(
                        R.id.tagsFragment,
                        bundleOf("element_id" to elementId),
                    )
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

        val tags: OsmTags = element.osmJson["tags"]?.jsonObject ?: OsmTags(emptyMap())

        binding.toolbar.title = tags.name(resources)

        val surveyDate = tags.bitcoinSurveyDate()

        if (surveyDate != null) {
            val date = DateUtils.getRelativeDateTimeString(
                requireContext(),
                surveyDate.toEpochSecond() * 1000,
                DateUtils.SECOND_IN_MILLIS,
                DateUtils.WEEK_IN_MILLIS,
                0,
            ).split(",").first()

            binding.lastVerified.text = date
            binding.lastVerified.setTextColor(requireContext().getOnSurfaceColor())
        } else {
            binding.lastVerified.text = getString(R.string.not_verified)
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

        val phone =
            tags["phone"]?.jsonPrimitive?.content ?: tags["contact:phone"]?.jsonPrimitive?.content
            ?: ""
        binding.phone.text = phone
        binding.phone.isVisible = phone.isNotBlank()

        val website = tags["website"]?.jsonPrimitive?.content
            ?: tags["contact:website"]?.jsonPrimitive?.content ?: ""
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
                .trimEnd('/')
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

        val email =
            tags["email"]?.jsonPrimitive?.content ?: tags["contact:email"]?.jsonPrimitive?.content
            ?: ""
        binding.email.text = email
        binding.email.isVisible = email.isNotBlank()

        val openingHours = tags["opening_hours"]?.jsonPrimitive?.content
        binding.openingHours.text = openingHours
        binding.openingHours.isVisible = openingHours != null

        binding.tags.text = tagsJsonFormatter.encodeToString(JsonObject.serializer(), tags)

        val pouchUsername = element.tags["payment:pouch"]?.jsonPrimitive?.content ?: ""

        if (pouchUsername.isNotBlank()) {
            binding.elementAction.setText(R.string.pay)
            binding.elementAction.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("https://app.pouch.ph/$pouchUsername")
                startActivity(intent)
            }
        } else {
            binding.elementAction.setText(R.string.verify)
            binding.elementAction.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data =
                    Uri.parse("https://btcmap.org/verify-location?&name=${element.osmJson["tags"]!!.jsonObject["name"]?.jsonPrimitive?.content ?: ""}&lat=${element.lat}&long=${element.lon}&${element.osmJson["type"]!!.jsonPrimitive.content}=${element.osmJson["id"]!!.jsonPrimitive.content}")
                startActivity(intent)
            }
        }
    }

    private fun TextView.styleAsLink() {
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