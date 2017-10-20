import com.bubelov.coins.model.Place
import com.bubelov.coins.repository.place.PlacesDb
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.*
import javax.inject.Inject

/**
 * @author Igor Bubelov
 */

class PlacesDbTests: BaseRobolectricTest() {
    @Inject lateinit var placesDb: PlacesDb

    private val cafe = Place(
            id = 1,
            name = "Coffee Corner",
            latitude = 50.0,
            longitude = 1.5,
            category = "cafe",
            description = "Best coffee in town!",
            currencies = arrayListOf("BTC", "ZEC"),
            openedClaims = 10,
            closedClaims = 1,
            phone = "12345",
            website = "https://foo.bar",
            openingHours = "7AM-5PM",
            visible = true,
            updatedAt = Date()
    )

    @Before
    fun setUp() {
        TestInjector.testComponent.inject(this)
    }

    @Test
    fun isEmptyByDefault() {
        assertEquals(0, placesDb.count())
    }

    @Test
    fun insertsPlace() {
        assertEquals(0, placesDb.count())
        placesDb.insert(cafe)
        assertEquals(1, placesDb.count())
        assertEquals(cafe, placesDb.findById(cafe.id).blockingObserve())
        assertEquals(cafe, placesDb.all().blockingObserve()[0])
    }

    @Test
    fun searchIsWorking() {
        (1..100L).forEach {
            placesDb.insert(cafe.copy(id = it, description = cafe.description + it))
        }

        assertEquals(100, placesDb.count())
        assertEquals(1, placesDb.findBySearchQuery("%${cafe.description}55%").blockingObserve().size)
    }
}