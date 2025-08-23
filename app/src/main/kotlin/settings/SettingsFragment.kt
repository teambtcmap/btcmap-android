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
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
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

    private fun HttpUrl?.toStyleName(): String {
        return when (this) {
            null -> getString(R.string.style_auto)
            "https://tiles.openfreemap.org/styles/liberty".toHttpUrl() -> getString(R.string.style_liberty)
            "https://tiles.openfreemap.org/styles/positron".toHttpUrl() -> getString(R.string.style_positron)
            "https://tiles.openfreemap.org/styles/bright".toHttpUrl() -> getString(R.string.style_bright)
            "https://static.btcmap.org/map-styles/dark.json".toHttpUrl() -> getString(R.string.style_dark)
            else -> conf.current.mapStyleUrl.toString()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.topAppBar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.currentMapStyle.text = conf.current.mapStyleUrl.toStyleName()

        binding.mapStyleButton.setOnClickListener {
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.map_style)
                .setView(R.layout.dialog_map_style)
                .show()

            val setupInterval = fun RadioButton?.(url: HttpUrl?) {
                if (this == null) return

                text = url.toStyleName()
                isChecked = conf.current.mapStyleUrl == url

                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        conf.update { it.copy(mapStyleUrl = url) }
                        binding.currentMapStyle.text = text
                        dialog.dismiss()
                    }
                }
            }

            setupInterval.apply {
                invoke(dialog.findViewById(R.id.auto), null)
                invoke(
                    dialog.findViewById(R.id.liberty),
                    "https://tiles.openfreemap.org/styles/liberty".toHttpUrl()
                )
                invoke(
                    dialog.findViewById(R.id.positron),
                    "https://tiles.openfreemap.org/styles/positron".toHttpUrl()
                )
                invoke(
                    dialog.findViewById(R.id.bright),
                    "https://tiles.openfreemap.org/styles/bright".toHttpUrl()
                )
                invoke(
                    dialog.findViewById(R.id.dark),
                    "https://static.btcmap.org/map-styles/dark.json".toHttpUrl()
                )
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