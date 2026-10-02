package org.btcmap.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.btcmap.api.Api
import org.btcmap.api.addComment
import org.btcmap.api.awaitPaidInvoice
import org.btcmap.api.getCommentQuote
import org.btcmap.ui.AddCommentForm
import org.btcmap.ui.AddCommentLabels
import org.btcmap.ui.AddCommentUiState
import org.btcmap.util.rethrowIfCancellation

private val COMMENT_LABELS = AddCommentLabels(
    disclosure = "A comment carries a small anti-spam fee, paid in sats over the " +
        "Lightning Network.",
    currentFee = "Current fee",
    comment = "Comment",
    placeholder = "Your comment",
    continueLabel = "Continue",
    emptyComment = "The comment cannot be empty.",
    failedToLoad = "The fee could not be loaded.",
    tapToRetry = "Tap to retry",
)

/**
 * The desktop's add-comment screen: the shared form, the fee quote, and the
 * Lightning invoice the comment is paid with. The comment is posted by the
 * server once the invoice is paid, which the poll below watches for.
 */
@Composable
internal fun DesktopAddCommentScreen(
    api: Api,
    placeId: Long,
    placeName: String,
    onBack: () -> Unit,
) {
    var quoteSat by remember { mutableStateOf<Long?>(null) }
    var loadingQuote by remember { mutableStateOf(true) }
    var quoteFailed by remember { mutableStateOf(false) }
    var ordering by remember { mutableStateOf(false) }
    var invoice by remember { mutableStateOf<Invoice?>(null) }
    var submitted by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun loadQuote() {
        loadingQuote = true
        quoteFailed = false
        try {
            quoteSat = api.getCommentQuote().quoteSat
        } catch (t: Throwable) {
            t.rethrowIfCancellation()
            quoteFailed = true
        } finally {
            loadingQuote = false
        }
    }

    LaunchedEffect(Unit) { loadQuote() }

    LaunchedEffect(invoice?.id) {
        val id = invoice?.id ?: return@LaunchedEffect
        try {
            api.awaitPaidInvoice(id)
            submitted = true
        } catch (t: Throwable) {
            t.rethrowIfCancellation()
            error = t.message ?: t.toString()
        }
    }

    ScreenPage(title = placeName.ifBlank { "Add comment" }, onBack = onBack) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            if (submitted) {
                Text("Your comment has been posted.")
                Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Back to the map")
                }
                return@Column
            }

            AddCommentForm(
                state = AddCommentUiState(
                    quote = quoteSat?.let(::formatSat),
                    loadingQuote = loadingQuote,
                    quoteFailed = quoteFailed && invoice == null,
                    inputEnabled = invoice == null && !ordering,
                    actionsEnabled = quoteSat != null && !loadingQuote && !ordering && invoice == null,
                    ordering = ordering,
                    showContinue = invoice == null,
                    labels = COMMENT_LABELS,
                ),
                onRetry = { scope.launch { loadQuote() } },
                onContinue = { text ->
                    ordering = true
                    error = null
                    scope.launch {
                        try {
                            val response = api.addComment(placeId = placeId, comment = text)
                            invoice = Invoice(id = response.invoiceId, bolt11 = response.invoice)
                        } catch (t: Throwable) {
                            t.rethrowIfCancellation()
                            error = t.message ?: t.toString()
                        } finally {
                            ordering = false
                        }
                    }
                },
            )

            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            invoice?.let {
                InvoicePaymentSection(
                    invoice = it,
                    onStartOver = { invoice = null },
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}
