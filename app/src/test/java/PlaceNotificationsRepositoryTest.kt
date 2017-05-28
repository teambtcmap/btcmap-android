import com.bubelov.coins.model.PlaceNotification

import org.junit.Assert
import org.junit.Test

/**
 * @author Igor Bubelov
 */

class PlaceNotificationsRepositoryTest : BaseRobolectricTest() {
    private val repository = lazy { dependencies.value.placeNotificationsRepository() }

    @Test
    fun placeNotificationsRepository_EmptyByDefault() {
        Assert.assertTrue(repository.value.notifications.isEmpty())
    }

    @Test
    fun placeNotificationsRepository_AddsNotification() {
        val notification = addNotification()
        Assert.assertTrue(repository.value.notifications.size == 1)
        Assert.assertTrue(repository.value.notifications.iterator().next() == notification)
    }

    @Test
    fun placeNotificationsRepository_ClearsNotifications() {
        addNotification()
        addNotification()
        Assert.assertTrue(repository.value.notifications.size == 2)
        repository.value.clear()
        Assert.assertTrue(repository.value.notifications.isEmpty())
    }

    private fun addNotification(): PlaceNotification {
        val notification = PlaceNotification(
                placeId = 1,
                placeName = "Test"
        )

        repository.value.addNotification(notification)
        return notification
    }
}