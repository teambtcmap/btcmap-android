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
import org.btcmap.dbstats.DbStatsFragment
import org.btcmap.imagestats.ImageStatsFragment
import org.btcmap.view.ColorSwatchView

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

        binding.showDebugInfo.isChecked = prefs.showDebugInfo
        binding.showDebugInfo.setOnCheckedChangeListener { _, isChecked ->
            prefs.showDebugInfo = isChecked
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

        initMarkerBackgroundButton()
        initMarkerIconButton()

        initBoostedMarkerBackgroundButton()
        initBoostedMarkerIconButton()

        setColor(
            binding.badgeBackgroundColorSwatch,
            binding.badgeBackgroundColor,
            prefs.badgeBackgroundColor(requireContext()),
        )

        binding.changeBadgeBackgroundColor.setOnClickListener {
            val colorPickerPopUp = ColorPickerPopUp(context)
            colorPickerPopUp.setShowAlpha(true)
                .setDefaultColor(prefs.badgeBackgroundColor(requireContext()))
                .setOnPickColorListener(object : OnPickColorListener {
                    override fun onColorPicked(color: Int) {
                        prefs.setBadgeBackgroundColor(color)
                        setColor(
                            binding.badgeBackgroundColorSwatch,
                            binding.badgeBackgroundColor,
                            color,
                        )
                    }

                    override fun onCancel() {
                        colorPickerPopUp.dismissDialog() // Dismiss the dialog.
                    }
                })
                .show()
            colorPickerPopUp.negativeButton.setOnClickListener {
                prefs.setBadgeBackgroundColor(null)
                setColor(
                    binding.badgeBackgroundColorSwatch,
                    binding.badgeBackgroundColor,
                    prefs.badgeBackgroundColor(requireContext()),
                )
                colorPickerPopUp.dismissDialog()
            }
        }

        setColor(
            binding.badgeTextColorSwatch,
            binding.badgeTextColor,
            prefs.badgeTextColor(requireContext()),
        )

        binding.changeBadgeTextColor.setOnClickListener {
            val colorPickerPopUp = ColorPickerPopUp(context)
            colorPickerPopUp.setShowAlpha(true)
                .setDefaultColor(prefs.badgeTextColor(requireContext()))
                .setOnPickColorListener(object : OnPickColorListener {
                    override fun onColorPicked(color: Int) {
                        prefs.setBadgeTextColor(color)
                        setColor(
                            binding.badgeTextColorSwatch,
                            binding.badgeTextColor,
                            color,
                        )
                    }

                    override fun onCancel() {
                        colorPickerPopUp.dismissDialog() // Dismiss the dialog.
                    }
                })
                .show()
            colorPickerPopUp.negativeButton.setOnClickListener {
                prefs.setBadgeTextColor(null)
                setColor(
                    binding.badgeTextColorSwatch,
                    binding.badgeTextColor,
                    prefs.badgeTextColor(requireContext()),
                )
                colorPickerPopUp.dismissDialog()
            }
        }

        setColor(
            binding.buttonBackgroundColorSwatch,
            binding.buttonBackgroundColor,
            prefs.buttonBackgroundColor(requireContext()),
        )

        binding.changeButtonBackgroundColor.setOnClickListener {
            val colorPickerPopUp = ColorPickerPopUp(context)
            colorPickerPopUp.setShowAlpha(true)
                .setDefaultColor(prefs.buttonBackgroundColor(requireContext()))
                .setOnPickColorListener(object : OnPickColorListener {
                    override fun onColorPicked(color: Int) {
                        prefs.setButtonBackgroundColor(color)
                        setColor(
                            binding.buttonBackgroundColorSwatch,
                            binding.buttonBackgroundColor,
                            color,
                        )
                    }

                    override fun onCancel() {
                        colorPickerPopUp.dismissDialog() // Dismiss the dialog.
                    }
                })
                .show()
            colorPickerPopUp.negativeButton.setOnClickListener {
                prefs.setButtonBackgroundColor(null)
                setColor(
                    binding.buttonBackgroundColorSwatch,
                    binding.buttonBackgroundColor,
                    prefs.buttonBackgroundColor(requireContext()),
                )
                colorPickerPopUp.dismissDialog()
            }
        }

        setColor(
            binding.buttonIconColorSwatch,
            binding.buttonIconColor,
            prefs.buttonIconColor(requireContext()),
        )

        binding.changeButtonIconColor.setOnClickListener {
            val colorPickerPopUp = ColorPickerPopUp(context)
            colorPickerPopUp.setShowAlpha(true)
                .setDefaultColor(prefs.buttonIconColor(requireContext()))
                .setOnPickColorListener(object : OnPickColorListener {
                    override fun onColorPicked(color: Int) {
                        prefs.setButtonIconColor(color)
                        setColor(
                            binding.buttonIconColorSwatch,
                            binding.buttonIconColor,
                            color,
                        )
                    }

                    override fun onCancel() {
                        colorPickerPopUp.dismissDialog() // Dismiss the dialog.
                    }
                })
                .show()
            colorPickerPopUp.negativeButton.setOnClickListener {
                prefs.setButtonIconColor(null)
                setColor(
                    binding.buttonIconColorSwatch,
                    binding.buttonIconColor,
                    prefs.buttonIconColor(requireContext()),
                )
                colorPickerPopUp.dismissDialog()
            }
        }

        setColor(
            binding.buttonBorderColorSwatch,
            binding.buttonBorderColor,
            prefs.buttonBorderColor(requireContext()),
        )

        binding.changeButtonBorderColor.setOnClickListener {
            val colorPickerPopUp = ColorPickerPopUp(context)
            colorPickerPopUp.setShowAlpha(true)
                .setDefaultColor(prefs.buttonBorderColor(requireContext()))
                .setOnPickColorListener(object : OnPickColorListener {
                    override fun onColorPicked(color: Int) {
                        prefs.setButtonBorderColor(color)
                        setColor(
                            binding.buttonBorderColorSwatch,
                            binding.buttonBorderColor,
                            color,
                        )
                    }

                    override fun onCancel() {
                        colorPickerPopUp.dismissDialog() // Dismiss the dialog.
                    }
                })
                .show()
            colorPickerPopUp.negativeButton.setOnClickListener {
                prefs.setButtonBorderColor(null)
                setColor(
                    binding.buttonBorderColorSwatch,
                    binding.buttonBorderColor,
                    prefs.buttonBorderColor(requireContext()),
                )
                colorPickerPopUp.dismissDialog()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setColor(swatch: ColorSwatchView, value: TextView, color: Int) {
        swatch.setColor(color)
        value.text = getString(R.string.color_hex, color.toHexString())
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

    private fun initMarkerBackgroundButton() {
        setColor(
            binding.markerBackgroundColorSwatch,
            binding.markerBackgroundColor,
            prefs.markerBackgroundColor(requireContext()),
        )

        binding.changeMarkerBackgroundColor.setOnClickListener {
            val colorPickerPopUp = ColorPickerPopUp(context)
            colorPickerPopUp.setShowAlpha(true)
                .setDefaultColor(prefs.markerBackgroundColor(requireContext()))
                .setOnPickColorListener(object : OnPickColorListener {
                    override fun onColorPicked(color: Int) {
                        prefs.setMarkerBackgroundColor(color)
                        setColor(
                            binding.markerBackgroundColorSwatch,
                            binding.markerBackgroundColor,
                            color,
                        )
                    }

                    override fun onCancel() {
                        colorPickerPopUp.dismissDialog()
                    }
                })
                .show()
            colorPickerPopUp.negativeButton.setText(R.string.reset)
            colorPickerPopUp.negativeButton.setOnClickListener {
                prefs.setMarkerBackgroundColor(null)
                setColor(
                    binding.markerBackgroundColorSwatch,
                    binding.markerBackgroundColor,
                    prefs.markerBackgroundColor(requireContext()),
                )
                colorPickerPopUp.dismissDialog()
            }
        }
    }

    private fun initMarkerIconButton() {
        setColor(
            binding.markerIconColorSwatch,
            binding.markerIconColor,
            prefs.markerIconColor(requireContext()),
        )

        binding.changeMarkerIconColor.setOnClickListener {
            val colorPickerPopUp = ColorPickerPopUp(context)
            colorPickerPopUp.setShowAlpha(true)
                .setDefaultColor(prefs.markerIconColor(requireContext()))
                .setOnPickColorListener(object : OnPickColorListener {
                    override fun onColorPicked(color: Int) {
                        prefs.setMarkerIconColor(color)
                        setColor(
                            binding.markerIconColorSwatch,
                            binding.markerIconColor,
                            color,
                        )
                    }

                    override fun onCancel() {
                        colorPickerPopUp.dismissDialog()
                    }
                })
                .show()
            colorPickerPopUp.negativeButton.setText(R.string.reset)
            colorPickerPopUp.negativeButton.setOnClickListener {
                prefs.setMarkerIconColor(null)
                setColor(
                    binding.markerIconColorSwatch,
                    binding.markerIconColor,
                    prefs.markerIconColor(requireContext()),
                )
                colorPickerPopUp.dismissDialog()
            }
        }
    }

    private fun initBoostedMarkerBackgroundButton() {
        setColor(
            binding.boostedMarkerBackgroundColorSwatch,
            binding.boostedMarkerBackgroundColor,
            prefs.boostedMarkerBackgroundColor(),
        )

        binding.changeBoostedMarkerBackgroundColor.setOnClickListener {
            val colorPickerPopUp = ColorPickerPopUp(context)
            colorPickerPopUp.setShowAlpha(true)
                .setDefaultColor(prefs.boostedMarkerBackgroundColor())
                .setOnPickColorListener(object : OnPickColorListener {
                    override fun onColorPicked(color: Int) {
                        prefs.setBoostedMarkerBackgroundColor(color)
                        setColor(
                            binding.boostedMarkerBackgroundColorSwatch,
                            binding.boostedMarkerBackgroundColor,
                            color,
                        )
                    }

                    override fun onCancel() {
                        colorPickerPopUp.dismissDialog() // Dismiss the dialog.
                    }
                })
                .show()
            colorPickerPopUp.negativeButton.setText(R.string.reset)
            colorPickerPopUp.negativeButton.setOnClickListener {
                prefs.setBoostedMarkerBackgroundColor(null)
                setColor(
                    binding.boostedMarkerBackgroundColorSwatch,
                    binding.boostedMarkerBackgroundColor,
                    prefs.boostedMarkerBackgroundColor(),
                )
                colorPickerPopUp.dismissDialog()
            }
        }
    }

    private fun initBoostedMarkerIconButton() {
        setColor(
            binding.boostedMarkerIconColorSwatch,
            binding.boostedMarkerIconColor,
            prefs.boostedMarkerIconColor(),
        )

        binding.changeBoostedMarkerIconColor.setOnClickListener {
            val colorPickerPopUp = ColorPickerPopUp(context)
            colorPickerPopUp.setShowAlpha(true)
                .setDefaultColor(prefs.boostedMarkerIconColor())
                .setOnPickColorListener(object : OnPickColorListener {
                    override fun onColorPicked(color: Int) {
                        prefs.setBoostedMarkerIconColor(color)
                        setColor(
                            binding.boostedMarkerIconColorSwatch,
                            binding.boostedMarkerIconColor,
                            color,
                        )
                    }

                    override fun onCancel() {
                        colorPickerPopUp.dismissDialog() // Dismiss the dialog.
                    }
                })
                .show()
            colorPickerPopUp.negativeButton.setText(R.string.reset)
            colorPickerPopUp.negativeButton.setOnClickListener {
                prefs.setBoostedMarkerIconColor(null)
                setColor(
                    binding.boostedMarkerIconColorSwatch,
                    binding.boostedMarkerIconColor,
                    prefs.boostedMarkerIconColor(),
                )
                colorPickerPopUp.dismissDialog()
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