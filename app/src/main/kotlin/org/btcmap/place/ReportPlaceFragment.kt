package org.btcmap.place

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.btcmap.R
import org.btcmap.api
import org.btcmap.api.reportPlace
import org.btcmap.databinding.ReportPlaceFragmentBinding

class ReportPlaceFragment : Fragment() {

    private data class Args(
        val placeId: Long,
        val defaultType: String?,
    )

    private val args by lazy {
        Args(
            placeId = requireArguments().getLong("place_id"),
            defaultType = if (requireArguments().containsKey("default_type")) {
                requireArguments().getString("default_type")
            } else null,
        )
    }

    private var _binding: ReportPlaceFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = ReportPlaceFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.topAppBar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        when (args.defaultType) {
            "verified" -> binding.reportType.check(R.id.typeVerified)
            "refused_sats" -> binding.reportType.check(R.id.typeRefusedSats)
            "out_of_business" -> binding.reportType.check(R.id.typeOutOfBusiness)
            else -> Unit
        }

        binding.btnSubmit.setOnClickListener { submit() }
    }

    private fun submit() {
        val type = when (binding.reportType.checkedRadioButtonId) {
            R.id.typeVerified -> "verified"
            R.id.typeRefusedSats -> "refused_sats"
            R.id.typeOutOfBusiness -> "out_of_business"
            else -> null
        }

        if (type == null) {
            Toast.makeText(
                requireContext(),
                R.string.report_select_type,
                Toast.LENGTH_SHORT,
            ).show()
            return
        }

        val comment = binding.comment.text?.toString()?.trim().orEmpty()

        binding.reportType.isEnabled = false
        binding.comment.isEnabled = false
        binding.btnSubmit.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                api().reportPlace(
                    placeId = args.placeId,
                    type = type,
                    comment = comment.takeIf { it.isNotEmpty() },
                )
                withResumed {
                    Toast.makeText(
                        requireContext(),
                        R.string.report_submitted,
                        Toast.LENGTH_LONG,
                    ).show()
                    parentFragmentManager.popBackStack()
                }
            } catch (t: Throwable) {
                Log.e(null, null, t)
                withResumed {
                    binding.reportType.isEnabled = true
                    binding.comment.isEnabled = true
                    binding.btnSubmit.isEnabled = true
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.error)
                        .setMessage(t.toString())
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}