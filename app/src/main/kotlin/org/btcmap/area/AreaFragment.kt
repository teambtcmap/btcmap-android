package org.btcmap.area

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import coil3.load
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.R
import org.btcmap.api
import org.btcmap.auth.showAuthDialog
import org.btcmap.db
import org.btcmap.db.table.user.User
import org.btcmap.databinding.AreaFragmentBinding
import org.btcmap.db.table.event.Event
import org.btcmap.settings.authorized
import org.btcmap.settings.prefs
import org.btcmap.util.CronUtils
import org.btcmap.util.openInBrowser
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class AreaFragment : Fragment() {

    private val areaId by lazy {
        requireArguments().getString("area_id")!!
    }

    private var _binding: AreaFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = AreaFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.save -> {
                    if (prefs.authorized) {
                        try {
                            viewLifecycleOwner.lifecycleScope.launch {
                                val user = db().user.select()!!
                                if (user.savedAreas.any { savedArea -> savedArea.asJsonObject["id"].asLong == areaId.toLong() }) {
                                    api().removeSavedArea(areaId.toLong())
                                } else {
                                    api().saveArea(areaId.toLong())
                                }
                                val updatedUser = api().getUser()
                                db().transaction {
                                    db().user.delete()
                                    db().user.insert(
                                        User(
                                            id = updatedUser.id,
                                            name = updatedUser.name,
                                            roles = updatedUser.roles,
                                            savedPlaces = updatedUser.savedPlaces,
                                            savedAreas = updatedUser.savedAreas,
                                        )
                                    )
                                }
                                updateBookmarkIcon()
                            }
                        } catch (e: Throwable) {
                            Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
                        }
                    } else {
                        showAuthDialog(getString(R.string.auth_to_save_area)) {
                            try {
                                viewLifecycleOwner.lifecycleScope.launch {
                                    val user = db().user.select()!!
                                    if (user.savedAreas.any { savedArea -> savedArea.asJsonObject["id"].asLong == areaId.toLong() }) {
                                        api().removeSavedArea(areaId.toLong())
                                    } else {
                                        api().saveArea(areaId.toLong())
                                    }
                                    val updatedUser = api().getUser()
                                    db().transaction {
                                        db().user.delete()
                                        db().user.insert(
                                            User(
                                                id = updatedUser.id,
                                                name = updatedUser.name,
                                                roles = updatedUser.roles,
                                                savedPlaces = updatedUser.savedPlaces,
                                                savedAreas = updatedUser.savedAreas,
                                            )
                                        )
                                    }
                                    updateBookmarkIcon()
                                }
                            } catch (e: Throwable) {
                                Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG)
                                    .show()
                            }
                        }
                    }
                }
            }

            true
        }

        initInsets(binding.root)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val area = withContext(Dispatchers.IO) {
                    api().getArea(areaId)
                }
                binding.toolbar.title = area.name
                binding.icon.isVisible = area.icon != null
                binding.icon.load(area.iconWide ?: area.icon)
                binding.description.isVisible = area.description != null
                binding.description.text = area.description
                binding.website.text = (if (areaId == "671") "https://btcmap.org/phuket" else area.websiteUrl)
                    .replace("https://", "")
                    .replace("http://", "")
                    .trimEnd('/')
                updateBookmarkIcon()
                loadUpcomingEvents()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initInsets(root: View) {
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.top
            }
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun updateBookmarkIcon() {
        if (prefs.authorized) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val user = db().user.select()!!
                    val saved =
                        user.savedAreas.any { it.asJsonObject["id"].asLong == areaId.toLong() }
                    withResumed {
                        binding.toolbar.menu.findItem(R.id.save).setIcon(
                            if (saved) R.drawable.icon_bookmark_check else R.drawable.icon_bookmark
                        )
                    }
                } catch (e: Throwable) {
                    Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
                }
            }
        } else {
            binding.toolbar.menu.findItem(R.id.save).setIcon(R.drawable.icon_bookmark)
        }
    }

    private fun loadUpcomingEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val events = withContext(Dispatchers.IO) {
                    db().event.selectByAreaId(areaId.toLong())
                }
                val now = ZonedDateTime.now(ZoneId.systemDefault())
                val upcomingOccurrences = events.flatMap { event ->
                    val dates = if (event.cronSchedule != null) {
                        CronUtils.nextExecutions(event.cronSchedule, 5, from = now)
                    } else if (event.startsAt.isAfter(now)) {
                        listOf(event.startsAt)
                    } else {
                        emptyList()
                    }
                    dates.map { event to it }
                }.sortedBy { it.second }

                if (upcomingOccurrences.isEmpty()) return@launch

                val container = binding.upcomingEventsContainer
                val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d 'at' HH:mm")

                for ((event, date) in upcomingOccurrences) {
                        val itemBlock = LinearLayout(requireContext()).apply {
                            orientation = LinearLayout.HORIZONTAL
                            val itemParams = ViewGroup.MarginLayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                            )
                            itemParams.bottomMargin = (16 * resources.displayMetrics.density).toInt()
                            layoutParams = itemParams
                            val pad = (16 * resources.displayMetrics.density).toInt()
                            setPadding(pad, pad, pad, pad)
                            gravity = Gravity.CENTER_VERTICAL
                            background = androidx.core.content.ContextCompat.getDrawable(
                                context,
                                R.drawable.event_card_background,
                            )
                            isClickable = true
                            isFocusable = true
                            setOnClickListener {
                                openInBrowser(event.website.toString().toUri())
                            }
                        }

                        val iconView = ImageView(requireContext()).apply {
                            setImageResource(R.drawable.icon_event)
                            val tint = com.google.android.material.color.MaterialColors.getColor(
                                this,
                                com.google.android.material.R.attr.colorSecondary,
                                0,
                            )
                            imageTintList = android.content.res.ColorStateList.valueOf(tint)
                            val iconSize = (24 * resources.displayMetrics.density).toInt()
                            val iconParams = ViewGroup.MarginLayoutParams(iconSize, iconSize)
                            val iconEnd = (12 * resources.displayMetrics.density).toInt()
                            iconParams.marginEnd = iconEnd
                            layoutParams = iconParams
                        }
                        itemBlock.addView(iconView)

                        val textBlock = LinearLayout(requireContext()).apply {
                            orientation = LinearLayout.VERTICAL
                            layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                            )
                        }

                        val nameView = TextView(requireContext()).apply {
                            text = event.name
                            setTextAppearance(com.google.android.material.R.style.TextAppearance_MaterialComponents_Headline6)
                            setPadding(0, 0, 0, 4)
                        }
                        textBlock.addView(nameView)

                        val dateView = TextView(requireContext()).apply {
                            text = date.format(dateFormatter)
                            setTextAppearance(com.google.android.material.R.style.TextAppearance_MaterialComponents_Body1)
                            setPadding(0, 4, 0, 0)
                        }
                        textBlock.addView(dateView)

                        itemBlock.addView(textBlock)

                        container.addView(itemBlock)
                }

                binding.upcomingEventsContainer.isVisible = true
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }
}