package settings

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import org.btcmap.R
import org.btcmap.databinding.FragmentSettingsBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsFragment : Fragment() {

    private val model: SettingsViewModel by viewModel()

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
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        binding.distanceUnitsButton.setOnClickListener {
            lifecycleScope.launchWhenResumed {
                val labels = resources.getStringArray(R.array.distance_units)
                val values = resources.getStringArray(R.array.distance_units_values)

                val selectedUnits = model.getDistanceUnits().first()
                val selectedValueIndex = values.indexOf(selectedUnits)

                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.pref_distance_units)
                    .setSingleChoiceItems(labels, selectedValueIndex) { dialog, index ->
                        lifecycleScope.launch {
                            if (index == 0) {
                                model.setDistanceUnits("")
                            } else {
                                model.setDistanceUnits(values[index])
                            }

                            dialog.dismiss()
                        }
                    }
                    .show()
            }
        }

        lifecycleScope.launchWhenResumed {
            model.getDistanceUnits().map {
                if (it.isBlank()) {
                    resources.getString(R.string.pref_distance_units_automatic)
                } else {
                    it
                }
            }.collect {
                val labels = resources.getStringArray(R.array.distance_units)
                val values = resources.getStringArray(R.array.distance_units_values)
                binding.distanceUnits.text = labels[values.indexOf(it)]
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}