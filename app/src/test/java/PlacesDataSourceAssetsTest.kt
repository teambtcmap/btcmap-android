import com.bubelov.coins.repository.place.PlacesDataSourceAssets
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import javax.inject.Inject

/**
 * @author Igor Bubelov
 */

class PlacesDataSourceAssetsTest : BaseRobolectricTest() {
    @Inject lateinit var dataSource: PlacesDataSourceAssets

    @Before
    fun init() {
        TestInjector.testComponent.inject(this)
    }

    @Test
    fun isNotEmpty() {
        Assert.assertTrue(dataSource.getPlaces().isNotEmpty())
    }
}