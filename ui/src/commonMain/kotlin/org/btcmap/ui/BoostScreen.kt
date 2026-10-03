package org.btcmap.ui

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
import org.btcmap.api.awaitPaidInvoice
import org.btcmap.api.boostPlace
import org.btcmap.api.getPlaceBoostQuote
import org.btcmap.boost.BoostPlan
import org.btcmap.payment.InvoicePaymentFlow
import org.btcmap.payment.PaymentInvoice
import org.btcmap.util.rethrowIfCancellation

/** The boost screen's strings, so the screen stays resource-free. */
data class BoostScreenLabels(
    val active: String,
    val backToMap: String,
    val planLabel: (BoostPlan) -> String,
    val description: String,
    val durationTitle: String,
    val continueLabel: String,
    val invoice: InvoicePaymentSectionLabels,
)

/**
 * The boost screen: the fee quote, the duration choices and the Lightning
 * invoice the boost is paid with. The quote, order and invoice state come from
 * the shared [InvoicePaymentFlow]; the boost is applied by the server once the
 * invoice is paid, which the poll below watches for.
 *
 * The host supplies [onPay] and [onCopy] and its own top bar and back
 * affordance.
 */
@Composable
fun BoostScreen(
    api: Api,
    placeId: Long,
    labels: BoostScreenLabels,
    onPay: (bolt11: String) -> Unit,
    onCopy: (bolt11: String) -> Unit,
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

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        if (submitted) {
            Text(labels.active)
            Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
                Text(labels.backToMap)
            }
            return@Column
        }

        val options = BoostPlan.entries.map { plan ->
            val price = state.quote?.let { formatSat(plan.priceSat(it)) }
            BoostOption(
                key = plan.name,
                label = price?.let { "${labels.planLabel(plan)} - $it" } ?: labels.planLabel(plan),
            )
        }

        BoostForm(
            state = BoostFormUiState(
                description = labels.description,
                durationTitle = labels.durationTitle,
                options = options,
                continueLabel = labels.continueLabel,
                selectedKey = BoostPlan.THREE_MONTHS.name,
                optionsEnabled = state.quote != null && !state.ordering && state.invoice == null,
                actionsEnabled = state.actionsEnabled,
                showContinue = state.invoice == null,
            ),
            onContinue = { key ->
                BoostPlan.entries.firstOrNull { it.name == key }?.let { plan ->
                    error = null
                    flow.order {
                        val response = api.boostPlace(placeId = placeId, days = plan.days)
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
                labels = labels.invoice,
                onPay = onPay,
                onCopy = onCopy,
                onStartOver = flow::startOver,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}
