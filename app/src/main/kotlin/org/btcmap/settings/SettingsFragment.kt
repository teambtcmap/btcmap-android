package org.btcmap.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import org.btcmap.R
import org.btcmap.auth.registerAuthResultListener
import org.btcmap.auth.showAuthDialog
import org.btcmap.databinding.SettingsFragmentBinding
import org.btcmap.db
import org.btcmap.dbstats.DbStatsFragment
import org.btcmap.imagestats.ImageStatsFragment
import org.btcmap.ui.SettingsPageLabels

/**
 * The settings screen, hosted by the shared [org.btcmap.ui.SettingsPage]. The
 * page owns the rows, the toggles and the picker dialogs; this fragment only
 * supplies the labels and the navigation the rows trigger.
 */
class SettingsFragment : Fragment() {

    private var _binding: SettingsFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = SettingsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.topAppBar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.settingsList.apply {
            settings = prefs
            database = db()
            labels = pageLabels()
            onOpenAccount = {
                if (prefs.authorized) {
                    open { replace<UserProfileFragment>(R.id.fragmentContainerView, null) }
                } else {
                    showAuthDialog()
                }
            }
            onOpenColors = {
                open { replace<ColorSettingsFragment>(R.id.fragmentContainerView, null) }
            }
            onOpenDbStats = {
                open { replace<DbStatsFragment>(R.id.fragmentContainerView, null) }
            }
            onOpenImageStats = {
                open { replace<ImageStatsFragment>(R.id.fragmentContainerView, null) }
            }
        }

        // A sign-in returns to this screen without recreating it, so the account
        // row has to be told to re-read the cached user.
        registerAuthResultListener { binding.settingsList.reloadKey++ }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun pageLabels(): SettingsPageLabels = SettingsPageLabels(
        account = getString(R.string.not_logged_in),
        logIn = getString(R.string.create_account),
        loggedInAs = { getString(R.string.logged_in_as, it) },
        openProfile = getString(R.string.click_to_see_your_profile),
        mapStyle = getString(R.string.map_style),
        mapStyleValue = { it.name(requireContext()) },
        customizeColors = getString(R.string.customize_colors),
        customizeColorsSecondary = getString(R.string.customize_colors_secondary),
        verifiedFilter = getString(R.string.verified_filter),
        verifiedFilterValue = { it.toVerifiedFilterYears(requireContext()) },
        verifiedFilterYears = listOf(1, 2, 3),
        showAttribution = getString(R.string.show_attribution),
        showAttributionSecondary = getString(R.string.show_attribution_secondary),
        mapRotation = getString(R.string.map_rotation),
        mapRotationSecondary = getString(R.string.map_rotation_secondary),
        dbStats = getString(R.string.database_stats),
        dbStatsSecondary = getString(R.string.database_stats_secondary),
        imageStats = getString(R.string.image_stats),
        imageStatsSecondary = getString(R.string.image_stats_secondary),
        mapStyleDialogTitle = getString(R.string.map_style),
        verifiedFilterDialogTitle = getString(R.string.verified_filter),
        close = getString(R.string.close),
    )

    private fun open(block: androidx.fragment.app.FragmentTransaction.() -> Unit) {
        parentFragmentManager.commit {
            setReorderingAllowed(true)
            block()
            addToBackStack(null)
        }
    }
}
