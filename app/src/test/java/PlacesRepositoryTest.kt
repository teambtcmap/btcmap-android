import org.junit.Assert
import org.junit.Test

/**
 * @author Igor Bubelov
 */

class PlacesRepositoryTest : BaseRobolectricTest() {
    private val repository = dependencies!!.placesRepository()

    @Test
    fun placesRepository_FetchNewPlaces_NotEmpty() {
        Assert.assertFalse(repository.fetchNewPlaces().isEmpty())
    }

    @Test
    fun placesRepository_SavesNewPlaces() {
        val newPlaces = repository.fetchNewPlaces()
        Assert.assertFalse(newPlaces.isEmpty())
        Assert.assertNotNull(repository.getPlace(newPlaces[0].id))
    }
}