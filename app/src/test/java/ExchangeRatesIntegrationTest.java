import com.bubelov.coins.api.rates.provider.BitcoinAverage;
import com.bubelov.coins.api.rates.provider.Bitstamp;
import com.bubelov.coins.api.rates.provider.Coinbase;
import com.bubelov.coins.api.rates.provider.CryptoExchange;
import com.bubelov.coins.api.rates.provider.Winkdex;
import com.bubelov.coins.dagger.Injector;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Igor Bubelov
 */

public class ExchangeRatesIntegrationTest {
    @Before
    public void setUp() {
        Injector.INSTANCE.initCoreComponent();
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
