package settings

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import conf.ConfRepo
import conf.MapStyle
import conf.name
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

        binding.showSyncSummary.isChecked = conf.conf.value.showSyncSummary
        binding.showSyncSummary.setOnCheckedChangeListener { _, isChecked ->
            conf.update { it.copy(showSyncSummary = isChecked) }
        }

        binding.notifyOfNewElementsNearby.isEnabled =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

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