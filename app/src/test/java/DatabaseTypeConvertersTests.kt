import com.bubelov.coins.database.Converters
import junit.framework.Assert
import org.junit.Test
import java.util.*

/**
 * @author Igor Bubelov
 */

class DatabaseTypeConvertersTests : BaseRobolectricTest() {
    private val converters = Converters()

    private val date = Date()

    @Test
    fun convertsLongToDate() {
        Assert.assertEquals(date, converters.longToDate(date.time))
    }

    @Test
    fun convertsDateToLong() {
        Assert.assertEquals(date.time, converters.dateToLong(date))
    }

    @Test
    fun convertsStringToArrayList() {
        Assert.assertEquals(arrayListOf("1", "2"), converters.stringToArrayList("[\"1\",\"2\"]"))
    }

    @Test
    fun convertsArrayListToString() {
        Assert.assertEquals("[\"1\",\"2\"]", converters.arrayListToString(arrayListOf("1", "2")))
    }
}