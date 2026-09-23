package org.btcmap.payment

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.RecordedRequest
import org.btcmap.Activity
import org.btcmap.App
import org.btcmap.R
import org.btcmap.boost.BoostFragment
import org.btcmap.comment.AddCommentFragment
import org.btcmap.util.AppTestCase
import org.junit.Rule
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

internal const val BOOST_TAG = "boost"
internal const val COMMENT_TAG = "comment"

internal const val BOOST_QUOTE_JSON =
    """{"quote_30d_sat":5000,"quote_90d_sat":10000,"quote_365d_sat":30000}"""
internal const val BOOST_INVOICE_JSON = """{"invoice_id":"boost-1","invoice":"lnbc-boost"}"""
internal const val COMMENT_QUOTE_JSON = """{"quote_sat":1000}"""
internal const val COMMENT_INVOICE_JSON = """{"invoice_id":"comment-1","invoice":"lnbc-comment"}"""

internal fun jsonResponse(body: String, code: Int = 200): MockResponse =
    MockResponse.Builder()
        .code(code)
        .addHeader("Content-Type", "application/json")
        .body(body)
        .build()

internal fun invoiceJson(id: String, status: String): String =
    """{"id":"$id","status":"$status"}"""

/**
 * Serves a quote, an order and a sequence of invoice statuses. Each request kind
 * is counted so a test can prove an order is only ever placed once.
 */
internal class PaymentDispatcher(
    private val quotePath: String,
    private val quoteBody: String,
    private val orderPath: String,
    private val orderBody: String,
    private val quoteCode: Int = 200,
    private val orderCode: Int = 200,
    private val invoiceStatuses: List<String> = listOf("unpaid"),
) : Dispatcher() {

    val quoteRequests = AtomicInteger()
    val orderRequests = AtomicInteger()
    val invoiceRequests = AtomicInteger()

    /** Order request bodies, in the order they arrived. */
    val orderBodies = CopyOnWriteArrayList<String>()

    override fun dispatch(request: RecordedRequest): MockResponse {
        val path = request.url.encodedPath
        return when {
            path == quotePath -> {
                quoteRequests.incrementAndGet()
                jsonResponse(quoteBody, quoteCode)
            }

            path == orderPath && request.method == "POST" -> {
                orderRequests.incrementAndGet()
                orderBodies.add(request.body?.utf8() ?: "")
                jsonResponse(orderBody, orderCode)
            }

            path.startsWith("/v4/invoices/") -> {
                val index = invoiceRequests.getAndIncrement()
                val status = invoiceStatuses.getOrElse(index) { invoiceStatuses.last() }
                jsonResponse(invoiceJson(path.substringAfterLast('/'), status))
            }

            else -> jsonResponse("[]")
        }
    }
}

internal fun boostDispatcher(
    quoteBody: String = BOOST_QUOTE_JSON,
    quoteCode: Int = 200,
    orderBody: String = BOOST_INVOICE_JSON,
    orderCode: Int = 200,
    invoiceStatuses: List<String> = listOf("unpaid"),
): PaymentDispatcher = PaymentDispatcher(
    quotePath = "/v4/place-boosts/quote",
    quoteBody = quoteBody,
    orderPath = "/v4/place-boosts",
    orderBody = orderBody,
    quoteCode = quoteCode,
    orderCode = orderCode,
    invoiceStatuses = invoiceStatuses,
)

internal fun commentDispatcher(
    quoteBody: String = COMMENT_QUOTE_JSON,
    quoteCode: Int = 200,
    orderBody: String = COMMENT_INVOICE_JSON,
    orderCode: Int = 200,
    invoiceStatuses: List<String> = listOf("unpaid"),
): PaymentDispatcher = PaymentDispatcher(
    quotePath = "/v4/place-comments/quote",
    quoteBody = quoteBody,
    orderPath = "/v4/place-comments",
    orderBody = orderBody,
    quoteCode = quoteCode,
    orderCode = orderCode,
    invoiceStatuses = invoiceStatuses,
)

abstract class PaymentScreenTest : AppTestCase() {

    protected val app = ApplicationProvider.getApplicationContext<App>()

    protected fun withBoost(
        addToBackStack: Boolean = false,
        block: (ActivityScenario<Activity>, BoostFragment) -> Unit,
    ) {
        ActivityScenario.launch(Activity::class.java).use { scenario ->
            lateinit var fragment: BoostFragment
            scenario.onActivity { activity ->
                fragment = BoostFragment().apply {
                    arguments = Bundle().apply { putLong("place_id", 1L) }
                }
                showFragment(activity, fragment, BOOST_TAG, addToBackStack)
            }
            block(scenario, fragment)
        }
    }

    protected fun withComment(
        addToBackStack: Boolean = false,
        block: (ActivityScenario<Activity>, AddCommentFragment) -> Unit,
    ) {
        ActivityScenario.launch(Activity::class.java).use { scenario ->
            lateinit var fragment: AddCommentFragment
            scenario.onActivity { activity ->
                fragment = AddCommentFragment().apply {
                    arguments = Bundle().apply { putLong("place_id", 1L) }
                }
                showFragment(activity, fragment, COMMENT_TAG, addToBackStack)
            }
            block(scenario, fragment)
        }
    }

    protected fun restoredFragment(scenario: ActivityScenario<Activity>, tag: String): Fragment {
        lateinit var fragment: Fragment
        scenario.onActivity {
            fragment = requireNotNull(it.supportFragmentManager.findFragmentByTag(tag))
        }
        return fragment
    }

    private fun showFragment(
        activity: Activity,
        fragment: Fragment,
        tag: String,
        addToBackStack: Boolean,
    ) {
        activity.supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.fragmentContainerView, fragment, tag)
            if (addToBackStack) addToBackStack(null)
        }
        activity.supportFragmentManager.executePendingTransactions()
    }
}
