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

    @Before
    fun init() {
        TestInjector.testComponent.inject(this)
    }

    @Test
    fun hasUsdBtcSources() {
        Assert.assertFalse(repository.getExchangeRatesSources("USD", "BTC").isEmpty())
    }
}