package donation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
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
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                binding.topAppBar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }
                initOnchain()
                initLightning()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initOnchain() {
        val paymentAddress = getString(R.string.donation_address_onchain)
        val paymentUrl = "bitcoin:$paymentAddress"

        val qrEncoder = QRGEncoder(paymentUrl, null, QRGContents.Type.TEXT, 1000)
        qrEncoder.colorBlack = Color.BLACK
        qrEncoder.colorWhite = Color.WHITE
        val bitmap = qrEncoder.getBitmap(0)
        binding.onchainQr.setImageBitmap(bitmap)

        listOf(binding.onchainQr, binding.onchainPay).forEach {
            it.setOnClickListener { payWithExternalWallet(paymentUrl) }
        }

        binding.onchainCopyAddress.setOnClickListener {
            addToClipboard(
                label = getString(R.string.btc_map_donation_address),
                text = paymentAddress,
            )
        }
    }

    private fun initLightning() {
        val paymentAddress = getString(R.string.donation_address_lightning)
        val paymentUrl = "lightning:$paymentAddress"

        val qrEncoder = QRGEncoder(paymentUrl, null, QRGContents.Type.TEXT, 1000)
        qrEncoder.colorBlack = Color.BLACK
        qrEncoder.colorWhite = Color.WHITE
        val bitmap = qrEncoder.getBitmap(0)
        binding.lnQr.setImageBitmap(bitmap)

        listOf(binding.lnQr, binding.lnPay).forEach {
            it.setOnClickListener { payWithExternalWallet(paymentUrl) }
        }

        binding.lnCopyAddress.setOnClickListener {
            addToClipboard(
                label = getString(R.string.btc_map_donation_address),
                text = paymentAddress,
            )
        }
    }

    private fun payWithExternalWallet(paymentUrl: String) {
        runCatching {
            require(paymentUrl.startsWith("bitcoin") || paymentUrl.startsWith("lightning"))
            Uri.parse(paymentUrl)
        }.onFailure {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.error)
                .setMessage(R.string.invalid_payment_url)
                .setPositiveButton(R.string.close, null)
                .show()
            return
        }

        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(paymentUrl)))
        }.onFailure {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.error)
                .setMessage(R.string.you_dont_have_a_compatible_wallet)
                .setPositiveButton(R.string.close, null)
                .show()
        }
    }

    private fun addToClipboard(label: String, text: String) {
        val clipManager =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipManager.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(requireContext(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }
}