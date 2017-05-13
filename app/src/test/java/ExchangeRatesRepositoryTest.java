import com.bubelov.coins.BuildConfig;
import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.repository.rate.ExchangeRatesRepository;

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
public class ExchangeRatesRepositoryTest {
    private ExchangeRatesRepository ratesRepository;

    @Before
    public void setUp() {
        Injector.INSTANCE.initMainComponent(RuntimeEnvironment.application);
        ratesRepository = Injector.INSTANCE.mainComponent().exchangeRatesRepository();
    }

    @Test
    public void exchangeRatesRepository_RatesNotEmpty() {
        Assert.assertFalse(ratesRepository.getExchangeRates().isEmpty());
    }
}
