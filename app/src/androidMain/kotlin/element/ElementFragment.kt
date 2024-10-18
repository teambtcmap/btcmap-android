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
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import coil.load
import icons.iconTypeface
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import map.MapMarkersRepo
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
import java.time.ZonedDateTime

class ElementFragment : Fragment() {

    private val elementsRepo: ElementsRepo by inject()

    private val resultModel: SearchResultModel by activityViewModel()

    private var elementId = -1L

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

            val elementId = requireArguments().getLong("element_id")
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

                val markersRepo = MapMarkersRepo(requireContext())
                val marker = Marker(binding.map)
                marker.position = GeoPoint(element.lat, element.lon)
                marker.icon = markersRepo.getMarker(
                    element.tags.optString("icon:android").ifBlank { "question_mark" }, 0
                )
                binding.map.overlays.add(marker)
            }
        }

        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_show_directions -> {
                    val element = runBlocking { elementsRepo.selectById(elementId)!! }
                    val uri = "geo:${element.lat},${element.lon}?q=${element.name(resources)}"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                    requireContext().startActivity(Intent.createChooser(intent, null))
                }

                R.id.action_share -> {
                    val element = runBlocking { elementsRepo.selectById(elementId)!! }
                    val osmType = element.overpassData.optString("type")
                    val osmId = element.overpassData.optLong("id")
                    val uri = Uri.parse("https://btcmap.org/merchant/$osmType:$osmId")
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        putExtra(Intent.EXTRA_TEXT, uri.toString())
                        type = "text/plain"
                    }
                    requireContext().startActivity(Intent.createChooser(intent, null))
                }

                R.id.action_view_on_osm -> {
                    val element = runBlocking { elementsRepo.selectById(elementId)!! }
                    val osmType = element.overpassData.optString("type")
                    val osmId = element.overpassData.optLong("id")
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse("https://www.openstreetmap.org/$osmType/$osmId")
                    startActivity(intent)
                }

                R.id.action_edit_on_osm -> {
                    val element = runBlocking { elementsRepo.selectById(elementId)!! }
                    val osmType = element.overpassData.optString("type")
                    val osmId = element.overpassData.optLong("id")
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse("https://www.openstreetmap.org/edit?$osmType=$osmId")
                    startActivity(intent)
                }

                R.id.action_editor_manual -> {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data =
                        Uri.parse("https://wiki.btcmap.org/general/tagging-instructions.html")
                    startActivity(intent)
                }
            }

            true
        }

        binding.outdated.typeface = requireContext().iconTypeface()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setElement(element: Element) {
        elementId = element.id

        val tags: OsmTags = element.overpassData.optJSONObject("tags") ?: OsmTags()

        binding.toolbar.title = tags.name(resources)

        if (element.osmTag("payment:lightning:requires_companion_app") == "yes") {
            binding.companionWarning.isVisible = true
            binding.companionWarning.setTextColor(requireContext().getErrorColor())
            val companionApp = element
                .osmTag("payment:lightning:companion_app_url")
                .ifBlank { getString(R.string.unknown) }
            binding.companionWarning.text = getString(R.string.companion_warning, companionApp)
        } else {
            binding.companionWarning.isVisible = false
        }

        val surveyDate = tags.bitcoinSurveyDate()

        val outdatedUri = "https://wiki.btcmap.org/general/outdated".toUri()

        if (surveyDate != null) {
            val date = DateUtils.getRelativeDateTimeString(
                requireContext(),
                surveyDate.toEpochSecond() * 1000,
                DateUtils.SECOND_IN_MILLIS,
                DateUtils.WEEK_IN_MILLIS,
                0,
            ).split(",").first()

            binding.lastVerified.text = date

            if (surveyDate.isAfter(ZonedDateTime.now().minusYears(1))) {
                binding.lastVerified.setTextColor(requireContext().getOnSurfaceColor())
                binding.lastVerified.setOnClickListener(null)
                binding.outdated.isInvisible = true
                binding.outdated.setOnClickListener(null)
            } else {
                binding.lastVerified.setTextColor(requireContext().getErrorColor())
                binding.lastVerified.setOnClickListener { openUri(outdatedUri) }
                binding.outdated.isInvisible = false
                binding.outdated.setOnClickListener { openUri(outdatedUri) }
            }
        } else {
            binding.lastVerified.text = getString(R.string.not_verified)
            binding.lastVerified.setTextColor(requireContext().getErrorColor())
            binding.lastVerified.setOnClickListener { openUri(outdatedUri) }
            binding.outdated.isInvisible = false
            binding.outdated.setOnClickListener { openUri(outdatedUri) }
        }

        val address = buildString {
            if (tags.optString("addr:housenumber").isNotBlank()) {
                append(tags.getString("addr:housenumber"))
            }

            if (tags.optString("addr:street").isNotBlank()) {
                append(" ")
                append(tags.getString("addr:street"))
            }

            if (tags.optString("addr:city").isNotBlank()) {
                append(", ")
                append(tags.getString("addr:city"))
            }

            if (tags.optString("addr:postcode").isNotBlank()) {
                append(", ")
                append(tags.getString("addr:postcode"))
            }
        }.trim(',', ' ')

        binding.address.isVisible = address.isNotBlank()
        binding.address.text = address

        val phone = tags.optString("phone").ifBlank { tags.optString("contact:phone") }
        binding.phone.text = phone
        binding.phone.isVisible = phone.isNotBlank()

        val website = tags.optString("website").ifBlank { tags.optString("contact:website") }
        binding.website.text = website
            .replace("https://www.", "")
            .replace("http://www.", "")
            .replace("https://", "")
            .replace("http://", "")
            .trim('/')
        binding.website.isVisible = website.isNotBlank() && website.toHttpUrlOrNull() != null

        val twitter: String = tags.optString("contact:twitter")
        binding.twitter.text = twitter.replace("https://twitter.com/", "").trim('@')
        binding.twitter.styleAsLink()
        binding.twitter.isVisible = twitter.isNotBlank()

        binding.twitter.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://twitter.com/${binding.twitter.text}")
            startActivity(intent)
        }

        var facebookUrl = tags.optString("contact:facebook")
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

        val instagram = tags.optString("contact:instagram")
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

        val email = tags.optString("email").ifBlank { tags.optString("contact:email") }
        binding.email.text = email
        binding.email.isVisible = email.isNotBlank()

        val openingHours = tags.optString("opening_hours")
        binding.openingHours.text = openingHours
        binding.openingHours.isVisible = openingHours.isNotBlank()

        val pouchUsername = element.tags.optString("payment:pouch")

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
                    Uri.parse("https://btcmap.org/verify-location?id=$elementId")
                startActivity(intent)
            }
        }

        val imageUrl = tags.optString("image").toHttpUrlOrNull()

        if (imageUrl != null) {
            binding.image.isVisible = true
            binding.image.load(imageUrl) {
                this.fallback(R.drawable.merchant)
                this.error(R.drawable.merchant)
            }
        } else {
            binding.image.isVisible = false
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

    private fun openUri(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = uri
        startActivity(intent)
    }
}
