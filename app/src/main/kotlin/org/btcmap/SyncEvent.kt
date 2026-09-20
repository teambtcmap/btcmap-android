package org.btcmap

/**
 * A table the app-scoped sync changed, emitted once the step that touched it
 * finishes.
 *
 * The sync is app-scoped, so a screen that merely displays the data cannot
 * drive it and instead reacts to what actually changed: the map rebuilds its
 * viewport cache for the affected table, or reloads the area chips.
 */
internal sealed interface SyncEvent {
    data object PlacesChanged : SyncEvent
    data object EventsChanged : SyncEvent
    data object CommentsChanged : SyncEvent
    data object AreasChanged : SyncEvent
}
