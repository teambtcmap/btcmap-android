import com.bubelov.coins.model.CurrencyPair
import com.bubelov.coins.repository.Result
import com.bubelov.coins.repository.rate.Coinbase
import com.bubelov.coins.repository.rate.ExchangeRatesRepository
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import javax.inject.Inject

/**
 * @author Igor Bubelov
 */

class ExchangeRatesRepositoryTest : BaseRobolectricTest() {
    @Inject lateinit var repository: ExchangeRatesRepository

    @Inject lateinit var coinbase: Coinbase

    @Before
    fun init() {
        TestInjector.testComponent.inject(this)
    }

    @Test
    fun hasUsdBtcSources() {
        Assert.assertFalse(repository.getExchangeRatesSources(CurrencyPair.BTC_USD).isEmpty())
    }

    @Test
    fun coinbaseIsWorking() {
        val result = coinbase.getExchangeRate(CurrencyPair.BTC_USD)
        System.out.println(result)
        Assert.assertTrue(result is Result.Success)
    }
}