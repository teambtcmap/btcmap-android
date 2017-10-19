import com.bubelov.coins.util.UtcDateTypeAdapter
import org.junit.Assert
import org.junit.Test
import java.util.*

/**
 * @author Igor Bubelov
 */

class UtcDateTypeAdapterTest : BaseRobolectricTest() {
    private val adapter = UtcDateTypeAdapter()

    @Test
    fun convertsDateToString() {
        val date = Date(1504586827857)
        Assert.assertEquals("2017-09-05T04:47:07.857Z", adapter.format(date))
    }

    @Test
    fun convertsStringToDate() {
        val date = adapter.parse("2017-09-05T04:47:07.857Z")
        Assert.assertNotNull(date)
        Assert.assertEquals(Date(1504586827857), date)
    }
}