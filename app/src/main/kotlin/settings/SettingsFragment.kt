package settings

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenResumed
import androidx.navigation.fragment.findNavController
import conf.ConfRepo
import kotlinx.coroutines.launch
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
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { toolbar, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            toolbar.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topMargin = insets.top
            }
            WindowInsetsCompat.CONSUMED
        }

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        WindowCompat.getInsetsController(
            requireActivity().window,
            requireActivity().window.decorView,
        ).isAppearanceLightStatusBars =
            when (requireContext().resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_NO -> true
                else -> false
            }

        binding.themedPins.isChecked = conf.conf.value.themedPins
        binding.themedPins.setOnCheckedChangeListener { _, isChecked ->
            conf.update { it.copy(themedPins = isChecked) }
        }

        binding.darkMap.isChecked = conf.conf.value.darkMap
        binding.darkMap.setOnCheckedChangeListener { _, isChecked ->
            conf.update { it.copy(darkMap = isChecked) }
        }

        binding.showTags.isChecked = conf.conf.value.showTags
        binding.showTags.setOnCheckedChangeListener { _, isChecked ->
            conf.update { it.copy(showTags = isChecked) }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            whenResumed {
                conf.conf.collect {
                    if (it.osmLogin.isNotBlank()) {
                        binding.accountTitle.setText(R.string.log_out)
                        binding.accountSubtitle.isVisible = true
                        binding.accountSubtitle.text = it.osmLogin

                        binding.account.setOnClickListener {
                            conf.update { prev ->
                                prev.copy(osmLogin = "", osmPassword = "")
                            }
                        }
                    } else {
                        binding.accountTitle.setText(R.string.connect_osm_account)
                        binding.accountSubtitle.isVisible = false

                        binding.account.setOnClickListener {
                            findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToLoginFragment())
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}