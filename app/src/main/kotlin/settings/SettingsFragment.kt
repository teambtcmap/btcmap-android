package settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}