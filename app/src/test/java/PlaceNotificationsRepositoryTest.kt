import com.bubelov.coins.model.PlaceNotification

import org.junit.Assert
import org.junit.Test

/**
 * @author Igor Bubelov
 */

class PlaceNotificationsRepositoryTest : BaseRobolectricTest() {
    private val repository = dependencies!!.placeNotificationsRepository()

    @Test
    fun placeNotificationsRepository_EmptyByDefault() {
        Assert.assertTrue(repository.notifications.isEmpty())
    }

    @Test
    fun placeNotificationsRepository_AddsNotification() {
        val notification = addNotification()
        Assert.assertTrue(repository.notifications.size == 1)
        Assert.assertTrue(repository.notifications.iterator().next() == notification)
    }

    @Test
    fun placeNotificationsRepository_ClearsNotifications() {
        addNotification()
        addNotification()
        Assert.assertTrue(repository.notifications.size == 2)
        repository.clear()
        Assert.assertTrue(repository.notifications.isEmpty())
    }

    private fun addNotification(): PlaceNotification {
        val notification = PlaceNotification(
                placeId = 1,
                placeName = "Test"
        )

        repository.addNotification(notification)
        return notification
    }
}