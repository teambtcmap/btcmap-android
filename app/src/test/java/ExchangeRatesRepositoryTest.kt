import com.bubelov.coins.model.CurrencyPair
import com.bubelov.coins.repository.Result
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
    fun returnsRates() {
        CurrencyPair.values().forEach { pair ->
            repository.getExchangeRatesSources(pair).forEach { source ->
                System.out.println("Pair: $pair")
                System.out.println("Source: ${source.name}")
                val result = source.getExchangeRate(pair)
                System.out.println("Result: $result")
                Assert.assertTrue(result is Result.Success)
            }
        }
    }
}