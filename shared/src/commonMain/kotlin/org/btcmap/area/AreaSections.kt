package org.btcmap.area

import kotlin.time.Clock
import kotlin.time.Instant
import org.btcmap.api.Api
import org.btcmap.api.GetEventsItem
import org.btcmap.api.getPlaceIssues
import org.btcmap.db.Database
import org.btcmap.db.table.area.Area
import org.btcmap.db.table.area.geoJsonGeometry
import org.btcmap.db.table.event.Event
import org.btcmap.db.table.event.isWithin
import org.btcmap.db.table.place.Place
import org.btcmap.place.isBoosted
import org.btcmap.place.isWithin
import org.btcmap.util.isUpcoming

/** A place issue as the area screen lists it, with the matching cached place. */
data class AreaPlaceIssue(
    val elementOsmType: String,
    val elementOsmId: Long,
    val elementName: String,
    val issueCode: String,
    /** The matching cached place's icon glyph, or null when it is not cached. */
    val placeIcon: String?,
)

/** The capped issue rows and the true total number of issues an area has. */
data class AreaIssues(
    val rows: List<AreaPlaceIssue>,
    val totalIssues: Long,
)

/**
 * Loads the area screen's content sections from the shared cache and API, so
 * every host shares one definition of what belongs to an area.
 *
 * The area-to-item link is the same bbox pre-filter plus point-in-polygon test
 * the server applies (see [org.btcmap.db.table.event.isWithin]).
 */
object AreaSections {

    /** How many boosted merchants the section lists. */
    const val BOOSTED_MERCHANTS_LIMIT = 10

    /** How many issue rows the endpoint is asked for. */
    const val PLACE_ISSUES_LIMIT = 50L

    /**
     * The area's currently boosted merchants, from the local cache so the
     * section works offline. A place is linked to the area by the same bbox
     * pre-filter and point-in-polygon test the server applies, and the boost
     * must still be active; the longest-running boost is the most prominent.
     */
    suspend fun boostedMerchants(
        db: Database,
        area: Area,
        now: Instant = Clock.System.now(),
        limit: Int = BOOSTED_MERCHANTS_LIMIT,
    ): List<Place> {
        val west = area.bboxWest ?: return emptyList()
        val south = area.bboxSouth ?: return emptyList()
        val east = area.bboxEast ?: return emptyList()
        val north = area.bboxNorth ?: return emptyList()

        val geometry = area.geoJsonGeometry()
        return db.place.selectByBounds(south, north, west, east, withBoost = true)
            .filter { it.isWithin(geometry) }
            .filter { it.isBoosted(now) }
            .sortedByDescending { it.boostedUntil }
            .take(limit)
    }

    /**
     * The area's upcoming events, from the local cache instead of
     * `GET /v4/areas/{id}/events`: the v4 events payload carries no area
     * association, so the server links an event to an area geometrically and
     * the same rule is applied to the cached area geometry here. Past events
     * are dropped and the rest are sorted soonest first.
     */
    suspend fun events(
        db: Database,
        area: Area,
        now: Instant = Clock.System.now(),
    ): List<GetEventsItem> {
        val west = area.bboxWest ?: return emptyList()
        val south = area.bboxSouth ?: return emptyList()
        val east = area.bboxEast ?: return emptyList()
        val north = area.bboxNorth ?: return emptyList()

        val geometry = area.geoJsonGeometry()
        return db.event.selectByBounds(south, north, west, east)
            .filter { it.isWithin(geometry) }
            .map { it.toGetEventsItem() }
            .filter { it.startsAt.isUpcoming(now) }
            .sortedBy { it.startsAt }
    }

    /** The area's place issues from the API, capped, with the true total. */
    suspend fun placeIssues(
        api: Api,
        db: Database,
        areaId: Long,
        limit: Long = PLACE_ISSUES_LIMIT,
    ): AreaIssues {
        val response = api.getPlaceIssues(areaId, limit)
        val places = db.place.selectByOsmIds(
            response.requestedIssues.map { "${it.elementOsmType}:${it.elementOsmId}" }.toSet(),
        )
        val rows = response.requestedIssues.map { issue ->
            AreaPlaceIssue(
                elementOsmType = issue.elementOsmType,
                elementOsmId = issue.elementOsmId,
                elementName = issue.elementName,
                issueCode = issue.issueCode,
                placeIcon = places["${issue.elementOsmType}:${issue.elementOsmId}"]?.icon,
            )
        }

        return AreaIssues(rows = rows, totalIssues = response.totalIssues.toLong())
    }
}

private fun Event.toGetEventsItem(): GetEventsItem {
    return GetEventsItem(
        id = id,
        lat = lat,
        lon = lon,
        name = name,
        website = website,
        startsAt = startsAt,
        endsAt = endsAt,
    )
}

private val PARAGRAPH_SEPARATOR = Regex("\n\\s*\n")

/** Splits a description into its trimmed, non-empty paragraphs. */
fun descriptionParagraphs(description: String?): List<String> {
    return description
        ?.split(PARAGRAPH_SEPARATOR)
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        .orEmpty()
}

/** The website URL as it is shown next to its icon. */
fun websiteDisplayText(websiteUrl: String): String {
    return websiteUrl.replace("https://", "").replace("http://", "").trimEnd('/')
}
