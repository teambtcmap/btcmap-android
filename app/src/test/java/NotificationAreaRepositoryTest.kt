import com.bubelov.coins.model.NotificationArea
import org.junit.Assert

import org.junit.Test

/**
 * @author Igor Bubelov
 */

class NotificationAreaRepositoryTest : BaseRobolectricTest() {
    private val repository = dependencies!!.notificationAreaRepository()

    @Test
    fun notificationAreaRepository_NullByDefault() {
        Assert.assertTrue(repository.notificationArea == null)
    }

    @Test
    fun notificationAreaRepository_AreaSaved() {
        val area = NotificationArea(
                latitude = 50.0,
                longitude = 0.0,
                radius = 100.0
        )

        repository.notificationArea = area
        Assert.assertEquals(repository.notificationArea, area)
    }

    @Test
    fun notificationAreaRepository_areaCleared() {
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