package org.btcmap.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.mrudultora.colorpicker.ColorPickerPopUp
import com.mrudultora.colorpicker.ColorPickerPopUp.OnPickColorListener
import org.btcmap.R
import org.btcmap.databinding.ColorSettingsFragmentBinding
import org.btcmap.ui.mapColorItems

/**
 * The map's customizable colors, one row per element, split off the main
 * settings screen so its list of colors does not crowd out everything else.
 *
 * The list itself and the stored keys come from the shared `MapColor` registry;
 * only the picker is Android's.
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
            MapColor.fromKey(key)?.let { showColorPicker(it) }
        }

        refreshItems()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @StringRes
    private fun MapColor.titleRes(): Int = when (this) {
        MapColor.MarkerBackground -> R.string.marker_background_color
        MapColor.MarkerIcon -> R.string.marker_icon_color
        MapColor.BoostedMarkerBackground -> R.string.boosted_marker_background
        MapColor.BoostedMarkerIcon -> R.string.boosted_marker_icon
        MapColor.BadgeBackground -> R.string.badge_background
        MapColor.BadgeText -> R.string.badge_text
        MapColor.ButtonBackground -> R.string.button_background
        MapColor.ButtonIcon -> R.string.button_icon
        MapColor.ButtonBorder -> R.string.button_border
    }

    private fun refreshItems() {
        _binding ?: return

        binding.colorSettingsList.items = mapColorItems(prefs) { color ->
            getString(color.titleRes())
        }
    }

    /**
     * Opens the Android color picker for [color]. The four marker colors get a
     * reset labelled as such, matching the desktop; the badge and button rows
     * keep the picker's own negative-button label.
     */
    private fun showColorPicker(color: MapColor) {
        val colorPickerPopUp = ColorPickerPopUp(context)
        colorPickerPopUp.setShowAlpha(true)
            .setDefaultColor(prefs.mapColor(color))
            .setOnPickColorListener(object : OnPickColorListener {
                override fun onColorPicked(picked: Int) {
                    prefs.setMapColor(color, picked)
                    refreshItems()
                }

                override fun onCancel() {
                    colorPickerPopUp.dismissDialog()
                }
            })
            .show()
        if (color.resettable) {
            colorPickerPopUp.negativeButton.setText(R.string.reset)
        }
        colorPickerPopUp.negativeButton.setOnClickListener {
            prefs.setMapColor(color, null)
            refreshItems()
            colorPickerPopUp.dismissDialog()
        }
    }
}
