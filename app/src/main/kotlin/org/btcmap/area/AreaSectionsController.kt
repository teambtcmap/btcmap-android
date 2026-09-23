package org.btcmap.area

import android.content.res.ColorStateList
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.lifecycleScope
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.Activity
import org.btcmap.R
import org.btcmap.api
import org.btcmap.api.GetEventsItem
import org.btcmap.api.GetPlaceIssuesItem
import org.btcmap.api.getPlaceIssues
import org.btcmap.databinding.AreaFragmentBinding
import org.btcmap.databinding.ItemAreaCardBinding
import org.btcmap.db
import org.btcmap.db.table.area.Area
import org.btcmap.db.table.area.geoJsonGeometry
import org.btcmap.db.table.event.Event
import org.btcmap.db.table.event.isWithin
import org.btcmap.db.table.place.Place
import org.btcmap.event.EventFragment
import org.btcmap.event.toBundle
import org.btcmap.place.isBoosted
import org.btcmap.place.isWithin
import org.btcmap.util.iconTypeface
import org.btcmap.util.openInBrowser
import org.btcmap.util.rethrowIfCancellation
import org.btcmap.util.showError
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Renders the area screen's content sections: the area's boosted merchants, its
 * upcoming events and its places with issues.
 *
 * Kept out of [AreaFragment] so the screen only wires the area header and
 * bookkeeping. All sections build their rows from the same [ItemAreaCardBinding]
 * through [addCard], so their cards stay visually consistent.
 */
