import com.bubelov.coins.repository.ApiResult
import com.bubelov.coins.repository.place.PlacesRepository
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import javax.inject.Inject

/**
 * @author Igor Bubelov
 */

class PlacesRepositoryTest : BaseRobolectricTest() {
    @Inject lateinit var repository: PlacesRepository

    @Before
    fun init() {
        TestInjector.testComponent.inject(this)
    }

    @Test
    fun isNotEmptyByDefault() {
        assertTrue(repository.allPlaces.blockingObserve(2).isNotEmpty())
    }

    @Test
    fun returnsRandomPlace() {
        assertNotNull(repository.getRandomPlace().blockingObserve(2))
    }

    @Test
    fun fetchesNewPlaces() {
        val result = repository.fetchNewPlaces()
        assertTrue(result is ApiResult.Success && !result.data.isEmpty())
    }
}