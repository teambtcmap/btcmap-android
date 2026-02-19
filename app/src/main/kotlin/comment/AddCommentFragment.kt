package comment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
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
import androidx.core.net.toUri
import androidx.lifecycle.withResumed
import api.CommentApi
import api.InvoiceApi
import api.InvoiceApi.paid
import kotlinx.coroutines.delay
import org.btcmap.databinding.AddCommentFragmentBinding

class AddCommentFragment : Fragment() {

    private data class Args(
        val placeId: Long,
    )

    private val args by lazy {
        Args(requireArguments().getLong("place_id"))
    }

    private var _binding: AddCommentFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = AddCommentFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.topAppBar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // disable invoice generation until quote is fetched
        binding.generateInvoice.isEnabled = false

        // get quote and enable generate invoice button on success
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val quote = CommentApi.getQuote()
                withResumed {
                    binding.fee.text = getString(R.string.d_sat, quote.quoteSat.toString())
                    binding.generateInvoice.isEnabled = true
                }
            } catch (t: Throwable) {
                Log.e(null, null, t)
                withResumed {
                    parentFragmentManager.popBackStack()
                    MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.error)
                        .setMessage(t.toString())
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }
                return@launch
            }
        }

        // send comment request and fetch an invoice
        binding.generateInvoice.setOnClickListener {
            val commentText = binding.comment.text.toString().trim()
            if (commentText.isEmpty()) {
                Toast.makeText(requireContext(), R.string.comment_cannot_be_empty, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.comment.isEnabled = false
            binding.generateInvoice.isEnabled = false

            viewLifecycleOwner.lifecycleScope.launch {
                val addCommentResponse = try {
                    CommentApi.addComment(
                        placeId = args.placeId,
                        comment = commentText,
                    )
                } catch (t: Throwable) {
                    Log.e(null, null, t)
                    withResumed {
                        binding.comment.isEnabled = true
                        binding.generateInvoice.isEnabled = true
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.error)
                            .setMessage(t.toString())
                            .setPositiveButton(R.string.close, null)
                            .show()
                    }
                    return@launch
                }

                // if invoice is fetched, monitor its status in background
                launch {
                    while (true) {
                        val invoice = try {
                            InvoiceApi.getInvoice(addCommentResponse.invoiceId)
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

                withResumed {
                    val qrEncoder =
                        QRGEncoder(addCommentResponse.invoice, null, QRGContents.Type.TEXT, 1000)
                    qrEncoder.colorBlack = Color.BLACK
                    qrEncoder.colorWhite = Color.WHITE
                    val bitmap = qrEncoder.getBitmap(0)

                    binding.qr.isVisible = true
                    binding.qr.setImageBitmap(bitmap)

                    binding.payInvoice.isVisible = true
                    binding.payInvoice.setOnClickListener {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = "lightning:${addCommentResponse.invoice}".toUri()
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

                    binding.copyInvoice.isVisible = true
                    binding.copyInvoice.setOnClickListener {
                        val clipManager =
                            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clipLabel = getString(R.string.btc_map_comment_payment_request)
                        clipManager.setPrimaryClip(
                            ClipData.newPlainText(
                                clipLabel,
                                addCommentResponse.invoice,
                            )
                        )
                        Toast.makeText(
                            requireContext(),
                            R.string.copied_to_clipboard,
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}