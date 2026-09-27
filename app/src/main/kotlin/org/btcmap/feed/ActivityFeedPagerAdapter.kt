package org.btcmap.feed

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * Hosts the feed's tabs in the feed fragment's own child FragmentManager, so a
 * tab belongs to the feed instance that created it and is destroyed with it.
 * Attaching them to the Activity's FragmentManager instead lets a tab outlive
 * its feed, and [ActivityFeedFragment] could then show the filter dialog of a
 * stale tab whose areas no longer match the visible list.
 */
class ActivityFeedPagerAdapter(
    fragment: Fragment,
    private val tabs: List<TabSpec>,
) : FragmentStateAdapter(fragment) {

    data class TabSpec(val title: String, val factory: () -> Fragment)

    override fun getItemCount(): Int = tabs.size

    override fun createFragment(position: Int): Fragment = tabs[position].factory()
}
