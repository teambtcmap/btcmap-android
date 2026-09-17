package org.btcmap.place

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.format.DateUtils
import android.text.style.URLSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.boost.BoostFragment
import org.btcmap.db.table.place.Place
import org.btcmap.settings.prefs
import org.btcmap.comment.AddCommentFragment
import org.btcmap.comment.CommentsAdapter
import org.btcmap.comment.CommentsAdapterItem
import org.btcmap.comment.CommentsFragment
import org.btcmap.util.iconTypeface
import org.btcmap.util.showError
import org.btcmap.map.getErrorColor
import org.btcmap.map.getOnSurfaceColor
import org.btcmap.R
import org.btcmap.auth.showAuthDialog
import org.btcmap.db
import org.btcmap.databinding.PlaceFragmentBinding
import org.btcmap.i18n.getLocalizedName
import org.btcmap.i18n.getLocalizedOpeningHours
import org.btcmap.saved.toggleSavedPlace
import org.btcmap.settings.authorized
import org.btcmap.util.rethrowIfCancellation
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class PlaceFragment : Fragment() {

    private var placeId = 0L

    private var placeName = ""

    private lateinit var commentsAdapter: CommentsAdapter

    private var _binding: PlaceFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = PlaceFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.directions -> {
                    val place =
                        db().place.selectById(placeId) ?: return@setOnMenuItemClickListener true
                    val uri = "geo:${place.lat},${place.lon}?q=${place.getLocalizedName()}".toUri()
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    requireContext().startActivity(Intent.createChooser(intent, null))
                }

                R.id.share -> {
                    val uri = "https://btcmap.org/merchant/$placeId".toUri()
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        putExtra(Intent.EXTRA_TEXT, uri.toString())
                        type = "text/plain"
                    }
                    requireContext().startActivity(Intent.createChooser(intent, null))
                }

                R.id.view_on_btcmap -> {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = "https://btcmap.org/merchant/$placeId".toUri()
                    startActivity(intent)
                }

                R.id.save -> onSaveClicked()
            }

            true
        }

        binding.outdated.typeface = iconTypeface
        binding.outdated.setTextColor(requireContext().getErrorColor())

        binding.companionWarning.setTextColor(requireContext().getErrorColor())
        TextViewCompat.setCompoundDrawableTintList(
            binding.companionWarning,
            ColorStateList.valueOf(requireContext().getErrorColor()),
        )

        commentsAdapter = CommentsAdapter()
        binding.commentsList.layoutManager = LinearLayoutManager(requireContext())
        binding.commentsList.adapter = commentsAdapter
    }

    private fun onSaveClicked() {
        val toggle = {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    toggleSavedPlace(placeId, placeName)
                    updateBookmarkIcon()
                } catch (e: Throwable) {
                    e.rethrowIfCancellation()
                    showError(e)
                }
            }
            Unit
        }

        if (prefs.authorized) {
            toggle()
        } else {
            showAuthDialog { toggle() }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setPlace(place: Place) {
        placeId = place.id
        placeName = place.getLocalizedName().orEmpty()

        binding.toolbar.title = place.getLocalizedName()
        binding.toolbar.setSingleLine(false)

        updateBookmarkIcon()

        binding.bundledWarning.isVisible = place.bundled

        // disable action buttons for bundled places
        binding.btnVerify.isVisible = !place.bundled
        binding.btnReport.isVisible = !place.bundled
        binding.comments.isVisible = !place.bundled
        binding.boost.isVisible = !place.bundled
        binding.addComment.isVisible = !place.bundled

        if (place.requiredAppUrl != null) {
            binding.companionWarning.isVisible = true
            binding.companionWarning.setTextColor(requireContext().getErrorColor())
            binding.companionWarning.text =
                getString(R.string.companion_warning, place.requiredAppUrl.toString().trimEnd('/'))
        } else {
            binding.companionWarning.isVisible = false
        }

        val outdatedUri =
            "https://wiki.btcmap.org/Verifying-Existing-Merchants".toUri()

        if (place.verifiedAt != null) {
            val date = DateUtils.getRelativeDateTimeString(
                requireContext(),
                place.verifiedAt.toLocalDate().toEpochDay() * 24 * 3600 * 1000,
                DateUtils.SECOND_IN_MILLIS,
                DateUtils.WEEK_IN_MILLIS,
                0,
            ).split(",").first()

            binding.lastVerified.text = date

            if (place.verifiedAt.isAfter(ZonedDateTime.now().minusYears(1))) {
                binding.lastVerified.isVisible = true
                binding.lastVerified.setTextColor(requireContext().getOnSurfaceColor())
                binding.lastVerified.setOnClickListener(null)
                binding.outdated.isVisible = false
                binding.outdated.setOnClickListener(null)
            } else {
                binding.lastVerified.isVisible = true
                binding.lastVerified.setTextColor(requireContext().getErrorColor())
                binding.lastVerified.setOnClickListener {
                    startActivity(Intent(Intent.ACTION_VIEW, outdatedUri))
                }
                binding.outdated.isVisible = true
                binding.outdated.setOnClickListener {
                    startActivity(Intent(Intent.ACTION_VIEW, outdatedUri))
                }
            }
        } else {
            if (!place.bundled) {
                binding.lastVerified.isVisible = true
                binding.lastVerified.text = getString(R.string.not_verified)
                binding.lastVerified.setTextColor(requireContext().getErrorColor())
                binding.lastVerified.setOnClickListener {
                    startActivity(Intent(Intent.ACTION_VIEW, outdatedUri))
                }
                binding.outdated.isVisible = true
                binding.outdated.setOnClickListener {
                    startActivity(Intent(Intent.ACTION_VIEW, outdatedUri))
                }
            } else {
                binding.lastVerified.isVisible = false
                binding.outdated.isVisible = false
            }
        }

        binding.address.text = place.address
        binding.address.isVisible = !place.address.isNullOrBlank()

        binding.phone.text = place.phone
        binding.phone.isVisible = !place.phone.isNullOrBlank()

        binding.website.text = place.website.toString().replace("https://", "").trimEnd('/')
        binding.website.isVisible = place.website != null

        if (place.twitter == null) {
            binding.twitter.isVisible = false
        } else {
            binding.twitter.isVisible = true
            binding.twitter.text =
                place.twitter.toString().replace("https://twitter.com/", "").trim('@')
            binding.twitter.styleAsLink()
            binding.twitter.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = place.twitter.toString().toUri()
                startActivity(intent)
            }
        }

        if (place.telegram == null) {
            binding.telegram.isVisible = false
        } else {
            binding.telegram.isVisible = true
            binding.telegram.text = place.telegram.toString().replace("https://t.me/", "")
            binding.telegram.styleAsLink()
            binding.telegram.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = place.telegram.toString().toUri()
                startActivity(intent)
            }
        }

        if (place.line == null) {
            binding.line.isVisible = false
        } else {
            binding.line.isVisible = true
            if (place.line.queryParameter("accountId").isNullOrBlank()) {
                binding.line.text = place.line.toString().replace("https://line.me/R/ti/p/@", "")
            } else {
                binding.line.text = place.line.queryParameter("accountId")
            }
            binding.line.styleAsLink()
            binding.line.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = place.line.toString().toUri()
                startActivity(intent)
            }
        }

        if (place.facebook == null) {
            binding.facebook.isVisible = false
        } else {
            binding.facebook.isVisible = true
            var text =
                place.facebook.toString().replace("https://www.facebook.com/people/", "")
                    .replace("https://www.facebook.com/p/", "")
                    .replace("https://www.facebook.com/", "")
                    .replace("https://facebook.com/", "").trimEnd('/')
            if (text.contains("/") && text.split("/").size == 2) {
                text = text.split("/").first()
            }
            binding.facebook.text = text
            binding.facebook.styleAsLink()
            binding.facebook.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = place.facebook.toString().toUri()
                startActivity(intent)
            }
        }

        if (place.instagram == null) {
            binding.instagram.isVisible = false
        } else {
            binding.instagram.isVisible = true
            binding.instagram.text =
                place.instagram.toString().replace("https://www.instagram.com/", "")
                    .replace("https://instagram.com/", "").trim('@', '/')
            binding.instagram.styleAsLink()
            binding.instagram.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = place.instagram.toString().toUri()
                startActivity(intent)
            }
        }

        binding.email.text = place.email
        binding.email.isVisible = place.email != null

        binding.openingHours.text = place.getLocalizedOpeningHours(requireContext())
        binding.openingHours.isVisible = place.getLocalizedOpeningHours(requireContext()) != null

        binding.btnVerify.setOnClickListener {
            openReport(defaultType = "verified")
        }

        binding.btnReport.setOnClickListener {
            openReport(defaultType = null)
        }

        binding.comments.setOnClickListener {
            val comments = db().comment.selectByPlaceId(place.id)
            if (comments.isEmpty()) {
                openAddComment()
            } else {
                openComments()
            }
        }

        binding.boost.setOnClickListener {
            requireActivity().supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace<BoostFragment>(
                    R.id.fragmentContainerView, null, Bundle().apply {
                        putLong("place_id", place.id)
                    }
                )
                addToBackStack(null)
            }
        }

        binding.addComment.setOnClickListener { openAddComment() }

        val comments = db().comment.selectByPlaceId(place.id)
        binding.commentsTitle.text = getString(R.string.comments_d, comments.size)
        binding.commentsTitle.isVisible = comments.isNotEmpty()
        binding.comments.text = if (comments.isEmpty()) {
            getString(R.string.comment)
        } else {
            getString(R.string.comments_d, comments.size)
        }
        binding.comments.isEnabled = true
        val commentDateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        commentsAdapter.submitList(comments.map {
            CommentsAdapterItem(
                comment = it.comment,
                localizedDate = it.createdAt.format(commentDateFormat),
            )
        })
    }

    private fun openReport(defaultType: String?) {
        val placeId = placeId
        val navigate = {
            requireActivity().supportFragmentManager.commit {
                setReorderingAllowed(true)
                val args = Bundle().apply {
                    putLong("place_id", placeId)
                    if (defaultType != null) putString("default_type", defaultType)
                }
                replace<ReportPlaceFragment>(R.id.fragmentContainerView, null, args)
                addToBackStack(null)
            }
            Unit
        }
        if (prefs.authorized) {
            navigate()
        } else {
            showAuthDialog { navigate() }
        }
    }

    private fun openComments() {
        val placeId = placeId
        requireActivity().supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace<CommentsFragment>(
                R.id.fragmentContainerView, null, Bundle().apply { putLong("place_id", placeId) }
            )
            addToBackStack(null)
        }
    }

    private fun openAddComment() {
        requireActivity().supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace<AddCommentFragment>(
                R.id.fragmentContainerView, null, Bundle().apply { putLong("place_id", placeId) }
            )
            addToBackStack(null)
        }
    }

    fun onSlide(bottomSheetTop: Int) {
        val toolbar = _binding?.toolbar ?: return
        val statusBarTop = ViewCompat.getRootWindowInsets(toolbar)
            ?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
        toolbar.updateLayoutParams<LinearLayout.LayoutParams> {
            topMargin = (statusBarTop - bottomSheetTop).coerceIn(0, statusBarTop)
        }
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

    fun Toolbar.setSingleLine(singleLine: Boolean) {
        for (i in 0..childCount) {
            val child = getChildAt(i)
            if (child is TextView) {
                child.isSingleLine = singleLine
            }
        }
    }

    private fun updateBookmarkIcon() {
        if (prefs.authorized) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val user = withContext(Dispatchers.IO) { db().user.select() }
                    val saved = user?.savedPlaces?.any { it.id == placeId } == true
                    withResumed {
                        binding.toolbar.menu.findItem(R.id.save).setIcon(
                            if (saved) R.drawable.icon_bookmark_check else R.drawable.icon_bookmark
                        )
                    }
                } catch (e: Throwable) {
                    e.rethrowIfCancellation()
                    showError(e)
                }
            }
        } else {
            binding.toolbar.menu.findItem(R.id.save).setIcon(R.drawable.icon_bookmark)
        }
    }
}
