package comment

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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.btcmap.R
import org.btcmap.databinding.FragmentAddElementCommentBinding
import androidx.core.net.toUri
import androidx.lifecycle.withResumed
import api.CommentApi
import api.InvoiceApi
import api.InvoiceApi.paid
import kotlinx.coroutines.delay
import log.log

class AddCommentFragment : Fragment() {

    private data class Args(
        val elementId: Long,
    )

    private val args = lazy {
        Args(requireArguments().getLong("element_id"))
    }

    private var _binding: FragmentAddElementCommentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAddElementCommentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.topAppBar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // disable some views until quote is fetched
        val tempDisabledViews = arrayOf(
            binding.comment,
            binding.generateInvoice,
        ).also {
            it.forEach { view ->
                view.isEnabled = false
            }
        }

        // get quote and make enable generate invoice button on success
        viewLifecycleOwner.lifecycleScope.launch {
            val quote = try {
                CommentApi.getQuote()
            } catch (t: Throwable) {
                t.log()
                parentFragmentManager.popBackStack()
                return@launch
            } finally {
                tempDisabledViews.forEach { it.isEnabled = true }
            }

            binding.fee.text = getString(R.string.d_sat, quote.quoteSat.toString())
        }

        var addCommentResponse: CommentApi.AddCommentResponse? = null

        // send boost request and fetch an invoice
        binding.generateInvoice.setOnClickListener {
            tempDisabledViews.forEach { it.isEnabled = false }

            viewLifecycleOwner.lifecycleScope.launch {
                addCommentResponse = try {
                    CommentApi.addComment(
                        placeId = args.value.elementId,
                        comment = binding.comment.text.toString(),
                    )
                } catch (t: Throwable) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.error)
                        .setMessage(t.toString())
                        .setPositiveButton(R.string.close, null)
                        .show()
                    return@launch
                } finally {
                    tempDisabledViews.forEach { it.isEnabled = true }
                }

                val qrEncoder =
                    QRGEncoder(addCommentResponse.paymentRequest, null, QRGContents.Type.TEXT, 1000)
                qrEncoder.colorBlack = Color.BLACK
                qrEncoder.colorWhite = Color.WHITE
                val bitmap = qrEncoder.getBitmap(0)
                binding.qr.isVisible = true
                binding.qr.setImageBitmap(bitmap)
                binding.payInvoice.isVisible = true
                binding.copyInvoice.isVisible = true
            }
        }

        binding.payInvoice.setOnClickListener {
            val paymentRequest = addCommentResponse?.paymentRequest ?: return@setOnClickListener
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

        binding.copyInvoice.setOnClickListener {
            val paymentRequest = addCommentResponse?.paymentRequest ?: return@setOnClickListener
            val clipManager =
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipLabel = getString(R.string.btc_map_comment_payment_request)
            val clipText = paymentRequest
            clipManager.setPrimaryClip(ClipData.newPlainText(clipLabel, clipText))
            Toast.makeText(requireContext(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT)
                .show()
        }

        // once invoice is fetched, start polling it's status, till we know it's paid
        viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                val invoiceUuid = addCommentResponse?.invoiceUuid

                if (invoiceUuid == null) {
                    delay(50)
                    continue
                }

                val invoice = try {
                    InvoiceApi.getInvoice(invoiceUuid)
                } catch (_: Throwable) {
                    delay(500)
                    continue
                }

                if (invoice.paid) {
                    withResumed {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.your_comment_has_been_posted),
                            Toast.LENGTH_LONG,
                        ).show()
                        parentFragmentManager.popBackStack()
                    }
                } else {
                    delay(500)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}