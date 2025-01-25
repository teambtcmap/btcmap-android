package boost_element

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
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.coroutines.executeAsync
import org.btcmap.R
import org.btcmap.databinding.FragmentBoostElementBinding
import org.json.JSONObject
import java.text.NumberFormat

class BoostElementFragment : Fragment() {

    private data class Args(
        val elementId: Long,
    )

    private val args = lazy {
        Args(requireArguments().getLong("element_id"))
    }

    private var _binding: FragmentBoostElementBinding? = null
    private val binding get() = _binding!!

    private var invoice = ""

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
            withContext(Dispatchers.IO) {
                val httpClient = OkHttpClient()
                val url = "https://api.btcmap.org/rpc"
                val requestBody =
                    """{"jsonrpc": "2.0", "method": "paywall_get_boost_element_quote", "id": 1}""".trimIndent()
                val res = httpClient.newCall(
                    Request.Builder()
                        .post(requestBody.toRequestBody("application/json".toMediaType())).url(url)
                        .build()
                ).executeAsync()

                if (res.isSuccessful) {
                    val body = res.body.string()
                    Log.d("RPC", body)

                    withContext(Dispatchers.Main) {
                        onFeeResponse(JSONObject(body))
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
            withContext(Dispatchers.IO) {
                val httpClient = OkHttpClient()
                val url = "https://api.btcmap.org/rpc"
                val requestBody =
                    """{"jsonrpc": "2.0", "method": "paywall_boost_element", "params": {"element_id": "${args.value.elementId}", "days": $days}, "id": 1}""".trimIndent()
                val res = httpClient.newCall(
                    Request.Builder()
                        .post(requestBody.toRequestBody("application/json".toMediaType())).url(url)
                        .build()
                ).executeAsync()

                if (!res.isSuccessful) {
                    onRpcRequestFail(res.code)
                } else {
                    val body = res.body.string()
                    Log.d("RPC", body)

                    withContext(Dispatchers.Main) {
                        onPaymentRequestResponse(JSONObject(body))
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
        binding.durationOptions.isEnabled = true
        binding.boost1m.isEnabled = true
        binding.boost3m.isEnabled = true
        binding.boost12m.isEnabled = true
        binding.generateInvoice.isEnabled = true
    }

    private fun onFeeResponse(rpcResponse: JSONObject) {
        if (rpcResponse.has("error")) {
            return
        }

        val quote30d = rpcResponse.getJSONObject("result").getString("quote_30d_sat")
        val quote90d = rpcResponse.getJSONObject("result").getString("quote_90d_sat")
        val quote365d = rpcResponse.getJSONObject("result").getString("quote_365d_sat")

        binding.boost1m.append(" - ")
        binding.boost1m.append(getString(R.string.d_sat, NumberFormat.getNumberInstance().format(quote30d.toInt())))

        binding.boost3m.append(" - ")
        binding.boost3m.append(getString(R.string.d_sat, NumberFormat.getNumberInstance().format(quote90d.toInt())))

        binding.boost12m.append(" - ")
        binding.boost12m.append(getString(R.string.d_sat, NumberFormat.getNumberInstance().format(quote365d.toInt())))
    }

    private fun onPaymentRequestResponse(rpcResponse: JSONObject) {
        if (rpcResponse.has("error")) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.error)
                .setMessage(rpcResponse.getJSONObject("error").toString())
                .setPositiveButton(R.string.close, null)
                .setOnDismissListener {
                    binding.durationOptions.isEnabled = true
                    binding.boost1m.isEnabled = true
                    binding.boost3m.isEnabled = true
                    binding.boost12m.isEnabled = true
                    binding.generateInvoice.isEnabled = true
                }
                .show()
            return
        }

        invoice = rpcResponse.getJSONObject("result").getString("payment_request")
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
        val clipLabel = "BTC Map Boost Payment Request"
        val clipText = invoice
        clipManager.setPrimaryClip(ClipData.newPlainText(clipLabel, clipText))
        Toast.makeText(requireContext(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }
}