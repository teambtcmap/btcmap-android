package settings

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import conf.ConfRepo
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

    private val postNotificationsPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { }

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
            parentFragmentManager.popBackStack()
        }

        binding.showAtms.isChecked = conf.conf.value.showAtms
        binding.showAtms.setOnCheckedChangeListener { _, isChecked ->
            conf.update { it.copy(showAtms = isChecked) }
        }

        binding.showSyncSummary.isChecked = conf.conf.value.showSyncSummary
        binding.showSyncSummary.setOnCheckedChangeListener { _, isChecked ->
            conf.update { it.copy(showSyncSummary = isChecked) }
        }

        binding.notifyOfNewElementsNearby.isEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

        binding.notifyOfNewElementsNearby.isChecked = conf.conf.value.notifyOfNewElementsNearby
        binding.notifyOfNewElementsNearby.setOnCheckedChangeListener { _, isChecked ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                postNotificationsPermissionRequest.launch(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS)
                )
            }
            conf.update { it.copy(notifyOfNewElementsNearby = isChecked) }
        }

        val lastSyncDate = conf.conf.value.lastSyncDate

        if (lastSyncDate != null) {
            val dateFormat = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
            binding.lastSyncDate.text = getString(
                R.string.updated_s,
                dateFormat.format(lastSyncDate.withZoneSameInstant(ZoneOffset.systemDefault())),
            )
        } else {
            binding.lastSyncDate.setText(R.string.database_is_empty)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}