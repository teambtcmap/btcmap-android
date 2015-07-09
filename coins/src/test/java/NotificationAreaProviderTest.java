import com.bubelov.coins.BuildConfig;
import com.bubelov.coins.model.NotificationArea;
import com.bubelov.coins.provider.NotificationAreaProvider;
import com.google.android.gms.maps.model.LatLng;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/**
 * Author: Igor Bubelov
 * Date: 7/7/15 12:40 AM
 */

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, emulateSdk = 21)
public class NotificationAreaProviderTest {
    private NotificationAreaProvider provider;

    @Before
    public void initProvider() {
        provider = new NotificationAreaProvider(RuntimeEnvironment.application);
    }

    @Test
    public void testDefaultState() {
        Assert.assertEquals(provider.get(), null);
    }

    @Test
    public void testSaveArea() {
        NotificationArea area = new NotificationArea(new LatLng(51, 2));
        provider.save(area);
        Assert.assertEquals(provider.get(), area);
    }
}