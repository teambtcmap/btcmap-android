import org.junit.Assert
import org.junit.Test

import java.io.IOException

/**
 * @author Igor Bubelov
 */

class CurrenciesRepositoryTest : BaseRobolectricTest() {
    private val repository = lazy { dependencies.value.currenciesRepository() }

    @Test
    @Throws(IOException::class)
    fun currenciesRepository_ReloadsWithoutException() {
        repository.value.reloadFromApi()
    }

    @Test
    @Throws(IOException::class)
    fun currenciesRepository_HasBitcoin() {
        repository.value.reloadFromApi()
        Assert.assertTrue(repository.value.getCurrency("BTC") != null)
    }
}