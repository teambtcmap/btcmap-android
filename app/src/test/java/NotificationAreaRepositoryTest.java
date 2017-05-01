import com.bubelov.coins.BuildConfig;
import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.data.repository.area.NotificationAreaRepository;
import com.bubelov.coins.domain.NotificationArea;
import com.google.android.gms.maps.model.LatLng;

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
@Config(constants = BuildConfig.class, sdk = 23)
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
        NotificationArea area = new NotificationArea(new LatLng(50.0d, 0d), 100);
        repository.setNotificationArea(area);
        Assert.assertEquals(repository.getNotificationArea(), area);
    }

    @Test
    public void notificationAreaRepository_areaCleared() {
        NotificationArea area = new NotificationArea(new LatLng(50.0d, 0d), 100);
        repository.setNotificationArea(area);
        repository.setNotificationArea(null);
        Assert.assertTrue(repository.getNotificationArea() == null);
    }
}