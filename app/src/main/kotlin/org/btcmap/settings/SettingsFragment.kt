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

        updateAccountUi()
        registerAuthResultListener { updateAccountUi() }

        binding.accountButton.setOnClickListener {
            if (prefs.authorized) {
                parentFragmentManager.commit {
                    setReorderingAllowed(true)
                    replace<UserProfileFragment>(R.id.fragmentContainerView, null)
                    addToBackStack(null)
                }
            } else {
                showAuthDialog()
            }
        }

        initMapStyleButton()
        initVerifiedFilterButton()

        binding.showAttribution.isChecked = prefs.showAttribution
        binding.showAttribution.setOnCheckedChangeListener { _, isChecked ->
            prefs.showAttribution = isChecked
        }

        binding.mapRotation.isChecked = prefs.mapRotationEnabled
        binding.mapRotation.setOnCheckedChangeListener { _, isChecked ->
            prefs.mapRotationEnabled = isChecked
        }

        binding.dbStatsButton.setOnClickListener {
            parentFragmentManager.commit {
                setReorderingAllowed(true)
                replace<DbStatsFragment>(R.id.fragmentContainerView, null)
                addToBackStack(null)
            }
        }

        binding.imageStatsButton.setOnClickListener {
            parentFragmentManager.commit {
                setReorderingAllowed(true)
                replace<ImageStatsFragment>(R.id.fragmentContainerView, null)
                addToBackStack(null)
            }
        }

        binding.customizeColorsButton.setOnClickListener {
            parentFragmentManager.commit {
                setReorderingAllowed(true)
                replace<ColorSettingsFragment>(R.id.fragmentContainerView, null)
                addToBackStack(null)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initMapStyleButton() {
        binding.currentMapStyle.text = prefs.mapStyle.name(requireContext())

        binding.mapStyleButton.setOnClickListener {
            val dialog = MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.map_style)
                .setView(R.layout.map_style_dialog).show()

            val setupInterval = fun RadioButton?.(style: MapStyle) {
                if (this == null) return

                text = style.name(requireContext())
                isChecked = prefs.mapStyle == style

                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        prefs.mapStyle = style
                        binding.currentMapStyle.text = text
                        dialog.dismiss()
                    }
                }
            }

            setupInterval.apply {
                invoke(dialog.findViewById(R.id.auto), MapStyle.Auto)
                invoke(dialog.findViewById(R.id.liberty), MapStyle.Liberty)
                invoke(dialog.findViewById(R.id.positron), MapStyle.Positron)
                invoke(dialog.findViewById(R.id.bright), MapStyle.Bright)
                invoke(dialog.findViewById(R.id.dark), MapStyle.Dark)
            }
        }
    }

    private fun initVerifiedFilterButton() {
        binding.currentVerifiedFilter.text =
            prefs.verifiedFilterYears.toVerifiedFilterYears(requireContext())

        binding.verifiedFilterButton.setOnClickListener {
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
                        binding.currentVerifiedFilter.text = text
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

            if (username != null) {
                binding.accountStatus.text = getString(R.string.logged_in_as, username)
                binding.accountSecondary.text = getString(R.string.click_to_see_your_profile)
            } else {
                binding.accountStatus.text = getString(R.string.not_logged_in)
                binding.accountSecondary.text = getString(R.string.create_account)
            }
            binding.accountSecondary.visibility = View.VISIBLE
        }
    }
}