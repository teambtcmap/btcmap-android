package settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import conf.ConfRepo
import conf.MapStyle
import conf.name
import org.btcmap.R
import org.btcmap.databinding.FragmentSettingsBinding
import org.koin.android.ext.android.inject

class SettingsFragment : Fragment() {

    private val conf: ConfRepo by inject()

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

        binding.currentMapStyle.text = conf.current.mapStyle.name(requireContext())

        binding.mapStyleButton.setOnClickListener {
            val dialog = MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.map_style)
                .setView(R.layout.dialog_map_style).show()

            val setupInterval = fun RadioButton?.(style: MapStyle) {
                if (this == null) return

                text = style.name(requireContext())
                isChecked = conf.current.mapStyle == style

                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        conf.update { it.copy(mapStyle = style) }
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

        binding.showAtms.isChecked = conf.conf.value.showAtms
        binding.showAtms.setOnCheckedChangeListener { _, isChecked ->
            conf.update { it.copy(showAtms = isChecked) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}