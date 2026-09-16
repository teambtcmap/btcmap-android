package org.btcmap.feed

import androidx.fragment.app.Fragment
import org.btcmap.db

class FollowingActivityFragment : BaseActivityFeedTab() {

    override fun emptyMessage(): String {
        return when {
            !isLoggedIn() -> "Sign in to see activity from your saved areas"
            savedAreaIds().isEmpty() -> "Save some areas to follow their activity"
            else -> "No activity in your followed areas"
        }
    }

    private fun isLoggedIn(): Boolean = db().user.select() != null

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