internal class AreaSectionsController(
    private val fragment: Fragment,
    private val binding: AreaFragmentBinding,
) {

    private val eventDateFormat: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

    private val boostDateFormat: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

    /**
     * Renders the area's currently boosted merchants from the local cache, so
     * the section works offline like the events one. A place is linked to the
     * area by the same bbox pre-filter and point-in-polygon test the server
     * applies, and the boost must still be active.
     */
    fun loadBoostedMerchants(area: Area) {
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            try {
                val places = withContext(Dispatchers.IO) { fetchBoostedMerchants(area) }
                renderBoostedMerchants(places)
            } catch (e: Throwable) {
                // The section is secondary: when it cannot be read, leave it
                // hidden rather than interrupting the area screen.
                e.rethrowIfCancellation()
            }
        }
    }

    private fun fetchBoostedMerchants(area: Area): List<Place> {
        val west = area.bboxWest ?: return emptyList()
        val south = area.bboxSouth ?: return emptyList()
        val east = area.bboxEast ?: return emptyList()
        val north = area.bboxNorth ?: return emptyList()

        val geometry = area.geoJsonGeometry()
        val now = ZonedDateTime.now()
        return fragment.db().place.selectByBounds(south, north, west, east, withBoost = true)
            .filter { it.isWithin(geometry) }
            .filter { it.isBoosted(now) }
            // The boost that runs longest is the most prominent, like the
            // website's boosted section.
            .sortedByDescending { it.boostedUntil }
            .take(BOOSTED_MERCHANTS_LIMIT)
    }

    private fun renderBoostedMerchants(places: List<Place>) {
        if (places.isEmpty()) return

        val container = binding.boostedMerchantsContainer
        container.removeAllViews()
        places.forEach { container.addBoostedMerchantCard(it) }
        binding.boostedMerchantsTitle.isVisible = true
        container.isVisible = true
    }

    private fun ViewGroup.addBoostedMerchantCard(place: Place) {
        addCard(
            icon = TextView(fragment.requireContext()).apply {
                typeface = iconTypeface
                text = place.icon
                setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24f)
                setTextColor(ContextCompat.getColor(context, R.color.bitcoin_orange))
                gravity = Gravity.CENTER
            },
            title = place.name.orEmpty(),
            titleVisible = !place.name.isNullOrBlank(),
            subtitle = boostSubtitle(place.boostedUntil),
        ) {
            (fragment.activity as? Activity)?.openPlace(place.id)
        }
    }

    private fun boostSubtitle(boostedUntil: ZonedDateTime?): String {
        val date = boostedUntil?.format(boostDateFormat)
            ?: return fragment.getString(R.string.boosted)
        return fragment.getString(R.string.boosted_until_s, date)
    }

    fun loadEvents(area: Area) {
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            try {
                val events = withContext(Dispatchers.IO) { fetchAreaEvents(area) }
                renderUpcomingEvents(events)
            } catch (e: Throwable) {
                e.rethrowIfCancellation()
                fragment.showError(e)
            }
        }
    }

    fun loadPlaceIssues(areaId: Long) {
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            try {
                val (rows, totalIssues) = withContext(Dispatchers.IO) { fetchPlaceIssues(areaId) }
                renderPlaceIssues(rows, totalIssues)
            } catch (e: Throwable) {
                // The issues list is secondary: when it cannot be fetched
                // (typically offline) leave the section hidden rather than
                // interrupting the area screen, which is fully usable offline.
                e.rethrowIfCancellation()
            }
        }
    }

    /**
     * Reads the area's upcoming events from the local cache instead of calling
     * `GET /v4/areas/{id}/events`.
     *
     * The v4 events payload does not carry `area_id`, so the association cannot
     * be looked up by column. The server links an event to an area by
     * pre-filtering on the area's bbox and then running a point-in-polygon test
     * against its GeoJSON; the same rule is applied here to the cached area
     * geometry, mirroring [org.btcmap.map.MapAreasController].
     */
    private fun fetchAreaEvents(area: Area): List<GetEventsItem> {
        val west = area.bboxWest ?: return emptyList()
        val south = area.bboxSouth ?: return emptyList()
        val east = area.bboxEast ?: return emptyList()
        val north = area.bboxNorth ?: return emptyList()

        val geometry = area.geoJsonGeometry()
        return fragment.db().event.selectByBounds(south, north, west, east)
            .filter { it.isWithin(geometry) }
            .map { it.toGetEventsItem() }
    }

    private fun Event.toGetEventsItem(): GetEventsItem {
        return GetEventsItem(
            id = id,
            areaId = areaId,
            lat = lat,
            lon = lon,
            name = name,
            website = website,
            startsAt = startsAt,
            endsAt = endsAt,
        )
    }

    private fun renderUpcomingEvents(events: List<GetEventsItem>) {
        val sorted = upcomingEvents(events, ZonedDateTime.now(ZoneId.systemDefault()))

        if (sorted.isEmpty()) return

        val container = binding.upcomingEventsContainer
        container.removeAllViews()
        sorted.forEach { container.addEventCard(it) }
        binding.eventsTitle.isVisible = true
        container.isVisible = true
    }

    private fun ViewGroup.addEventCard(event: GetEventsItem) {
        addCard(
            icon = ImageView(fragment.requireContext()).apply {
                setImageResource(R.drawable.icon_event)
                imageTintList = ColorStateList.valueOf(
                    MaterialColors.getColor(
                        this,
                        com.google.android.material.R.attr.colorSecondary,
                        0,
                    )
                )
            },
            title = event.name,
            subtitle = event.startsAt.format(eventDateFormat),
        ) {
            fragment.parentFragmentManager.commit {
                setReorderingAllowed(true)
                replace<EventFragment>(R.id.fragmentContainerView, null, event.toBundle())
                addToBackStack(null)
            }
        }
    }

    private data class IssueRow(
        val issue: GetPlaceIssuesItem,
        val place: Place?,
    )

    private suspend fun fetchPlaceIssues(areaId: Long): Pair<List<IssueRow>, Int> {
        val response = fragment.api().getPlaceIssues(areaId, PLACE_ISSUES_LIMIT)
        val places = fragment.db().place.selectByOsmIds(
            response.requestedIssues.map { "${it.elementOsmType}:${it.elementOsmId}" }.toSet(),
        )
        val rows = response.requestedIssues.map { issue ->
            IssueRow(
                issue = issue,
                place = places["${issue.elementOsmType}:${issue.elementOsmId}"],
            )
        }

        return rows to response.totalIssues
    }

    private fun renderPlaceIssues(rows: List<IssueRow>, totalIssues: Int) {
        if (rows.isEmpty()) return

        val density = fragment.resources.displayMetrics.density
        val container = binding.issuesContainer

        val topMarginDp = if (binding.upcomingEventsContainer.isVisible) 8 else 24
        (binding.issuesHeader.layoutParams as ViewGroup.MarginLayoutParams).topMargin =
            (topMarginDp * density).toInt()

        container.removeAllViews()
        rows.forEach { container.addIssueCard(it) }

        binding.issuesTitle.text = if (rows.size < totalIssues) {
            fragment.getString(R.string.issues_d_of_d, rows.size, totalIssues)
        } else {
            fragment.getString(R.string.issues_d, totalIssues)
        }
        binding.issuesHeader.isVisible = true
        container.isVisible = true
    }

    private fun ViewGroup.addIssueCard(row: IssueRow) {
        val placeIcon = row.place?.icon?.takeIf { it.isNotBlank() }
        addCard(
            icon = TextView(fragment.requireContext()).apply {
                typeface = iconTypeface
                text = placeIcon ?: ISSUE_FALLBACK_ICON
                setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24f)
                setTextColor(
                    MaterialColors.getColor(
                        this,
                        if (placeIcon != null) {
                            com.google.android.material.R.attr.colorSecondary
                        } else {
                            android.R.attr.colorError
                        },
                        0,
                    )
                )
                gravity = Gravity.CENTER
            },
            title = row.issue.elementName,
            titleVisible = row.issue.elementName.isNotBlank(),
            subtitle = issueDescription(row.issue.issueCode),
        ) {
            fragment.openInBrowser(
                "https://www.openstreetmap.org/edit?${row.issue.elementOsmType}=${row.issue.elementOsmId}"
                    .toUri(),
            )
        }
    }

    private fun issueDescription(code: String): String {
        val description = describeIssue(code)
        return if (description.formatArg != null) {
            fragment.getString(description.resId, description.formatArg)
        } else {
            fragment.getString(description.resId)
        }
    }

    /**
     * Inflates a card for this section and fills it. Shared by the event and
     * issue rows so their icon container, title visibility and click wiring
     * cannot drift apart.
     */
    private fun ViewGroup.addCard(
        icon: View,
        title: CharSequence,
        subtitle: CharSequence,
        titleVisible: Boolean = true,
        onClick: () -> Unit,
    ) {
        val card = ItemAreaCardBinding.inflate(fragment.layoutInflater, this, true)
        card.icon.addView(
            icon,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        card.title.isVisible = titleVisible
        card.title.text = title
        card.subtitle.text = subtitle
        card.root.setOnClickListener { onClick() }
    }

    private companion object {
        const val PLACE_ISSUES_LIMIT = 50L

        const val BOOSTED_MERCHANTS_LIMIT = 10

        const val ISSUE_FALLBACK_ICON = "warning"
    }
}
