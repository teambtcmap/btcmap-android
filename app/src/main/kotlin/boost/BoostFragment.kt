package boost

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import api.InvoiceApi
import api.PlaceApi
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.btcmap.R
import org.btcmap.databinding.FragmentBoostElementBinding
import java.text.NumberFormat
import androidx.core.net.toUri
import api.InvoiceApi.paid

class BoostFragment : Fragment() {

    private data class Args(
        val elementId: Long,
    )

    private val args = lazy {
        Args(requireArguments().getLong("place_id"))
    }

    private var _binding: FragmentBoostElementBinding? = null
    private val binding get() = _binding!!

    private var paymentRequest = ""
    private var invoiceUuid = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentBoostElementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.topAppBar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.generateInvoice.setOnClickListener { onGenerateInvoiceButtonClick() }
        binding.payInvoice.setOnClickListener { onPayInvoiceClick() }
        binding.copyInvoice.setOnClickListener { onCopyInvoiceClick() }

        viewLifecycleOwner.lifecycleScope.launch {
            val quote = try {
                PlaceApi.getBoostQuote()
            } catch (_: Throwable) {
                parentFragmentManager.popBackStack()
                return@launch
            }

            onQuoteReceived(quote)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                delay(500)

                val invoiceUuid = invoiceUuid

                if (!invoiceUuid.isBlank()) {
                    val invoice = try {
                        InvoiceApi.getInvoice(invoiceUuid)
                    } catch (_: Throwable) {
                        continue
                    }

                    if (invoice.paid) {
                        withResumed {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.place_has_been_boosted),
                                Toast.LENGTH_LONG,
                            ).show()
                            parentFragmentManager.popBackStack()
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

    private fun onGenerateInvoiceButtonClick() {
        val days = if (binding.boost12m.isChecked) {
            365
        } else if (binding.boost3m.isChecked) {
            90
        } else {
            30
        }

        binding.durationOptions.isEnabled = false
        binding.boost1m.isEnabled = false
        binding.boost3m.isEnabled = false
        binding.boost12m.isEnabled = false
        binding.generateInvoice.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            val invoice = try {
                PlaceApi.boost(args.value.elementId, days.toLong())
            } catch (t: Throwable) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.error)
                    .setMessage(t.toString())
                    .setPositiveButton(R.string.close, null)
                    .setOnDismissListener {
                        binding.durationOptions.isEnabled = true
                        binding.boost1m.isEnabled = true
                        binding.boost3m.isEnabled = true
                        binding.boost12m.isEnabled = true
                        binding.generateInvoice.isEnabled = true
                    }
                    .show()
                return@launch
            }

            onInvoiceReceived(invoice)
        }
    }

    private fun onQuoteReceived(quote: PlaceApi.BoostQuote) {
        binding.boost1m.append(" - ")
        binding.boost1m.append(
            getString(
                R.string.d_sat,
                NumberFormat.getNumberInstance().format(quote.quote30dsat),
            )
        )

        binding.boost3m.append(" - ")
        binding.boost3m.append(
            getString(
                R.string.d_sat,
                NumberFormat.getNumberInstance().format(quote.quote90dsat),
            )
        )

        binding.boost12m.append(" - ")
        binding.boost12m.append(
            getString(
                R.string.d_sat,
                NumberFormat.getNumberInstance().format(quote.quote365dsat),
            )
        )
    }

    private fun onInvoiceReceived(invoice: PlaceApi.BoostResponse) {
        this.paymentRequest = invoice.paymentRequest
        this.invoiceUuid = invoice.invoiceUuid
        val qrEncoder = QRGEncoder(invoice.paymentRequest, null, QRGContents.Type.TEXT, 1000)
        qrEncoder.colorBlack = Color.BLACK
        qrEncoder.colorWhite = Color.WHITE
        val bitmap = qrEncoder.getBitmap(0)
        binding.qr.isVisible = true
        binding.qr.setImageBitmap(bitmap)
        binding.payInvoice.isVisible = true
        binding.copyInvoice.isVisible = true
    }

    private fun onPayInvoiceClick() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = "lightning:$paymentRequest".toUri()
        runCatching {
            startActivity(intent)
        }.onFailure {
            Toast.makeText(
                requireContext(),
                R.string.you_dont_have_a_compatible_wallet,
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    private fun onCopyInvoiceClick() {
        val clipManager =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipLabel = getString(R.string.btc_map_boost_payment_request)
        val clipText = paymentRequest
        clipManager.setPrimaryClip(ClipData.newPlainText(clipLabel, clipText))
        Toast.makeText(requireContext(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }
}