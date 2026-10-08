package org.btcmap.sync

/**
 * A table the app-scoped sync changed, emitted once the step that touched it
 * finishes.
 *
 * The sync is app-scoped, so a screen that merely displays the data cannot
 * drive it and instead reacts to what actually changed: the map rebuilds its
 * viewport cache for the affected table, or reloads the area chips.
 */
sealed interface SyncEvent {
    data object PlacesChanged : SyncEvent
    data object EventsChanged : SyncEvent
    data object CommentsChanged : SyncEvent
    data object AreasChanged : SyncEvent

    /**
     * The signed-in user's cached personal notes changed, so the map redraws its
     * note layer.
     */
    data object NotesChanged : SyncEvent

    /**
     * The signed-in profile changed on the server: a role was granted or
     * revoked, the account was renamed, or its saved items changed. Emitted so
     * the role-gated UI re-derives its admin and event-manager affordances
     * without a re-login.
     */
    data object UserChanged : SyncEvent
}
