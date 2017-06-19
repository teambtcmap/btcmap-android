import org.junit.Assert
import org.junit.Test

import java.io.IOException

/**
 * @author Igor Bubelov
 */

class CurrenciesRepositoryTest : BaseRobolectricTest() {
    private val repository = dependencies!!.currenciesRepository()

    @Test
    @Throws(IOException::class)
    fun currenciesRepository_ReloadsWithoutException() {
        repository.reloadFromApi()
    }

    @Test
    @Throws(IOException::class)
    fun currenciesRepository_HasBitcoin() {
        repository.reloadFromApi()
        Assert.assertTrue(repository.getCurrency("BTC") != null)
    }
}