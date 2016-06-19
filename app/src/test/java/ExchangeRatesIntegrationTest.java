import com.bubelov.coins.model.ExchangeRate;
import com.bubelov.coins.service.rates.provider.ExchangeRatesProviderFactory;
import com.bubelov.coins.service.rates.provider.ExchangeRatesProviderType;

import org.junit.Assert;
import org.junit.Test;

/**
 * Author: Igor Bubelov
 * Date: 10/9/15 11:52 AM
 */

public class ExchangeRatesIntegrationTest {
    @Test
    public void testBitstamp() {
        testProvider(ExchangeRatesProviderType.BITSTAMP);
    }

    @Test
    public void testCoinbase() {
        testProvider(ExchangeRatesProviderType.COINBASE);
    }

    @Test
    public void testBitcoinAverage() {
        testProvider(ExchangeRatesProviderType.BITCOIN_AVERAGE);
    }

    @Test
    public void testWinkdex() {
        testProvider(ExchangeRatesProviderType.WINKDEX);
    }

    private void testProvider(ExchangeRatesProviderType providerType) {
        ExchangeRate exchangeRate = null;

        try {
            exchangeRate = ExchangeRatesProviderFactory.newProvider(providerType).getExchangeRate("BTC", "USD");
        } catch (Exception ignored) {
            // Nothing to do here
        }

        Assert.assertTrue(exchangeRate != null && exchangeRate.getValue() > 0);
    }
}
