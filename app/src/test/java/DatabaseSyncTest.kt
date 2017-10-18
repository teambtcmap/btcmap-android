import com.bubelov.coins.database.dao.PlaceDao
import com.bubelov.coins.database.sync.DatabaseSync
import com.bubelov.coins.repository.place.PlacesRepository
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import javax.inject.Inject

/**
 * @author Igor Bubelov
 */

class DatabaseSyncTest : BaseRobolectricTest() {
    @Inject lateinit var placesRepository: PlacesRepository

    @Inject lateinit var databaseSync: DatabaseSync

    @Inject lateinit var placeDao: PlaceDao

    @Before
    fun init() {
        TestInjector.testComponent.inject(this)
    }

    @Test
    fun syncing() {
        runBlocking {
            placesRepository.getPlaces("").blockingObserve(2)
            val placesBeforeSync = placeDao.count()
            Assert.assertTrue(placesBeforeSync > 0)
            databaseSync.start().join()
            Assert.assertNotEquals(placesBeforeSync, placeDao.count())
        }
    }
}