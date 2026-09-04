package org.btcmap.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.btcmap.R
import java.util.Locale

class ActivityFeedFragment : Fragment() {

    private var _binding: org.btcmap.databinding.ActivityFeedFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = org.btcmap.databinding.ActivityFeedFragmentBinding.inflate(
            inflater, container, false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.topAppBar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.topAppBar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.filter -> {
                    currentActiveTab()?.showFilterDialog()
                    true
                }
                else -> false
            }
        }

        val areaIds = arguments?.getStringArrayList("area_ids") ?: arrayListOf()
        val areaNames = arguments?.getStringArrayList("area_names") ?: arrayListOf()
        val areaTypes = arguments?.getStringArrayList("area_types") ?: arrayListOf()

        val tabs = listOf(
            ActivityFeedPagerAdapter.TabSpec(
                title = getString(R.string.activity_tab_local),
                factory = {
                    LocalActivityFragment.create(
                        areaIds.toList(),
                        areaNames.toList(),
                        areaTypes.toList(),
                    )
                },
            ),
            ActivityFeedPagerAdapter.TabSpec(
                title = getString(R.string.activity_tab_following),
                factory = { FollowingActivityFragment.create() },
            ),
        )

        binding.pager.adapter = ActivityFeedPagerAdapter(requireActivity(), tabs)
        binding.pager.offscreenPageLimit = 1
        binding.tabs.tabMode = TabLayout.MODE_FIXED

        TabLayoutMediator(binding.tabs, binding.pager) { tab, position ->
            tab.text = tabs[position].title.uppercase(Locale.getDefault())
        }.attach()
    }

    private fun currentLocalFragment(): LocalActivityFragment? {
        return requireActivity().supportFragmentManager.fragments
            .filterIsInstance<LocalActivityFragment>()
            .firstOrNull()
    }

    private fun currentFollowingFragment(): FollowingActivityFragment? {
        return requireActivity().supportFragmentManager.fragments
            .filterIsInstance<FollowingActivityFragment>()
            .firstOrNull()
    }

    private fun currentActiveTab(): BaseActivityFeedTab? {
        return when (binding.pager.currentItem) {
            0 -> currentLocalFragment()
            1 -> currentFollowingFragment()
            else -> null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
