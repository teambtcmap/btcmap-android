package org.btcmap.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.R
import org.btcmap.auth.registerAuthResultListener
import org.btcmap.auth.showAuthDialog
import org.btcmap.databinding.SettingsFragmentBinding
import org.btcmap.db
import org.btcmap.dbstats.DbStatsFragment
import org.btcmap.imagestats.ImageStatsFragment
import org.btcmap.ui.SettingsItem

class SettingsFragment : Fragment() {

    private var _binding: SettingsFragmentBinding? = null
    private val binding get() = _binding!!

    /**
     * The account row's strings. Read off the main thread by [updateAccountUi],
     * so they are cached here and folded into the row list by [refreshItems].
     */
    private var accountTitle = ""
    private var accountSecondary = ""

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

        binding.settingsList.onItemClick = ::onItemClick
        binding.settingsList.onItemCheckedChange = ::onItemCheckedChange

        if (prefs.authorized) {
            accountTitle = ""
            accountSecondary = ""
        } else {
            accountTitle = getString(R.string.not_logged_in)
            accountSecondary = getString(R.string.create_account)
        }

        updateAccountUi()
        registerAuthResultListener { updateAccountUi() }
        refreshItems()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /** Rebuilds the row list from the current settings. */
    private fun refreshItems() {
        _binding ?: return

        binding.settingsList.items = listOf(
            SettingsItem.Action("account", accountTitle, accountSecondary),
            SettingsItem.Action(
                "mapStyle",
                getString(R.string.map_style),
                prefs.mapStyle.name(requireContext()),
            ),
            SettingsItem.Action(
                "customizeColors",
                getString(R.string.customize_colors),
                getString(R.string.customize_colors_secondary),
            ),
            SettingsItem.Action(
                "verifiedFilter",
                getString(R.string.verified_filter),
                prefs.verifiedFilterYears.toVerifiedFilterYears(requireContext()),
            ),
            SettingsItem.Toggle(
                "showAttribution",
                getString(R.string.show_attribution),
                getString(R.string.show_attribution_secondary),
                prefs.showAttribution,
            ),
            SettingsItem.Toggle(
                "mapRotation",
                getString(R.string.map_rotation),
                getString(R.string.map_rotation_secondary),
                prefs.mapRotationEnabled,
            ),
            SettingsItem.Action(
                "dbStats",
                getString(R.string.database_stats),
                getString(R.string.database_stats_secondary),
            ),
            SettingsItem.Action(
                "imageStats",
                getString(R.string.image_stats),
                getString(R.string.image_stats_secondary),
            ),
        )
    }

    private fun onItemClick(key: String) {
        when (key) {
            "account" -> {
                if (prefs.authorized) {
                    open { replace<UserProfileFragment>(R.id.fragmentContainerView, null) }
                } else {
                    showAuthDialog()
                }
            }
            "mapStyle" -> showMapStyleDialog()
            "customizeColors" -> open { replace<ColorSettingsFragment>(R.id.fragmentContainerView, null) }
            "verifiedFilter" -> showVerifiedFilterDialog()
            "dbStats" -> open { replace<DbStatsFragment>(R.id.fragmentContainerView, null) }
            "imageStats" -> open { replace<ImageStatsFragment>(R.id.fragmentContainerView, null) }
        }
    }

    private fun onItemCheckedChange(key: String, checked: Boolean) {
        when (key) {
            "showAttribution" -> prefs.showAttribution = checked
            "mapRotation" -> prefs.mapRotationEnabled = checked
        }
        refreshItems()
    }

    private fun open(block: androidx.fragment.app.FragmentTransaction.() -> Unit) {
        parentFragmentManager.commit {
            setReorderingAllowed(true)
            block()
            addToBackStack(null)
        }
    }

    private fun showMapStyleDialog() {
        val dialog = MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.map_style)
            .setView(R.layout.map_style_dialog).show()

        val setupStyle = fun RadioButton?.(style: MapStyle) {
            if (this == null) return

            text = style.name(requireContext())
            isChecked = prefs.mapStyle == style

            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    prefs.mapStyle = style
                    refreshItems()
                    dialog.dismiss()
                }
            }
        }

        setupStyle.apply {
            invoke(dialog.findViewById(R.id.auto), MapStyle.Auto)
            invoke(dialog.findViewById(R.id.liberty), MapStyle.Liberty)
            invoke(dialog.findViewById(R.id.positron), MapStyle.Positron)
            invoke(dialog.findViewById(R.id.bright), MapStyle.Bright)
            invoke(dialog.findViewById(R.id.dark), MapStyle.Dark)
            invoke(dialog.findViewById(R.id.dark_matter), MapStyle.DarkMatter)
        }
    }

    private fun showVerifiedFilterDialog() {
        val dialog =
            MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.verified_filter)
                .setView(R.layout.verified_filter_dialog).show()

        val setupFilter = fun RadioButton?.(filter: Int) {
            if (this == null) return

            text = filter.toVerifiedFilterYears(requireContext())
            isChecked = prefs.verifiedFilterYears == filter

            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    prefs.verifiedFilterYears = filter
                    refreshItems()
                    dialog.dismiss()
                }
            }
        }

        setupFilter.apply {
            invoke(dialog.findViewById(R.id.one_year), 1)
            invoke(dialog.findViewById(R.id.two_years), 2)
            invoke(dialog.findViewById(R.id.three_years), 3)
        }
    }

    private fun updateAccountUi() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Read the cached account off the main thread and only report it as
            // signed in when a usable session token is actually stored.
            val username = withContext(Dispatchers.IO) {
                if (!prefs.authorized) {
                    null
                } else {
                    db().user.select()?.name ?: run {
                        // A token without a cached account is not a usable
                        // session, so clear it instead of leaving the account
                        // button pointing at a profile that will be dropped.
                        prefs.clearSession(db())
                        null
                    }
                }
            }

            _binding ?: return@launch

            if (username != null) {
                accountTitle = getString(R.string.logged_in_as, username)
                accountSecondary = getString(R.string.click_to_see_your_profile)
            } else {
                accountTitle = getString(R.string.not_logged_in)
                accountSecondary = getString(R.string.create_account)
            }

            refreshItems()
        }
    }
}
