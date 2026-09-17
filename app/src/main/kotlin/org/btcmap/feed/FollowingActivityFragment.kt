package org.btcmap.feed

import androidx.fragment.app.Fragment
import org.btcmap.db
import org.btcmap.settings.authorized
import org.btcmap.settings.prefs

class FollowingActivityFragment : BaseActivityFeedTab() {

    override fun emptyMessage(): String {
        return when {
            !isLoggedIn() -> "Sign in to see activity from your saved areas"
            savedAreaIds().isEmpty() -> "Save some areas to follow their activity"
            else -> "No activity in your followed areas"
        }
    }

    // A cached user row can outlive the session (e.g. an unrecoverable token
    // leaves it behind), so authorization is the source of truth, not the row.
    private fun isLoggedIn(): Boolean = prefs.authorized

    private fun savedAreaIds(): List<String> {
        val user = db().user.select() ?: return emptyList()
        return user.savedAreas.map { it.id.toString() }
    }

    override fun loadAreaIds(): List<String>? {
        if (!isLoggedIn()) return null
        val ids = savedAreaIds()
        return if (ids.isEmpty()) emptyList() else ids
    }

    override fun onResume() {
        super.onResume()
        loadActivity()
    }

    companion object {
        fun create(): Fragment = FollowingActivityFragment()
    }
}
