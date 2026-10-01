package org.btcmap.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.graphics.Color
import androidx.fragment.app.Fragment
import com.mrudultora.colorpicker.ColorPickerPopUp
import com.mrudultora.colorpicker.ColorPickerPopUp.OnPickColorListener
import org.btcmap.R
import org.btcmap.databinding.ColorSettingsFragmentBinding
import org.btcmap.ui.ColorItem

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

        binding.colorSettingsList.onItemClick = { key ->
            specs()[key]?.let { showColorPicker(it) }
        }

        refreshItems()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * One customizable color: the label, how to read it and how to change it.
     * [showResetLabel] labels the picker's negative button as a reset, matching
     * the marker colors; the badge and button rows keep the picker's own label.
     */
    private data class ColorSpec(
        val titleRes: Int,
        val get: () -> Int,
        val apply: (Int?) -> Unit,
        val showResetLabel: Boolean = false,
    )

    private fun specs(): Map<String, ColorSpec> = linkedMapOf(
        "markerBackgroundColor" to ColorSpec(
            R.string.marker_background_color,
            { prefs.markerBackgroundColor(requireContext()) },
            { prefs.setMarkerBackgroundColor(it) },
            showResetLabel = true,
        ),
        "markerIconColor" to ColorSpec(
            R.string.marker_icon_color,
            { prefs.markerIconColor(requireContext()) },
            { prefs.setMarkerIconColor(it) },
            showResetLabel = true,
        ),
        "boostedMarkerBackgroundColor" to ColorSpec(
            R.string.boosted_marker_background,
            { prefs.boostedMarkerBackgroundColor() },
            { prefs.setBoostedMarkerBackgroundColor(it) },
            showResetLabel = true,
        ),
        "boostedMarkerIconColor" to ColorSpec(
            R.string.boosted_marker_icon,
            { prefs.boostedMarkerIconColor() },
            { prefs.setBoostedMarkerIconColor(it) },
            showResetLabel = true,
        ),
        "badgeBackgroundColor" to ColorSpec(
            R.string.badge_background,
            { prefs.badgeBackgroundColor(requireContext()) },
            { prefs.setBadgeBackgroundColor(it) },
        ),
        "badgeTextColor" to ColorSpec(
            R.string.badge_text,
            { prefs.badgeTextColor(requireContext()) },
            { prefs.setBadgeTextColor(it) },
        ),
        "buttonBackgroundColor" to ColorSpec(
            R.string.button_background,
            { prefs.buttonBackgroundColor(requireContext()) },
            { prefs.setButtonBackgroundColor(it) },
        ),
        "buttonIconColor" to ColorSpec(
            R.string.button_icon,
            { prefs.buttonIconColor(requireContext()) },
            { prefs.setButtonIconColor(it) },
        ),
        "buttonBorderColor" to ColorSpec(
            R.string.button_border,
            { prefs.buttonBorderColor(requireContext()) },
            { prefs.setButtonBorderColor(it) },
        ),
    )

    private fun refreshItems() {
        _binding ?: return

        binding.colorSettingsList.items = specs().map { (key, spec) ->
            val color = spec.get()
            ColorItem(
                key = key,
                title = getString(spec.titleRes),
                value = getString(R.string.color_hex, color.toHexString()),
                color = Color(color),
            )
        }
    }

    private fun showColorPicker(spec: ColorSpec) {
        val colorPickerPopUp = ColorPickerPopUp(context)
        colorPickerPopUp.setShowAlpha(true)
            .setDefaultColor(spec.get())
            .setOnPickColorListener(object : OnPickColorListener {
                override fun onColorPicked(color: Int) {
                    spec.apply(color)
                    refreshItems()
                }

                override fun onCancel() {
                    colorPickerPopUp.dismissDialog()
                }
            })
            .show()
        if (spec.showResetLabel) {
            colorPickerPopUp.negativeButton.setText(R.string.reset)
        }
        colorPickerPopUp.negativeButton.setOnClickListener {
            spec.apply(null)
            refreshItems()
            colorPickerPopUp.dismissDialog()
        }
    }
}
