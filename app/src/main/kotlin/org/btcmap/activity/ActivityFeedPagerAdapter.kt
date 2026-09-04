package org.btcmap.activity

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ActivityFeedPagerAdapter(
    activity: FragmentActivity,
    private val tabs: List<TabSpec>,
) : FragmentStateAdapter(activity) {

    data class TabSpec(val title: String, val factory: () -> Fragment)

    override fun getItemCount(): Int = tabs.size

    override fun createFragment(position: Int): Fragment = tabs[position].factory()
}
