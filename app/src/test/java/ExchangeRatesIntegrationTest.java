import com.bubelov.coins.api.rates.provider.BitcoinAverage;
import com.bubelov.coins.api.rates.provider.Bitstamp;
import com.bubelov.coins.api.rates.provider.Coinbase;
import com.bubelov.coins.api.rates.provider.CryptoExchange;
import com.bubelov.coins.api.rates.provider.Winkdex;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Igor Bubelov
 */

public class ExchangeRatesIntegrationTest {
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
        } catch (Exception ignored) {
            // Nothing to do here
        }

        Assert.assertTrue(currentRate > 0);
    }
}
