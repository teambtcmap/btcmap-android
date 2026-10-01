package org.btcmap.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
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
import org.btcmap.ui.RadioOption
import org.btcmap.ui.RadioPickerComposeView
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
        val options = MapStyle.entries.map { style ->
            RadioOption(key = style.name, label = style.name(requireContext()))
        }
        showRadioPickerDialog(
            titleRes = R.string.map_style,
            options = options,
            selectedKey = prefs.mapStyle.name,
        ) { key ->
            MapStyle.entries.firstOrNull { it.name == key }?.let { prefs.mapStyle = it }
            refreshItems()
        }
    }

    private fun showVerifiedFilterDialog() {
        val options = listOf(1, 2, 3).map { years ->
            RadioOption(key = years.toString(), label = years.toVerifiedFilterYears(requireContext()))
        }
        showRadioPickerDialog(
            titleRes = R.string.verified_filter,
            options = options,
            selectedKey = prefs.verifiedFilterYears.toString(),
        ) { key ->
            key.toIntOrNull()?.let { prefs.verifiedFilterYears = it }
            refreshItems()
        }
    }

    /**
     * Shows a titled radio picker. The content is a Compose view, so the dialog
     * window is given the view-tree owners it would otherwise miss; a selection
     * applies the setting and dismisses.
     */
    private fun showRadioPickerDialog(
        @StringRes titleRes: Int,
        options: List<RadioOption>,
        selectedKey: String?,
        onSelect: (key: String) -> Unit,
    ) {
        val view = RadioPickerComposeView(requireContext()).apply {
            setViewTreeLifecycleOwner(this@SettingsFragment)
            setViewTreeSavedStateRegistryOwner(this@SettingsFragment)
            setViewTreeViewModelStoreOwner(this@SettingsFragment)
            this.options = options
            this.selectedKey = selectedKey
        }
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(titleRes)
            .setView(view)
            .create()
        view.onSelect = { key ->
            onSelect(key)
            dialog.dismiss()
        }
        dialog.show()
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
