import com.bubelov.coins.BuildConfig;
import com.bubelov.coins.api.rates.provider.BitcoinAverage;
import com.bubelov.coins.api.rates.provider.Bitstamp;
import com.bubelov.coins.api.rates.provider.Coinbase;
import com.bubelov.coins.api.rates.provider.CryptoExchange;
import com.bubelov.coins.api.rates.provider.Winkdex;
import com.bubelov.coins.dagger.Injector;

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
@Config(constants = BuildConfig.class, sdk = 21)
public class ExchangeRatesIntegrationTest {
    @Before
    public void setUp() {
        Injector.INSTANCE.initMainComponent(RuntimeEnvironment.application);
    }

    @Test
    public void testBitstamp() {
        testExchange(new Bitstamp());
    }

    @Test
    public void testCoinbase() {
        testExchange(new Coinbase());
    }

    @Test
    public void testBitcoinAverage() {
        testExchange(new BitcoinAverage());
    }

    @Test
    public void testWinkdex() {
        testExchange(new Winkdex());
    }

    private void testExchange(CryptoExchange exchange) {
        double currentRate = 0;

        try {
            currentRate = exchange.getCurrentRate();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Assert.assertTrue(currentRate > 0);
    }
}
