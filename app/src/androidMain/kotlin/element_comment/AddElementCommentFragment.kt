package element_comment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.coroutines.executeAsync
import org.btcmap.R
import org.btcmap.databinding.FragmentAddElementCommentBinding
import org.json.JSONObject

class AddElementCommentFragment : Fragment() {

    private var _binding: FragmentAddElementCommentBinding? = null
    private val binding get() = _binding!!

    private var elementId = 0L
    private var invoice = ""

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

        elementId = requireArguments().getLong("element_id")

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.generateInvoice.setOnClickListener { onGenerateInvoiceButtonClick() }
        binding.payInvoice.setOnClickListener { onPayInvoiceClick() }
        binding.copyInvoice.setOnClickListener { onCopyInvoiceClick() }

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

    private fun onGenerateInvoiceButtonClick() {
        if (binding.comment.length() == 0) {
            return
        }

        val comment = binding.comment.text.toString()

        binding.comment.isEnabled = false
        binding.generateInvoice.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val httpClient = OkHttpClient()
                val url = "https://api.btcmap.org/rpc"
                val requestBody =
                    """{"jsonrpc": "2.0", "method": "add_paid_element_comment", "params": {"element_id": "$elementId", "comment": "$comment"}, "id": 1}""".trimIndent()
                val res = httpClient.newCall(
                    Request.Builder().post(requestBody.toRequestBody("application/json".toMediaType())).url(url).build()
                ).executeAsync()

                if (!res.isSuccessful) {
                    onRpcRequestFail(res.code)
                } else {
                    val body = res.body.string()
                    Log.d("RPC", body)
                    val json = JSONObject(body)
                    val result = json.getJSONObject("result")

                    withContext(Dispatchers.Main) {
                        onPaymentRequestReceived(result.getString("payment_request"))
                    }
                }
            }
        }
    }

    private fun onRpcRequestFail(code: Int) {
        Toast.makeText(
            requireContext(),
            "Unexpected response code: $code",
            Toast.LENGTH_LONG,
        ).show()
        binding.comment.isEnabled = true
        binding.generateInvoice.isEnabled = true
    }

    private fun onPaymentRequestReceived(paymentRequest: String) {
        invoice = paymentRequest
        val qrEncoder = QRGEncoder(invoice, null, QRGContents.Type.TEXT, 1000)
        qrEncoder.colorBlack = Color.BLACK
        qrEncoder.colorWhite = Color.WHITE
        val bitmap = qrEncoder.getBitmap(0)
        binding.qr.isVisible = true
        binding.qr.setImageBitmap(bitmap)
        binding.invoiceHint.isVisible = true
        binding.payInvoice.isVisible = true
        binding.copyInvoice.isVisible = true
    }

    private fun onPayInvoiceClick() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("lightning:$invoice")
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
        val clipLabel = "BTC Map Comment Payment Request"
        val clipText = invoice
        clipManager.setPrimaryClip(ClipData.newPlainText(clipLabel, clipText))
        Toast.makeText(requireContext(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }
}