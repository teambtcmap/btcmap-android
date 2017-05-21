import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.repository.currency.CurrenciesRepository;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.IOException;

/**
 * @author Igor Bubelov
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE, shadows = NetworkSecurityPolicyShadow.class)
public class CurrenciesRepositoryTest {
    private CurrenciesRepository currenciesRepository;

    @Before
    public void setUp() {
        Injector.INSTANCE.initMainComponent(RuntimeEnvironment.application);
        currenciesRepository = Injector.INSTANCE.mainComponent().currenciesRepository();
    }

    @Test
    public void currenciesRepository_SyncReturnsTrue() throws IOException {
        currenciesRepository.reloadFromApi();
    }

    @Test
    public void currenciesRepository_FetchesBitcoin() throws IOException {
        currenciesRepository.reloadFromApi();
        Assert.assertTrue(currenciesRepository.getCurrency("BTC") != null);
    }
}