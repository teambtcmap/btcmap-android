import com.bubelov.coins.repository.place.PlacesAssetsCache
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import javax.inject.Inject

/**
 * @author Igor Bubelov
 */

class PlacesAssetsCacheTest : BaseRobolectricTest() {
    @Inject lateinit var cache: PlacesAssetsCache

    @Before
    fun init() {
        TestInjector.testComponent.inject(this)
    }

    @Test
    fun isNotEmpty() {
        Assert.assertTrue(cache.getPlaces().isNotEmpty())
    }
}