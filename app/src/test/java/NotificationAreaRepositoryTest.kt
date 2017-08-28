import com.bubelov.coins.model.NotificationArea
import com.bubelov.coins.repository.area.NotificationAreaRepository
import org.junit.Assert
import org.junit.Before

import org.junit.Test
import javax.inject.Inject

/**
 * @author Igor Bubelov
 */

class NotificationAreaRepositoryTest : BaseRobolectricTest() {
    @Inject lateinit var repository: NotificationAreaRepository

    @Before
    fun init() {
        TestInjector.testComponent.inject(this)
    }

    @Test
    fun isNullByDefault() {
        Assert.assertTrue(repository.notificationArea == null)
    }

    @Test
    fun savesArea() {
        val area = NotificationArea(
                latitude = 50.0,
                longitude = 0.0,
                radius = 100.0
        )

        repository.notificationArea = area
        Assert.assertEquals(repository.notificationArea, area)
    }

    @Test
    fun clearsArea() {
        val area = NotificationArea(
                latitude = 50.0,
                longitude = 0.0,
                radius = 100.0
        )

        repository.notificationArea = area
        repository.notificationArea = null
        Assert.assertTrue(repository.notificationArea == null)
    }
}