import org.junit.Assert
import org.junit.Test

/**
 * @author Igor Bubelov
 */

class ExchangeRatesRepositoryTest : BaseRobolectricTest() {
    private val repository = dependencies!!.exchangeRatesRepository()

    @Test
    fun exchangeRatesRepository_LoadsAnyRates() {
        Assert.assertFalse(repository.exchangeRates.isEmpty())
    }
}