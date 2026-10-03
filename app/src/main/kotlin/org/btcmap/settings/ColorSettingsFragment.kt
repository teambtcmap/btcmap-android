package org.btcmap.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import org.btcmap.R
import org.btcmap.databinding.ColorSettingsFragmentBinding
import org.btcmap.ui.ColorsPageLabels

/**
 * The map's customizable colors, one row per element, split off the main
 * settings screen so its list of colors does not crowd out everything else.
 *
 * The list, the stored keys and the picker all come from the shared
 * [org.btcmap.ui.ColorsPage]; this fragment only supplies the labels.
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

        binding.colorSettingsList.settings = prefs
        binding.colorSettingsList.labels = ColorsPageLabels(
            colorTitle = { getString(it.titleRes()) },
            red = getString(R.string.color_picker_red),
            green = getString(R.string.color_picker_green),
            blue = getString(R.string.color_picker_blue),
            alpha = getString(R.string.color_picker_alpha),
            ok = getString(android.R.string.ok),
            reset = getString(R.string.reset),
            cancel = getString(android.R.string.cancel),
        )
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
}
