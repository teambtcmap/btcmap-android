import com.bubelov.coins.BuildConfig;
import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.repository.notification.PlaceNotificationsRepository;
import com.bubelov.coins.model.PlaceNotification;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/**
 * @author Igor Bubelov
 */

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 23)
public class PlaceNotificationsRepositoryTest {
    private PlaceNotificationsRepository notificationsRepository;

    @Before
    public void setUp() {
        Injector.INSTANCE.initMainComponent(RuntimeEnvironment.application);
        notificationsRepository = Injector.INSTANCE.mainComponent().placeNotificationsRepository();
    }

    @Test
    public void placeNotificationsRepository_EmptyByDefault() {
        Assert.assertTrue(notificationsRepository.getNotifications().isEmpty());
    }

    @Test
    public void placeNotificationsRepository_AddsNotification() {
        PlaceNotification notification = addNotification();
        Assert.assertTrue(notificationsRepository.getNotifications().size() == 1);
        Assert.assertTrue(notificationsRepository.getNotifications().iterator().next().equals(notification));
    }

    @Test
    public void placeNotificationsRepository_ClearsNotifications() {
        addNotification();
        addNotification();
        Assert.assertTrue(notificationsRepository.getNotifications().size() == 2);
        notificationsRepository.clear();
        Assert.assertTrue(notificationsRepository.getNotifications().isEmpty());
    }

    private PlaceNotification addNotification() {
        PlaceNotification notification = PlaceNotification.builder()
                .placeId(1)
                .placeName("Test")
                .build();

        notificationsRepository.addNotification(notification);
        return notification;
    }
}