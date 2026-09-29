package org.btcmap.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.mrudultora.colorpicker.ColorPickerPopUp
import com.mrudultora.colorpicker.ColorPickerPopUp.OnPickColorListener
import org.btcmap.R
import org.btcmap.databinding.ColorSettingsFragmentBinding
import org.btcmap.view.ColorSwatchView

/**
 * The map's customizable colors, one row per element, split off the main
 * settings screen so its list of colors does not crowd out everything else.
 */
class ColorSettingsFragment : Fragment() {

    private var _binding: ColorSettingsFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = ColorSettingsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.topAppBar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        initColorRow(
            changeView = binding.changeMarkerBackgroundColor,
            swatch = binding.markerBackgroundColorSwatch,
            value = binding.markerBackgroundColor,
            get = { prefs.markerBackgroundColor(requireContext()) },
            apply = { prefs.setMarkerBackgroundColor(it) },
            showResetLabel = true,
        )

        initColorRow(
            changeView = binding.changeMarkerIconColor,
            swatch = binding.markerIconColorSwatch,
            value = binding.markerIconColor,
            get = { prefs.markerIconColor(requireContext()) },
            apply = { prefs.setMarkerIconColor(it) },
            showResetLabel = true,
        )

        initColorRow(
            changeView = binding.changeBoostedMarkerBackgroundColor,
            swatch = binding.boostedMarkerBackgroundColorSwatch,
            value = binding.boostedMarkerBackgroundColor,
            get = { prefs.boostedMarkerBackgroundColor() },
            apply = { prefs.setBoostedMarkerBackgroundColor(it) },
            showResetLabel = true,
        )

        initColorRow(
            changeView = binding.changeBoostedMarkerIconColor,
            swatch = binding.boostedMarkerIconColorSwatch,
            value = binding.boostedMarkerIconColor,
            get = { prefs.boostedMarkerIconColor() },
            apply = { prefs.setBoostedMarkerIconColor(it) },
            showResetLabel = true,
        )

        initColorRow(
            changeView = binding.changeBadgeBackgroundColor,
            swatch = binding.badgeBackgroundColorSwatch,
            value = binding.badgeBackgroundColor,
            get = { prefs.badgeBackgroundColor(requireContext()) },
            apply = { prefs.setBadgeBackgroundColor(it) },
        )

        initColorRow(
            changeView = binding.changeBadgeTextColor,
            swatch = binding.badgeTextColorSwatch,
            value = binding.badgeTextColor,
            get = { prefs.badgeTextColor(requireContext()) },
            apply = { prefs.setBadgeTextColor(it) },
        )

        initColorRow(
            changeView = binding.changeButtonBackgroundColor,
            swatch = binding.buttonBackgroundColorSwatch,
            value = binding.buttonBackgroundColor,
            get = { prefs.buttonBackgroundColor(requireContext()) },
            apply = { prefs.setButtonBackgroundColor(it) },
        )

        initColorRow(
            changeView = binding.changeButtonIconColor,
            swatch = binding.buttonIconColorSwatch,
            value = binding.buttonIconColor,
            get = { prefs.buttonIconColor(requireContext()) },
            apply = { prefs.setButtonIconColor(it) },
        )

        initColorRow(
            changeView = binding.changeButtonBorderColor,
            swatch = binding.buttonBorderColorSwatch,
            value = binding.buttonBorderColor,
            get = { prefs.buttonBorderColor(requireContext()) },
            apply = { prefs.setButtonBorderColor(it) },
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Wires a color row: shows the current color and opens the picker on tap,
     * where the negative button resets the color to its default. [showResetLabel]
     * labels that button as a reset, matching the rows that do; the badge and
     * button rows keep the picker's own label.
     */
    private fun initColorRow(
        changeView: View,
        swatch: ColorSwatchView,
        value: TextView,
        get: () -> Int,
        apply: (Int?) -> Unit,
        showResetLabel: Boolean = false,
    ) {
        bindColor(swatch, value, get())

        changeView.setOnClickListener {
            val colorPickerPopUp = ColorPickerPopUp(context)
            colorPickerPopUp.setShowAlpha(true)
                .setDefaultColor(get())
                .setOnPickColorListener(object : OnPickColorListener {
                    override fun onColorPicked(color: Int) {
                        apply(color)
                        bindColor(swatch, value, color)
                    }

                    override fun onCancel() {
                        colorPickerPopUp.dismissDialog()
                    }
                })
                .show()
            if (showResetLabel) {
                colorPickerPopUp.negativeButton.setText(R.string.reset)
            }
            colorPickerPopUp.negativeButton.setOnClickListener {
                apply(null)
                bindColor(swatch, value, get())
                colorPickerPopUp.dismissDialog()
            }
        }
    }

    private fun bindColor(swatch: ColorSwatchView, value: TextView, color: Int) {
        swatch.setColor(color)
        value.text = getString(R.string.color_hex, color.toHexString())
    }
}
