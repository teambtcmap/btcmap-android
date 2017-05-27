import com.bubelov.coins.model.NotificationArea

import junit.framework.Assert

import org.junit.Test

/**
 * @author Igor Bubelov
 */

class NotificationAreaRepositoryTest : BaseRobolectricTest() {
    private val repository = lazy { dependencies.value.notificationAreaRepository() }

    @Test
    fun notificationAreaRepository_NullByDefault() {
        Assert.assertTrue(repository.value.notificationArea == null)
    }

    @Test
    fun notificationAreaRepository_AreaSaved() {
        val area = NotificationArea(
                latitude = 50.0,
                longitude = 0.0,
                radius = 100.0
        )

        repository.value.notificationArea = area
        Assert.assertEquals(repository.value.notificationArea, area)
    }

    @Test
    fun notificationAreaRepository_areaCleared() {
        val area = NotificationArea(
                latitude = 50.0,
                longitude = 0.0,
                radius = 100.0
        )

        repository.value.notificationArea = area
        repository.value.notificationArea = null
        Assert.assertTrue(repository.value.notificationArea == null)
    }
}