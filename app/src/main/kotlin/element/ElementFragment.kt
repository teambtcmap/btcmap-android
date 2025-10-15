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
import android.view.animation.AccelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.recyclerview.widget.LinearLayoutManager
import boost.BoostFragment
import db.db
import db.table.place.Place
import db.table.comment.CommentQueries
import comment.AddCommentFragment
import comment.CommentsFragment
import icons.iconTypeface
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import map.MapFragment
import map.getErrorColor
import map.getOnSurfaceColor
import org.btcmap.R
import org.btcmap.databinding.FragmentElementBinding
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.Style
import search.SearchResultModel
import settings.mapStyle
import settings.prefs
import settings.uri
import java.time.ZonedDateTime

class ElementFragment : Fragment() {

    private val elementsRepo: ElementsRepo by lazy {
        ElementsRepo()
    }

    private val resultModel: SearchResultModel by activityViewModels()

    private var elementId = -1L

    val commentsAdapter = CommentsAdapter()

    private var _binding: FragmentElementBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentElementBinding.inflate(inflater, container, false)
        return binding.root
    }

    fun onPartialExpanded(progress: Float) {
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { appBar, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            val interpolator = AccelerateInterpolator()
            appBar.updateLayoutParams<LinearLayout.LayoutParams> {
                topMargin = (insets.top * interpolator.getInterpolation(progress)).toInt()
            }
            WindowInsetsCompat.CONSUMED
        }
        binding.toolbar.requestApplyInsets()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.commentsList.layoutManager = LinearLayoutManager(requireContext())
        binding.commentsList.isNestedScrollingEnabled = false
        binding.commentsList.adapter = commentsAdapter

        if (arguments != null) {
            ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { appBar, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
                appBar.updateLayoutParams<LinearLayout.LayoutParams> {
                    topMargin = insets.top
                }
                val navBarsInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
                binding.scrollView.setPadding(0, 0, 0, navBarsInsets.bottom)
                WindowInsetsCompat.CONSUMED
            }

            val elementId = requireArguments().getLong("element_id")
            val element = runBlocking { elementsRepo.selectById(elementId) }
            setElement(element)

            binding.toolbar.setNavigationIcon(R.drawable.arrow_back)

            binding.toolbar.setNavigationOnClickListener {
                parentFragmentManager.popBackStack()
            }

            binding.map.isVisible = true

            binding.map.getMapAsync { map ->
                map.addOnMapClickListener {
                    resultModel.element.update { element }
                    parentFragmentManager.commit {
                        replace<MapFragment>(R.id.fragmentContainerView)
                    }
                    true
                }

                map.uiSettings.setAllGesturesEnabled(false)
                map.setStyle(
                    Style.Builder().fromUri(prefs.mapStyle.uri(requireContext()))
                )
                map.cameraPosition =
                    CameraPosition.Builder().target(LatLng(element.lat, element.lon)).zoom(15.0)
                        .build()
            }
        }

        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_show_directions -> {
                    val element = runBlocking { elementsRepo.selectById(elementId) }
                    val uri = "geo:${element.lat},${element.lon}?q=${element.name}".toUri()
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    requireContext().startActivity(Intent.createChooser(intent, null))
                }

                R.id.action_share -> {
//                    val element = runBlocking { elementsRepo.selectById(elementId)!! }
//                    val osmType = element.osmId.split(":").first()
//                    val osmId = element.osmId.split(":").last()
//                    val uri = Uri.parse("https://btcmap.org/merchant/$osmType:$osmId")
//                    val intent = Intent(Intent.ACTION_SEND).apply {
//                        putExtra(Intent.EXTRA_TEXT, uri.toString())
//                        type = "text/plain"
//                    }
//                    requireContext().startActivity(Intent.createChooser(intent, null))
                }

                R.id.action_view_on_osm -> {
//                    val element = runBlocking { elementsRepo.selectById(elementId)!! }
//                    val osmType = element.osmId.split(":").first()
//                    val osmId = element.osmId.split(":").last()
//                    val intent = Intent(Intent.ACTION_VIEW)
//                    intent.data = Uri.parse("https://www.openstreetmap.org/$osmType/$osmId")
//                    startActivity(intent)
                }

                R.id.action_edit_on_osm -> {
//                    val element = runBlocking { elementsRepo.selectById(elementId)!! }
//                    val osmType = element.osmId.split(":").first()
//                    val osmId = element.osmId.split(":").last()
//                    val intent = Intent(Intent.ACTION_VIEW)
//                    intent.data = Uri.parse("https://www.openstreetmap.org/edit?$osmType=$osmId")
//                    startActivity(intent)
                }

                R.id.action_editor_manual -> {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data =
                        "https://gitea.btcmap.org/teambtcmap/btcmap-general/wiki/Tagging-Merchants".toUri()
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

    fun setElement(element: Place) {
        elementId = element.id

        binding.toolbar.title = element.name

        binding.bundledWarning.isVisible = element.bundled

        binding.verifyOrReport.isVisible = !element.bundled
        binding.comments.isVisible = !element.bundled
        binding.boost.isVisible = !element.bundled
        binding.addComment.isVisible = !element.bundled

        if (element.requiredAppUrl != null) {
            binding.companionWarning.isVisible = true
            binding.companionWarning.setTextColor(requireContext().getErrorColor())
            binding.companionWarning.text =
                getString(R.string.companion_warning, element.requiredAppUrl)
        } else {
            binding.companionWarning.isVisible = false
        }

        val outdatedUri =
            "https://gitea.btcmap.org/teambtcmap/btcmap-general/wiki/Verifying-Existing-Merchants".toUri()

        if (element.verifiedAt != null) {
            val date = DateUtils.getRelativeDateTimeString(
                requireContext(),
                element.verifiedAt.toLocalDate().toEpochDay() * 24 * 3600 * 1000,
                DateUtils.SECOND_IN_MILLIS,
                DateUtils.WEEK_IN_MILLIS,
                0,
            ).split(",").first()

            binding.lastVerified.text = date

            if (element.verifiedAt.isAfter(ZonedDateTime.now().minusYears(1))) {
                binding.lastVerified.isVisible = true
                binding.lastVerified.setTextColor(requireContext().getOnSurfaceColor())
                binding.lastVerified.setOnClickListener(null)
                binding.outdated.isVisible = false
                binding.outdated.setOnClickListener(null)
            } else {
                binding.lastVerified.isVisible = true
                binding.lastVerified.setTextColor(requireContext().getErrorColor())
                binding.lastVerified.setOnClickListener { openUri(outdatedUri) }
                binding.outdated.isVisible = true
                binding.outdated.setOnClickListener { openUri(outdatedUri) }
            }
        } else {
            if (!element.bundled) {
                binding.lastVerified.isVisible = true
                binding.lastVerified.text = getString(R.string.not_verified)
                binding.lastVerified.setTextColor(requireContext().getErrorColor())
                binding.lastVerified.setOnClickListener { openUri(outdatedUri) }
                binding.outdated.isVisible = true
                binding.outdated.setOnClickListener { openUri(outdatedUri) }
            } else {
                binding.lastVerified.isVisible = false
                binding.outdated.isVisible = false
            }
        }

        binding.address.text = element.address
        binding.address.isVisible = !element.address.isNullOrBlank()

        binding.phone.text = element.phone
        binding.phone.isVisible = !element.phone.isNullOrBlank()

        binding.website.text = element.website.toString()
        binding.website.isVisible = element.website != null

        if (element.twitter == null) {
            binding.twitter.isVisible = false
        } else {
            binding.twitter.isVisible = true
            binding.twitter.text =
                element.twitter.toString().replace("https://twitter.com/", "").trim('@')
            binding.twitter.styleAsLink()
            binding.twitter.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = element.twitter.toString().toUri()
                startActivity(intent)
            }
        }

        if (element.telegram == null) {
            binding.facebook.isVisible = false
        } else {
            binding.telegram.isVisible = true
            binding.telegram.text = element.telegram.toString().replace("https://t.me/", "")
            binding.telegram.styleAsLink()
            binding.telegram.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = element.telegram.toString().toUri()
                startActivity(intent)
            }
        }

        if (element.facebook == null) {
            binding.facebook.isVisible = false
        } else {
            binding.facebook.isVisible = true
            var text =
                element.facebook.toString().replace("https://www.facebook.com/people/", "")
                    .replace("https://www.facebook.com/", "")
                    .replace("https://facebook.com/", "").trimEnd('/')
            if (text.contains("/") && text.split("/").size == 2) {
               text = text.split("/").first()
            }
            binding.facebook.text = text
            binding.facebook.styleAsLink()
            binding.facebook.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = element.facebook.toString().toUri()
                startActivity(intent)
            }
        }

        if (element.instagram == null) {
            binding.instagram.isVisible = false
        } else {
            binding.instagram.isVisible = true
            binding.instagram.text =
                element.instagram.toString().replace("https://www.instagram.com/", "")
                    .replace("https://instagram.com/", "").trim('@', '/')
            binding.instagram.styleAsLink()
            binding.instagram.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = element.instagram.toString().toUri()
                startActivity(intent)
            }
        }

        binding.email.text = element.email
        binding.email.isVisible = element.email != null

        binding.openingHours.text = element.openingHours
        binding.openingHours.isVisible = element.openingHours != null

        binding.verifyOrReport.setOnClickListener {
//            val intent = Intent(Intent.ACTION_VIEW)
//            val osmType = element.overpassData.optString("type")
//            val osmId = element.overpassData.optLong("id")
//            intent.data = Uri.parse("https://btcmap.org/verify-location?id=$osmType:$osmId")
//            startActivity(intent)
        }

        binding.comments.setOnClickListener {
            requireActivity().supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace<CommentsFragment>(
                    R.id.fragmentContainerView, null, bundleOf("place_id" to elementId)
                )
                addToBackStack(null)
            }
        }

        binding.boost.setOnClickListener {
            requireActivity().supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace<BoostFragment>(
                    R.id.fragmentContainerView, null, bundleOf("place_id" to elementId)
                )
                addToBackStack(null)
            }
        }

        binding.addComment.setOnClickListener {
            requireActivity().supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace<AddCommentFragment>(
                    R.id.fragmentContainerView, null, bundleOf("element_id" to elementId)
                )
                addToBackStack(null)
            }
        }

//        val imageUrl = tags.optString("image").toHttpUrlOrNull()
//
//        if (imageUrl != null) {
//            binding.image.isVisible = true
//            binding.image.load(imageUrl)
//        } else {
//            binding.image.isVisible = false
//        }

        val comments = CommentQueries.selectByPlaceId(
            element.id,
            db,
        )
        binding.commentsTitle.text = getString(R.string.comments_d, comments.size)
        binding.commentsTitle.isVisible = comments.isNotEmpty()
        binding.comments.text = getString(R.string.comments_d, comments.size)
        binding.comments.isEnabled = comments.isNotEmpty()
        commentsAdapter.submitList(comments)
    }

    private fun TextView.styleAsLink() {
        setText(
            SpannableString(text).apply {
                setSpan(
                    URLSpan(""), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
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
