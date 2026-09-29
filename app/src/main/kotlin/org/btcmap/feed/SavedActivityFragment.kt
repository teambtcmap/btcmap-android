package org.btcmap.feed

import androidx.fragment.app.Fragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.btcmap.R
import org.btcmap.db
import org.btcmap.settings.authorized
import org.btcmap.settings.prefs

class SavedActivityFragment : BaseActivityFeedTab() {

    /**
     * Whether the cached account has no saved items, resolved off the main
     * thread by [loadScope] and read by [emptyMessage] when the tab is empty.
     * Reading the cached user in [emptyMessage] itself would touch the shared
     * SQLite connection on the main thread.
     */
    @Volatile
    private var savedItemsEmpty = false

    override fun emptyMessage(): String {
        return when {
            !isLoggedIn() -> getString(R.string.activity_empty_saved_signed_out)
            savedItemsEmpty -> getString(R.string.activity_empty_saved_no_items)
            else -> getString(R.string.activity_empty_saved_no_activity)
        }
    }

    // A cached user row can outlive the session (e.g. an unrecoverable token
    // leaves it behind), so authorization is the source of truth, not the row.
    private fun isLoggedIn(): Boolean = prefs.authorized

    override suspend fun loadScope(): ActivityScope? {
        if (!isLoggedIn()) return null
        val user = withContext(Dispatchers.IO) { db().user.select() }
        savedItemsEmpty = user == null ||
            (user.savedAreas.isEmpty() && user.savedPlaces.isEmpty())
        if (user == null) return ActivityScope(areaIds = emptyList())
        return ActivityScope(
            areaIds = user.savedAreas.map { it.id.toString() },
            placeIds = user.savedPlaces.map { it.id.toString() },
        )
    }

    override fun onResume() {
        super.onResume()
        loadActivity()
    }

    companion object {
        fun create(): Fragment = SavedActivityFragment()
    }
}
