import org.junit.Assert
import org.junit.Test

/**
 * @author Igor Bubelov
 */

class ExchangeRatesRepositoryTest : BaseRobolectricTest() {
    private val repository = lazy { dependencies.value.exchangeRatesRepository() }

    @Test
    fun exchangeRatesRepository_LoadsAnyRates() {
        Assert.assertFalse(repository.value.exchangeRates.isEmpty())
    }
}