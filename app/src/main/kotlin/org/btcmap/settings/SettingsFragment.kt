package org.btcmap.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mrudultora.colorpicker.ColorPickerPopUp
import com.mrudultora.colorpicker.ColorPickerPopUp.OnPickColorListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.R
import org.btcmap.auth.registerAuthResultListener
import org.btcmap.auth.showAuthDialog
import org.btcmap.databinding.SettingsFragmentBinding
import org.btcmap.db

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

        binding.useAdaptiveColors.isChecked = prefs.useAdaptiveColors
        binding.useAdaptiveColors.setOnCheckedChangeListener { _, isChecked ->
            prefs.useAdaptiveColors = isChecked
            refreshAllColors()
        }

        binding.showAttribution.isChecked = prefs.showAttribution
        binding.showAttribution.setOnCheckedChangeListener { _, isChecked ->
            prefs.showAttribution = isChecked
        }

        binding.mapRotation.isChecked = prefs.mapRotationEnabled
        binding.mapRotation.setOnCheckedChangeListener { _, isChecked ->
            prefs.mapRotationEnabled = isChecked
        }

        binding.showDebugInfo.isChecked = prefs.showDebugInfo
        binding.showDebugInfo.setOnCheckedChangeListener { _, isChecked ->
            prefs.showDebugInfo = isChecked
        }

        initMarkerBackgroundButton()
        initMarkerIconButton()

        initBoostedMarkerBackgroundButton()

        binding.badgeBackgroundColor.setColorHex(prefs.badgeBackgroundColor(requireContext()))

        binding.changeBadgeBackgroundColor.setOnClickListener {
            val colorPickerPopUp = ColorPickerPopUp(context)
            colorPickerPopUp.setShowAlpha(true)
                .setDefaultColor(prefs.badgeBackgroundColor(requireContext()))
                .setOnPickColorListener(object : OnPickColorListener {
                    override fun onColorPicked(color: Int) {
                        prefs.setBadgeBackgroundColor(color)
                    }

                    override fun onCancel() {
                        colorPickerPopUp.dismissDialog() // Dismiss the dialog.
                    }
                })
                .show()
            colorPickerPopUp.negativeButton.setOnClickListener {
                prefs.setBadgeBackgroundColor(null)
                binding.badgeBackgroundColor.setColorHex(prefs.badgeBackgroundColor(requireContext()))
                colorPickerPopUp.dismissDialog()
            }
        }

        binding.badgeTextColor.setColorHex(prefs.badgeTextColor(requireContext()))

        binding.changeBadgeTextColor.setOnClickListener {
            val colorPickerPopUp = ColorPickerPopUp(context)
            colorPickerPopUp.setShowAlpha(true)
                .setDefaultColor(prefs.badgeTextColor(requireContext()))
                .setOnPickColorListener(object : OnPickColorListener {
                    override fun onColorPicked(color: Int) {
                        prefs.setBadgeTextColor(color)
                    }

                    override fun onCancel() {
                        colorPickerPopUp.dismissDialog() // Dismiss the dialog.
                    }
                })
                .show()
            colorPickerPopUp.negativeButton.setOnClickListener {
                prefs.setBadgeTextColor(null)
                binding.badgeTextColor.setColorHex(prefs.badgeTextColor(requireContext()))
                colorPickerPopUp.dismissDialog()
            }
        }

        binding.buttonBackgroundColor.setColorHex(prefs.buttonBackgroundColor(requireContext()))

        binding.changeButtonBackgroundColor.setOnClickListener {
            val colorPickerPopUp = ColorPickerPopUp(context)
            colorPickerPopUp.setShowAlpha(true)
                .setDefaultColor(prefs.buttonBackgroundColor(requireContext()))
                .setOnPickColorListener(object : OnPickColorListener {
                    override fun onColorPicked(color: Int) {
                        prefs.setButtonBackgroundColor(color)
                    }

                    override fun onCancel() {
                        colorPickerPopUp.dismissDialog() // Dismiss the dialog.
                    }
                })
                .show()
            colorPickerPopUp.negativeButton.setOnClickListener {
                prefs.setButtonBackgroundColor(null)
                binding.buttonBackgroundColor.setColorHex(prefs.buttonBackgroundColor(requireContext()))
                colorPickerPopUp.dismissDialog()
            }
        }

        binding.buttonIconColor.setColorHex(prefs.buttonIconColor(requireContext()))

        binding.changeButtonIconColor.setOnClickListener {
            val colorPickerPopUp = ColorPickerPopUp(context)
            colorPickerPopUp.setShowAlpha(true)
                .setDefaultColor(prefs.buttonIconColor(requireContext()))
                .setOnPickColorListener(object : OnPickColorListener {
                    override fun onColorPicked(color: Int) {
                        prefs.setButtonIconColor(color)
                    }

                    override fun onCancel() {
                        colorPickerPopUp.dismissDialog() // Dismiss the dialog.
                    }
                })
                .show()
            colorPickerPopUp.negativeButton.setOnClickListener {
                prefs.setButtonIconColor(null)
                binding.buttonIconColor.setColorHex(prefs.buttonIconColor(requireContext()))
                colorPickerPopUp.dismissDialog()
            }
        }

        binding.buttonBorderColor.setColorHex(prefs.buttonBorderColor(requireContext()))

        binding.changeButtonBorderColor.setOnClickListener {
            val colorPickerPopUp = ColorPickerPopUp(context)
            colorPickerPopUp.setShowAlpha(true)
                .setDefaultColor(prefs.buttonBorderColor(requireContext()))
                .setOnPickColorListener(object : OnPickColorListener {
                    override fun onColorPicked(color: Int) {
                        prefs.setButtonBorderColor(color)
                    }

                    override fun onCancel() {
                        colorPickerPopUp.dismissDialog() // Dismiss the dialog.
                    }
                })
                .show()
            colorPickerPopUp.negativeButton.setOnClickListener {
                prefs.setButtonBorderColor(null)
                binding.buttonBorderColor.setColorHex(prefs.buttonBorderColor(requireContext()))
                colorPickerPopUp.dismissDialog()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun TextView.setColorHex(color: Int) {
        text = context.getString(R.string.color_hex, color.toHexString())
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
                invoke(dialog.findViewById(R.id.carto_dark_matter), MapStyle.CartoDarkMatter)
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

    private fun initMarkerBackgroundButton() {
        binding.markerBackgroundColor.setColorHex(prefs.markerBackgroundColor(requireContext()))
        binding.markerBackgroundColor.setTextColor(prefs.markerBackgroundColor(requireContext()))

        binding.changeMarkerBackgroundColor.setOnClickListener {
            val colorPickerPopUp = ColorPickerPopUp(context)
            colorPickerPopUp.setShowAlpha(true)
                .setDefaultColor(prefs.markerBackgroundColor(requireContext()))
                .setOnPickColorListener(object : OnPickColorListener {
                    override fun onColorPicked(color: Int) {
                        prefs.setMarkerBackgroundColor(color)
                        binding.markerBackgroundColor.setTextColor(color)
                    }

                    override fun onCancel() {
                        colorPickerPopUp.dismissDialog()
                    }
                })
                .show()
            colorPickerPopUp.negativeButton.setText(R.string.reset)
            colorPickerPopUp.negativeButton.setOnClickListener {
                prefs.setMarkerBackgroundColor(null)
                binding.markerBackgroundColor.setColorHex(prefs.markerBackgroundColor(requireContext()))
                binding.markerBackgroundColor.setTextColor(
                    prefs.markerBackgroundColor(
                        requireContext()
                    )
                )
                colorPickerPopUp.dismissDialog()
            }
        }
    }

    private fun initMarkerIconButton() {
        binding.markerIconColor.setColorHex(prefs.markerIconColor(requireContext()))
        binding.markerIconColor.setTextColor(prefs.markerIconColor(requireContext()))

        binding.changeMarkerIconColor.setOnClickListener {
            val colorPickerPopUp = ColorPickerPopUp(context)
            colorPickerPopUp.setShowAlpha(true)
                .setDefaultColor(prefs.markerIconColor(requireContext()))
                .setOnPickColorListener(object : OnPickColorListener {
                    override fun onColorPicked(color: Int) {
                        prefs.setMarkerIconColor(color)
                        binding.markerIconColor.setTextColor(color)
                    }

                    override fun onCancel() {
                        colorPickerPopUp.dismissDialog()
                    }
                })
                .show()
            colorPickerPopUp.negativeButton.setText(R.string.reset)
            colorPickerPopUp.negativeButton.setOnClickListener {
                prefs.setMarkerIconColor(null)
                binding.markerIconColor.setColorHex(prefs.markerIconColor(requireContext()))
                binding.markerIconColor.setTextColor(prefs.markerIconColor(requireContext()))
                colorPickerPopUp.dismissDialog()
            }
        }
    }

    private fun initBoostedMarkerBackgroundButton() {
        binding.boostedMarkerBackgroundColor.setColorHex(prefs.boostedMarkerBackgroundColor())

        binding.changeBoostedMarkerBackgroundColor.setOnClickListener {
            val colorPickerPopUp = ColorPickerPopUp(context)
            colorPickerPopUp.setShowAlpha(true)
                .setDefaultColor(prefs.boostedMarkerBackgroundColor())
                .setOnPickColorListener(object : OnPickColorListener {
                    override fun onColorPicked(color: Int) {
                        prefs.setBoostedMarkerBackgroundColor(color)
                    }

                    override fun onCancel() {
                        colorPickerPopUp.dismissDialog() // Dismiss the dialog.
                    }
                })
                .show()
            colorPickerPopUp.negativeButton.setText(R.string.reset)
            colorPickerPopUp.negativeButton.setOnClickListener {
                prefs.setBoostedMarkerBackgroundColor(null)
                binding.boostedMarkerBackgroundColor.setColorHex(prefs.boostedMarkerBackgroundColor())
                colorPickerPopUp.dismissDialog()
            }
        }
    }

    private fun refreshAllColors() {
        binding.markerBackgroundColor.setColorHex(prefs.markerBackgroundColor(requireContext()))
        binding.markerBackgroundColor.setTextColor(prefs.markerBackgroundColor(requireContext()))
        binding.markerIconColor.setColorHex(prefs.markerIconColor(requireContext()))
        binding.markerIconColor.setTextColor(prefs.markerIconColor(requireContext()))
        binding.badgeBackgroundColor.setColorHex(prefs.badgeBackgroundColor(requireContext()))
        binding.badgeBackgroundColor.setTextColor(prefs.badgeBackgroundColor(requireContext()))
        binding.badgeTextColor.setColorHex(prefs.badgeTextColor(requireContext()))
        binding.badgeTextColor.setTextColor(prefs.badgeBackgroundColor(requireContext()))
        binding.buttonBackgroundColor.setColorHex(prefs.buttonBackgroundColor(requireContext()))
        binding.buttonBackgroundColor.setTextColor(prefs.buttonBackgroundColor(requireContext()))
        binding.buttonIconColor.setColorHex(prefs.buttonIconColor(requireContext()))
        binding.buttonIconColor.setTextColor(prefs.buttonIconColor(requireContext()))
        binding.buttonBorderColor.setColorHex(prefs.buttonBorderColor(requireContext()))
        binding.buttonBorderColor.setTextColor(prefs.buttonBorderColor(requireContext()))
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