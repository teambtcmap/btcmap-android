import com.bubelov.coins.BuildConfig;
import com.bubelov.coins.dao.CurrencyDAO;
import com.bubelov.coins.model.Currency;
import com.bubelov.coins.model.ExchangeRate;
import com.bubelov.coins.service.rates.provider.ExchangeRatesProviderFactory;
import com.bubelov.coins.service.rates.provider.ExchangeRatesProviderType;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

/**
 * Author: Igor Bubelov
 * Date: 10/9/15 11:52 AM
 */

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
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
        Currency btc = CurrencyDAO.query("BTC");
        Currency usd = CurrencyDAO.query("USD");

        ExchangeRate exchangeRate = null;

        try {
            exchangeRate = ExchangeRatesProviderFactory.newProvider(providerType).getExchangeRate(btc, usd);
        } catch (Exception ignored) {

        }

        Assert.assertTrue(exchangeRate != null && exchangeRate.getValue() > 0);
    }
}
