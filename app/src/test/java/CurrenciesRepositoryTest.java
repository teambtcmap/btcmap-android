import com.bubelov.coins.BuildConfig;
import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.data.repository.currency.CurrenciesRepository;

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
public class CurrenciesRepositoryTest {
    private CurrenciesRepository currenciesRepository;

    @Before
    public void setUp() {
        Injector.INSTANCE.initMainComponent(RuntimeEnvironment.application);
        currenciesRepository = Injector.INSTANCE.mainComponent().currenciesRepository();
    }

    @Test
    public void currenciesRepository_SyncReturnsTrue() {
        Assert.assertTrue(currenciesRepository.reloadFromNetwork());
    }

    @Test
    public void currenciesRepository_FetchesBitcoin() {
        currenciesRepository.reloadFromNetwork();
        Assert.assertTrue(currenciesRepository.getCurrency("BTC") != null);
    }
}