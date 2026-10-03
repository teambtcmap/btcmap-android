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
import org.btcmap.api.PlaceBoostQuoteResponse
import org.btcmap.api.awaitPaidInvoice
import org.btcmap.api.boostPlace
import org.btcmap.api.getPlaceBoostQuote
import org.btcmap.payment.InvoicePaymentFlow
import org.btcmap.payment.PaymentInvoice
import org.btcmap.ui.BoostForm
import org.btcmap.ui.BoostFormUiState
import org.btcmap.ui.BoostOption
import org.btcmap.util.rethrowIfCancellation

/** The boost durations the API accepts, mirroring the app's enum. */
private data class BoostDuration(val key: String, val days: Long, val label: String)

private val BOOST_DURATIONS = listOf(
    BoostDuration("ONE_MONTH", 30L, "1 month"),
    BoostDuration("THREE_MONTHS", 90L, "3 months"),
    BoostDuration("TWELVE_MONTHS", 365L, "12 months"),
)

private const val DEFAULT_BOOST_KEY = "THREE_MONTHS"

private fun BoostDuration.priceSat(quote: PlaceBoostQuoteResponse): Long = when (key) {
    "ONE_MONTH" -> quote.quote30dSat
    "THREE_MONTHS" -> quote.quote90dSat
    else -> quote.quote365dSat
}

/**
 * The desktop's boost screen: the fee quote, the duration choices and the
 * Lightning invoice the boost is paid with. The quote, order and invoice state
 * come from the shared [InvoicePaymentFlow], so it behaves exactly like the
 * app; the boost is applied by the server once the invoice is paid, which the
 * poll below watches for.
 */
@Composable
internal fun DesktopBoostScreen(
    api: Api,
    placeId: Long,
    placeName: String,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val flow = remember(api, placeId) {
        InvoicePaymentFlow(
            quoteLoader = { api.getPlaceBoostQuote() },
            scope = scope,
        )
    }
    val state by flow.state.collectAsState()
    var submitted by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(flow) { flow.loadQuote() }

    LaunchedEffect(flow) {
        flow.events.collect { event ->
            error = event.error.message ?: event.error.toString()
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

    ScreenPage(title = placeName.ifBlank { "Boost merchant" }, onBack = onBack) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            if (submitted) {
                Text("Your boost is active.")
                Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Back to the map")
                }
                return@Column
            }

            val options = BOOST_DURATIONS.map { duration ->
                val price = state.quote?.let { formatSat(duration.priceSat(it)) }
                BoostOption(
                    key = duration.key,
                    label = price?.let { "${duration.label} - $it" } ?: duration.label,
                )
            }

            BoostForm(
                state = BoostFormUiState(
                    description = "Boosting a place keeps it at the top of search results " +
                        "and highlights it on the map.",
                    durationTitle = "For how long?",
                    options = options,
                    continueLabel = "Continue",
                    selectedKey = DEFAULT_BOOST_KEY,
                    optionsEnabled = state.quote != null && !state.ordering && state.invoice == null,
                    actionsEnabled = state.actionsEnabled,
                    showContinue = state.invoice == null,
                ),
                onContinue = { key ->
                    BOOST_DURATIONS.firstOrNull { it.key == key }?.let { duration ->
                        error = null
                        flow.order {
                            val response = api.boostPlace(placeId = placeId, days = duration.days)
                            PaymentInvoice(id = response.invoiceId, bolt11 = response.invoice)
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
