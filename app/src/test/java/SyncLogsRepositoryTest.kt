import com.bubelov.coins.model.SyncLogEntry
import com.bubelov.coins.repository.synclogs.SyncLogsRepository
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import javax.inject.Inject

/**
 * @author Igor Bubelov
 */

class SyncLogsRepositoryTest : BaseRobolectricTest() {
    @Inject lateinit var repository: SyncLogsRepository

    @Before
    fun init() {
        TestInjector.testComponent.inject(this)
    }

    @Test
    fun isEmptyByDefault() {
        Assert.assertNotNull(repository.syncLogs)
        Assert.assertEquals(0, repository.syncLogs.size)
    }

    @Test
    fun savesNewEntry() {
        val entry = SyncLogEntry(time = System.currentTimeMillis(), affectedPlaces = 5)
        repository.addEntry(entry)
        Assert.assertEquals(1, repository.syncLogs.size)
        Assert.assertEquals(entry, repository.syncLogs.first())
    }
}