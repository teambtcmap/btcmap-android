import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * @author Igor Bubelov
 */

class PlacesRepositoryTest : BaseRobolectricTest() {
    private val repository = dependencies!!.placesRepository()

    @Test
    fun isNotEmptyByDefault() {
        assertTrue(repository.getPlaces("").isNotEmpty())
    }

    @Test
    fun returnsRandomPlace() {
        assertNotNull(repository.getRandomPlace())
    }

    @Test
    fun fetchesNewPlaces() {
        val newPlaces = repository.fetchNewPlaces()
        assertTrue(newPlaces.isNotEmpty())
    }
}