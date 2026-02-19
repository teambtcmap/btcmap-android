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
import org.btcmap.databinding.SettingsFragmentBinding

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

        initMapStyleButton()

        binding.useAdaptiveColors.isChecked = prefs.useAdaptiveColors
        binding.useAdaptiveColors.setOnCheckedChangeListener { _, isChecked ->
            prefs.useAdaptiveColors = isChecked
            refreshAllColors()
        }

        initMarkerBackgroundButton()
        initMarkerIconButton()

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

        binding.badgeBackgroundColor.text =
            "#${prefs.badgeBackgroundColor(requireContext()).toHexString()}"

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
                binding.badgeBackgroundColor.text =
                    "#${prefs.badgeBackgroundColor(requireContext()).toHexString()}"
                colorPickerPopUp.dismissDialog()
            }
        }

        binding.badgeTextColor.text =
            "#${prefs.badgeTextColor(requireContext()).toHexString()}"

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
                binding.badgeTextColor.text =
                    "#${prefs.badgeTextColor(requireContext()).toHexString()}"
                colorPickerPopUp.dismissDialog()
            }
        }

        binding.buttonBackgroundColor.text =
            "#${prefs.buttonBackgroundColor(requireContext()).toHexString()}"

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
                binding.buttonBackgroundColor.text =
                    "#${prefs.buttonBackgroundColor(requireContext()).toHexString()}"
                colorPickerPopUp.dismissDialog()
            }
        }

        binding.buttonIconColor.text =
            "#${prefs.buttonIconColor(requireContext()).toHexString()}"

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
                binding.buttonIconColor.text =
                    "#${prefs.buttonIconColor(requireContext()).toHexString()}"
                colorPickerPopUp.dismissDialog()
            }
        }

        binding.buttonBorderColor.text =
            "#${prefs.buttonBorderColor(requireContext()).toHexString()}"

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
                binding.buttonBorderColor.text =
                    "#${prefs.buttonBorderColor(requireContext()).toHexString()}"
                colorPickerPopUp.dismissDialog()
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
                invoke(dialog.findViewById(R.id.carto_dark_matter), MapStyle.CartoDarkMatter)
            }
        }
    }

    private fun initMarkerBackgroundButton() {
        binding.markerBackgroundColor.text =
            "#${prefs.markerBackgroundColor(requireContext()).toHexString()}"
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
                binding.markerBackgroundColor.text =
                    "#${prefs.markerBackgroundColor(requireContext()).toHexString()}"
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
        binding.markerIconColor.text =
            "#${prefs.markerIconColor(requireContext()).toHexString()}"
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
                binding.markerIconColor.text =
                    "#${prefs.markerIconColor(requireContext()).toHexString()}"
                binding.markerIconColor.setTextColor(prefs.markerIconColor(requireContext()))
                colorPickerPopUp.dismissDialog()
            }
        }
    }

    private fun refreshAllColors() {
        binding.markerBackgroundColor.text =
            "#${prefs.markerBackgroundColor(requireContext()).toHexString()}"
        binding.markerBackgroundColor.setTextColor(prefs.markerBackgroundColor(requireContext()))
        binding.markerIconColor.text =
            "#${prefs.markerIconColor(requireContext()).toHexString()}"
        binding.markerIconColor.setTextColor(prefs.markerIconColor(requireContext()))
        binding.badgeBackgroundColor.text =
            "#${prefs.badgeBackgroundColor(requireContext()).toHexString()}"
        binding.badgeBackgroundColor.setTextColor(prefs.badgeBackgroundColor(requireContext()))
        binding.badgeTextColor.text =
            "#${prefs.badgeTextColor(requireContext()).toHexString()}"
        binding.badgeTextColor.setTextColor(prefs.badgeTextColor(requireContext()))
        binding.buttonBackgroundColor.text =
            "#${prefs.buttonBackgroundColor(requireContext()).toHexString()}"
        binding.buttonBackgroundColor.setTextColor(prefs.buttonBackgroundColor(requireContext()))
        binding.buttonIconColor.text =
            "#${prefs.buttonIconColor(requireContext()).toHexString()}"
        binding.buttonIconColor.setTextColor(prefs.buttonIconColor(requireContext()))
        binding.buttonBorderColor.text =
            "#${prefs.buttonBorderColor(requireContext()).toHexString()}"
        binding.buttonBorderColor.setTextColor(prefs.buttonBorderColor(requireContext()))
    }
}