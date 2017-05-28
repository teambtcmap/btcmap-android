import org.junit.Assert
import org.junit.Test

/**
 * @author Igor Bubelov
 */

class PlacesRepositoryTest : BaseRobolectricTest() {
    private val repository = lazy { dependencies.value.placesRepository() }

    @Test
    fun placesRepository_FetchNewPlaces_NotEmpty() {
        Assert.assertFalse(repository.value.fetchNewPlaces().isEmpty())
    }

    @Test
    fun placesRepository_SavesNewPlaces() {
        val newPlaces = repository.value.fetchNewPlaces()
        Assert.assertFalse(newPlaces.isEmpty())
        Assert.assertNotNull(repository.value.getPlace(newPlaces[0].id()))
    }
}