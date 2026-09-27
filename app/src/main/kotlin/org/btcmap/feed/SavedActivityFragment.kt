package org.btcmap.feed

import androidx.fragment.app.Fragment
import org.btcmap.R
import org.btcmap.db
import org.btcmap.settings.authorized
import org.btcmap.settings.prefs

class SavedActivityFragment : BaseActivityFeedTab() {

    override fun emptyMessage(): String {
        return when {
            !isLoggedIn() -> getString(R.string.activity_empty_saved_signed_out)
            hasNoSavedItems() -> getString(R.string.activity_empty_saved_no_items)
            else -> getString(R.string.activity_empty_saved_no_activity)
        }
    }

    // A cached user row can outlive the session (e.g. an unrecoverable token
    // leaves it behind), so authorization is the source of truth, not the row.
    private fun isLoggedIn(): Boolean = prefs.authorized

    private fun hasNoSavedItems(): Boolean {
        val user = db().user.select() ?: return true
        return user.savedAreas.isEmpty() && user.savedPlaces.isEmpty()
    }

    override fun loadScope(): ActivityScope? {
        if (!isLoggedIn()) return null
        val user = db().user.select() ?: return ActivityScope(areaIds = emptyList())
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
