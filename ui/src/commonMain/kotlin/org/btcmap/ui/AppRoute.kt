package org.btcmap.ui

import java.time.LocalDateTime
import org.btcmap.db.table.event.Event

/**
 * A screen the shared application root can show. Arguments travel with the
 * route, so the navigation stack is self-contained and a host only ever names a
 * route instead of a fragment class.
 *
 * Only the screens already moved into the root appear here; the rest are still
 * Android fragments reached through the host's `FragmentManager`, and are added
 * as they migrate.
 */
sealed interface AppRoute {

    /** The admin-only infrastructure dashboard (see `InfraDashboardScreen`). */
    data object InfraDashboard : AppRoute

    /** The event review queue (see `EventReviewScreen`). */
    data object EventReview : AppRoute

    /** The boost payment screen for a place (see `BoostScreen`). */
    data class Boost(val placeId: Long, val placeName: String) : AppRoute

    /** The pay-what-you-want comment screen for a place (see `CommentScreen`). */
    data class AddComment(val placeId: Long, val placeName: String) : AppRoute

    /** A single event, with its map, dates and website (see `EventScreen`). */
    data class EventDetails(val event: Event) : AppRoute

    /**
     * Adding a place at a map position (see `AddPlaceScreen`). The optional
     * pre-fill seeds the form when the place is submitted from a basemap POI,
     * which already knows the feature's name and OSM category.
     */
    data class AddPlace(
        val lat: Double,
        val lon: Double,
        val name: String = "",
        val category: String = "",
    ) : AppRoute

    /**
     * Adding a note at a map position (see `AddNoteScreen`). The optional
     * pre-fill seeds the note body when it is added from a basemap POI, which
     * already knows the feature's name.
     */
    data class AddNote(
        val lat: Double,
        val lon: Double,
        val text: String = "",
    ) : AppRoute

    /**
     * Adding an event at a map position (see `AddEventScreen`). The optional
     * pre-fill repeats a submitted event from the profile.
     */
    data class AddEvent(
        val lat: Double,
        val lon: Double,
        val name: String = "",
        val website: String = "",
        val startsAt: LocalDateTime? = null,
        val endsAt: LocalDateTime? = null,
    ) : AppRoute

    /** Reporting the state of a place (see `ReportPlaceScreen`). */
    data class Report(
        val placeId: Long,
        val placeName: String,
        val defaultType: String?,
    ) : AppRoute

    /** The map's customizable colors (see `ColorsPage`). */
    data object Colors : AppRoute

    /** The database stats page (see `DbStatsPage`). */
    data object DbStats : AppRoute

    /** The image cache stats page (see `ImageStatsPage`). */
    data object ImageStats : AppRoute

    /**
     * The activity feed for the areas around the map, with their names and types
     * so the filter can show the area chips (see `ActivityFeedPage`).
     */
    data class Feed(
        val areaIds: List<String>,
        val areaNames: List<String>,
        val areaTypes: List<String>,
    ) : AppRoute

    /** An area's description, offline panel and sections (see `AreaScreen`). */
    data class Area(val areaId: Long) : AppRoute

    /** The settings list (see `SettingsPage`). */
    data object Settings : AppRoute

    /**
     * The admin-only area management list (see `ManageAreasScreen`), reached from
     * the settings row shown to area admins.
     */
    data object ManageAreas : AppRoute

    /**
     * The admin-only recent place-image moderation list (see
     * `ManagePlaceImagesScreen`), reached from the settings row shown to admins
     * and roots.
     */
    data object ManagePlaceImages : AppRoute

    /**
     * One area's cached fields, read-only, opened from the manage-areas list
     * (see `AreaAdminScreen`).
     */
    data class AreaAdmin(val areaId: Long) : AppRoute

    /** The signed-in account page (see `ProfileScreen`). */
    data object UserProfile : AppRoute

    /** The standalone place screen, opened outside the map (see `PlaceDetails`). */
    data class Place(val placeId: Long) : AppRoute

    /** The full-bleed map, the app's root screen (see `MapScreen`). */
    data object Map : AppRoute
}

/** A stable key for a route, so a host can pass a start route through a `Bundle`. */
fun AppRoute.key(): String = when (this) {
    AppRoute.InfraDashboard -> "infra-dashboard"
    AppRoute.EventReview -> "event-review"
    is AppRoute.Boost -> "boost"
    is AppRoute.AddComment -> "add-comment"
    is AppRoute.EventDetails -> "event-details"
    is AppRoute.AddPlace -> "add-place"
    is AppRoute.AddNote -> "add-note"
    is AppRoute.AddEvent -> "add-event"
    is AppRoute.Report -> "report"
    is AppRoute.Colors -> "colors"
    is AppRoute.DbStats -> "db-stats"
    is AppRoute.ImageStats -> "image-stats"
    is AppRoute.Feed -> "feed"
    is AppRoute.Area -> "area"
    is AppRoute.Settings -> "settings"
    is AppRoute.ManageAreas -> "manage-areas"
    is AppRoute.ManagePlaceImages -> "manage-place-images"
    is AppRoute.AreaAdmin -> "area-admin"
    is AppRoute.UserProfile -> "user-profile"
    is AppRoute.Place -> "place"
    is AppRoute.Map -> "map"
}
