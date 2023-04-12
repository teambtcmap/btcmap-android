package settings

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
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

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

        binding.themedPins.isChecked = conf.conf.value.themedPins
        binding.themedPins.setOnCheckedChangeListener { _, isChecked ->
            conf.update { it.copy(themedPins = isChecked) }
        }

        binding.showTags.isChecked = conf.conf.value.showTags
        binding.showTags.setOnCheckedChangeListener { _, isChecked ->
            conf.update { it.copy(showTags = isChecked) }
        }

        val lastSyncDate = conf.conf.value.lastSyncDate

        if (lastSyncDate != null) {
            val dateFormat = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
            binding.lastSyncDate.text = getString(
                R.string.last_sync_s,
                dateFormat.format(lastSyncDate.withZoneSameInstant(ZoneOffset.systemDefault())),
            )
        } else {
            binding.lastSyncDate.setText(R.string.database_is_empty)
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
                            findNavController().navigate(R.id.loginFragment)
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