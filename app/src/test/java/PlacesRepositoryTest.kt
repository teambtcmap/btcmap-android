import com.bubelov.coins.repository.Result
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
        assertNotNull(repository.getRandomPlace().blockingObserve(2))
    }

    @Test
    fun returnsRandomPlace() {
        assertNotNull(repository.getRandomPlace().blockingObserve(2))
    }

    @Test
    fun fetchesNewPlaces() {
        val result = repository.fetchNewPlaces()
        assertTrue(result is Result.Success && !result.data.isEmpty())
    }

    @Test
    fun returnsCurrenciesToPlaces() {
        val currenciesToPlaces = repository.getCurrenciesToPlacesMap().blockingObserve(2)
        assertNotNull(currenciesToPlaces)
        assertTrue(currenciesToPlaces.size > 1)
    }
}