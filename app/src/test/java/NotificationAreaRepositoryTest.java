import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.repository.area.NotificationAreaRepository;
import com.bubelov.coins.model.NotificationArea;

import junit.framework.Assert;

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
@Config(manifest=Config.NONE, shadows = NetworkSecurityPolicyShadow.class)
public class NotificationAreaRepositoryTest {
    private NotificationAreaRepository repository;

    @Before
    public void setUp() {
        Injector.INSTANCE.initMainComponent(RuntimeEnvironment.application);
        repository = Injector.INSTANCE.mainComponent().notificationAreaRepository();
    }

    @Test
    public void notificationAreaRepository_NullByDefault() {
        Assert.assertTrue(repository.getNotificationArea() == null);
    }

    @Test
    public void notificationAreaRepository_AreaSaved() {
        NotificationArea area = NotificationArea.builder()
                .latitude(50)
                .longitude(0)
                .radius(100)
                .build();

        repository.setNotificationArea(area);
        Assert.assertEquals(repository.getNotificationArea(), area);
    }

    @Test
    public void notificationAreaRepository_areaCleared() {
        NotificationArea area = NotificationArea.builder()
                .latitude(50)
                .longitude(0)
                .radius(100)
                .build();

        repository.setNotificationArea(area);
        repository.setNotificationArea(null);
        Assert.assertTrue(repository.getNotificationArea() == null);
    }
}