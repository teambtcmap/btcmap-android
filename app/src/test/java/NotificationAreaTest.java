import com.bubelov.coins.BuildConfig;
import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.data.DataManager;
import com.bubelov.coins.data.model.NotificationArea;
import com.google.android.gms.maps.model.LatLng;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/**
 * Author: Igor Bubelov
 */

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class NotificationAreaTest {
    private DataManager dataManager;

    @Before
    public void setUp() {
        Injector.INSTANCE.initMainComponent(RuntimeEnvironment.application);
        dataManager = Injector.INSTANCE.mainComponent().dataManager();
    }

    @Test
    public void test_noAreaByDefault() {
        Assert.assertTrue(dataManager.preferences().getNotificationArea() == null);
    }

    @Test
    public void test_areaSaved() {
        NotificationArea area = new NotificationArea(new LatLng(50.0d, 0d), 100);
        dataManager.preferences().setNotificationArea(area);
        Assert.assertEquals(dataManager.preferences().getNotificationArea(), area);
    }

    @Test
    public void test_areaCleared() {
        NotificationArea area = new NotificationArea(new LatLng(50.0d, 0d), 100);
        dataManager.preferences().setNotificationArea(area);
        dataManager.preferences().setNotificationArea(null);
        Assert.assertTrue(dataManager.preferences().getNotificationArea() == null);
    }
}