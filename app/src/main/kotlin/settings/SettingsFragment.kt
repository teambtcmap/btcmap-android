package settings

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.work.WorkManager
import androidx.work.WorkQuery
import app.isDebuggable
import conf.ConfRepo
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.btcmap.R
import org.btcmap.databinding.FragmentSettingsBinding
import org.koin.android.ext.android.inject
import sync.BackgroundSyncScheduler
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
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

        binding.showAtms.isChecked = conf.conf.value.showAtms
        binding.showAtms.setOnCheckedChangeListener { _, isChecked ->
            conf.update { it.copy(showAtms = isChecked) }
        }

        binding.showOsmAttribution.isChecked = conf.conf.value.showOsmAttribution
        binding.showOsmAttribution.setOnCheckedChangeListener { _, isChecked ->
            conf.update { it.copy(showOsmAttribution = isChecked) }
        }

        binding.showSyncSummary.isChecked = conf.conf.value.showSyncSummary
        binding.showSyncSummary.setOnCheckedChangeListener { _, isChecked ->
            conf.update { it.copy(showSyncSummary = isChecked) }
        }

        binding.showAllNewElements.isChecked = conf.conf.value.showAllNewElements
        binding.showAllNewElements.setOnCheckedChangeListener { _, isChecked ->
            conf.update { it.copy(showAllNewElements = isChecked) }
        }

        val lastSyncDate = conf.conf.value.lastSyncDate

        if (lastSyncDate != null) {
            val dateFormat = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
            binding.lastSyncDate.text = getString(
                R.string.updated_s,
                dateFormat.format(lastSyncDate.withZoneSameInstant(ZoneOffset.systemDefault())),
            )

            if (requireContext().isDebuggable()) {
                runBlocking {
                    WorkManager.getInstance(requireContext()).getWorkInfosFlow(
                        WorkQuery.fromUniqueWorkNames(
                            BackgroundSyncScheduler.WORK_NAME,
                        )
                    ).firstOrNull()?.firstOrNull()?.let {
                        val nextScheduleTime = ZonedDateTime.ofInstant(
                            Instant.ofEpochMilli(it.nextScheduleTimeMillis),
                            ZoneOffset.systemDefault(),
                        )
                        Log.d("sync", "Next schedule time: $nextScheduleTime")
                        Toast.makeText(
                            requireContext(),
                            "Next background sync time: $nextScheduleTime",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        } else {
            binding.lastSyncDate.setText(R.string.database_is_empty)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}