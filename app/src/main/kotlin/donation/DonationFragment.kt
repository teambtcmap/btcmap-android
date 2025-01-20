package donation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import org.btcmap.R
import org.btcmap.databinding.FragmentDonationBinding

class DonationFragment : Fragment() {

    private var _binding: FragmentDonationBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentDonationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.apply {
            toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }
            listOf(qr, pay).forEach {
                it.setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data =
                        Uri.parse("bitcoin:${getString(R.string.donation_address_onchain)}")
                    runCatching {
                        startActivity(intent)
                    }.onFailure {
                        Toast.makeText(
                            requireContext(),
                            R.string.you_dont_have_a_compatible_wallet,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            copy.setOnClickListener { onCopyButtonClick() }
            listOf(lnQr, lnPay).forEach {
                it.setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data =
                        Uri.parse("lightning:${getString(R.string.donation_address_lightning)}")
                    runCatching {
                        startActivity(intent)
                    }.onFailure {
                        Toast.makeText(
                            requireContext(),
                            R.string.you_dont_have_a_compatible_wallet,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            lnCopy.setOnClickListener { onLnCopyButtonClick() }
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { toolbar, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            toolbar.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topMargin = insets.top
            }
            WindowInsetsCompat.CONSUMED
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onCopyButtonClick() {
        val clipManager =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipLabel = getString(R.string.btc_map_donation_address)
        val clipText = getString(R.string.donation_address_onchain)
        clipManager.setPrimaryClip(ClipData.newPlainText(clipLabel, clipText))
        Toast.makeText(requireContext(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }

    private fun onLnCopyButtonClick() {
        val clipManager =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipLabel = getString(R.string.btc_map_donation_address)
        val clipText = getString(R.string.donation_address_lightning)
        clipManager.setPrimaryClip(ClipData.newPlainText(clipLabel, clipText))
        Toast.makeText(requireContext(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }
}