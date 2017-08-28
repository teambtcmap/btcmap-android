import com.bubelov.coins.repository.currency.CurrenciesRepository
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import java.io.IOException
import javax.inject.Inject

/**
 * @author Igor Bubelov
 */

class CurrenciesRepositoryTest : BaseRobolectricTest() {
    @Inject lateinit var repository: CurrenciesRepository

    @Before
    fun init() {
        TestInjector.testComponent.inject(this)
    }

    @Test
    @Throws(IOException::class)
    fun reloadsWithoutException() {
        repository.reloadFromApi()
    }

    @Test
    @Throws(IOException::class)
    fun hasBitcoin() {
        repository.reloadFromApi()
        Assert.assertTrue(repository.getCurrency("BTC") != null)
    }
}