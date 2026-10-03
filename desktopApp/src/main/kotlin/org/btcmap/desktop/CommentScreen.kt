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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.btcmap.api.Api
import org.btcmap.api.addComment
import org.btcmap.api.awaitPaidInvoice
import org.btcmap.api.getCommentQuote
import org.btcmap.payment.InvoicePaymentFlow
import org.btcmap.payment.PaymentEvent
import org.btcmap.payment.PaymentInvoice
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
 * Lightning invoice the comment is paid with. The quote, order and invoice
 * state come from the shared [InvoicePaymentFlow]; the comment is posted by the
 * server once the invoice is paid, which the poll below watches for.
 */
@Composable
internal fun DesktopAddCommentScreen(
    api: Api,
    placeId: Long,
    placeName: String,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val flow = remember(api, placeId) {
        InvoicePaymentFlow(
            quoteLoader = { api.getCommentQuote() },
            scope = scope,
        )
    }
    val state by flow.state.collectAsState()
    var submitted by remember { mutableStateOf(false) }
    var quoteFailed by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(flow) { flow.loadQuote() }

    LaunchedEffect(flow) {
        flow.events.collect { event ->
            when (event) {
                is PaymentEvent.QuoteFailed -> quoteFailed = true
                else -> error = event.error.message ?: event.error.toString()
            }
        }
    }

    LaunchedEffect(state.invoice?.id) {
        val id = state.invoice?.id ?: return@LaunchedEffect
        try {
            api.awaitPaidInvoice(id)
            submitted = true
        } catch (t: Throwable) {
            t.rethrowIfCancellation()
            flow.reportPaymentFailure(t)
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
                    quote = state.quote?.quoteSat?.let(::formatSat),
                    loadingQuote = state.loadingQuote,
                    quoteFailed = quoteFailed && state.invoice == null,
                    inputEnabled = state.inputEnabled,
                    actionsEnabled = state.actionsEnabled,
                    ordering = state.ordering,
                    showContinue = state.invoice == null,
                    labels = COMMENT_LABELS,
                ),
                onRetry = {
                    quoteFailed = false
                    flow.loadQuote()
                },
                onContinue = { text ->
                    error = null
                    flow.order {
                        val response = api.addComment(placeId = placeId, comment = text)
                        PaymentInvoice(id = response.invoiceId, bolt11 = response.invoice)
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

            state.invoice?.let {
                InvoicePaymentSection(
                    invoice = it,
                    onStartOver = flow::startOver,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}
