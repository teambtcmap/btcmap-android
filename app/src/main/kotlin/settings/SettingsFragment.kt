package settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mrudultora.colorpicker.ColorPickerPopUp
import com.mrudultora.colorpicker.ColorPickerPopUp.OnPickColorListener
import org.btcmap.R
import org.btcmap.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.topAppBar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.currentMapStyle.text = prefs.mapStyle.name(requireContext())

        binding.markerBackgroundColor.text =
            "#${prefs.markerBackgroundColor(requireContext()).toHexString()}"

        binding.changeMarkerBackgroundColor.setOnClickListener {
            val colorPickerPopUp = ColorPickerPopUp(context)
            colorPickerPopUp.setShowAlpha(true)
                .setDefaultColor(prefs.markerBackgroundColor(requireContext()))
                .setOnPickColorListener(object : OnPickColorListener {
                    override fun onColorPicked(color: Int) {
                        prefs.setMarkerBackgroundColor(color)
                    }

                    override fun onCancel() {
                        colorPickerPopUp.dismissDialog() // Dismiss the dialog.
                    }
                })
                .show()
            colorPickerPopUp.negativeButton.setOnClickListener {
                prefs.setMarkerBackgroundColor(null)
                binding.markerBackgroundColor.text =
                    "#${prefs.markerBackgroundColor(requireContext()).toHexString()}"
                colorPickerPopUp.dismissDialog()
            }
        }

        binding.boostedMarkerBackgroundColor.text =
            "#${prefs.boostedMarkerBackgroundColor().toHexString()}"

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
            colorPickerPopUp.negativeButton.setOnClickListener {
                prefs.setBoostedMarkerBackgroundColor(null)
                binding.boostedMarkerBackgroundColor.text =
                    "#${prefs.boostedMarkerBackgroundColor().toHexString()}"
                colorPickerPopUp.dismissDialog()
            }
        }

        binding.markerIconColor.text =
            "#${prefs.markerIconColor(requireContext()).toHexString()}"

        binding.changeMarkerIconColor.setOnClickListener {
            val colorPickerPopUp = ColorPickerPopUp(context)
            colorPickerPopUp.setShowAlpha(true)
                .setDefaultColor(prefs.markerIconColor(requireContext()))
                .setOnPickColorListener(object : OnPickColorListener {
                    override fun onColorPicked(color: Int) {
                        prefs.setMarkerIconColor(color)
                    }

                    override fun onCancel() {
                        colorPickerPopUp.dismissDialog() // Dismiss the dialog.
                    }
                })
                .show()
            colorPickerPopUp.negativeButton.setOnClickListener {
                prefs.setMarkerIconColor(null)
                binding.markerIconColor.text =
                    "#${prefs.markerIconColor(requireContext()).toHexString()}"
                colorPickerPopUp.dismissDialog()
            }
        }

        binding.badgeBackgroundColor.text =
            "#${prefs.badgeBackgroundColor().toHexString()}"

        binding.changeBadgeBackgroundColor.setOnClickListener {
            val colorPickerPopUp = ColorPickerPopUp(context)
            colorPickerPopUp.setShowAlpha(true)
                .setDefaultColor(prefs.badgeBackgroundColor())
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
                binding.badgeBackgroundColor.text =
                    "#${prefs.badgeBackgroundColor().toHexString()}"
                colorPickerPopUp.dismissDialog()
            }
        }

        binding.badgeTextColor.text =
            "#${prefs.badgeTextColor().toHexString()}"

        binding.changeBadgeTextColor.setOnClickListener {
            val colorPickerPopUp = ColorPickerPopUp(context)
            colorPickerPopUp.setShowAlpha(true)
                .setDefaultColor(prefs.badgeTextColor())
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
                binding.badgeTextColor.text =
                    "#${prefs.badgeTextColor().toHexString()}"
                colorPickerPopUp.dismissDialog()
            }
        }

        binding.mapStyleButton.setOnClickListener {
            val dialog = MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.map_style)
                .setView(R.layout.dialog_map_style).show()

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}