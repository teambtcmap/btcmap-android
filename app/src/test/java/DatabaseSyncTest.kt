import com.bubelov.coins.repository.place.PlacesDb
import com.bubelov.coins.db.sync.DatabaseSync
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

    @Inject lateinit var placesDb: PlacesDb

    @Before
    fun init() {
        TestInjector.testComponent.inject(this)
    }

    @Test
    fun syncing() {
        runBlocking {
            placesRepository.getPlaces("").blockingObserve()
            val placesBeforeSync = placesDb.count()
            Assert.assertTrue(placesBeforeSync > 0)
            databaseSync.start().join()
            Assert.assertNotEquals(placesBeforeSync, placesDb.count())
        }
    }
}