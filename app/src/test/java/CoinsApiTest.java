import com.bubelov.coins.Constants;
import com.bubelov.coins.api.CoinsApi;
import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.model.Currency2;
import com.bubelov.coins.model.Place;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Response;

/**
 * @author Igor Bubelov
 */

public class CoinsApiTest {
    private CoinsApi api;

    @Before
    public void setUp() {
        Injector.INSTANCE.initMainComponent(null);
        api = Injector.INSTANCE.mainComponent().api();
    }

    @Test
    public void returnsCurrencies() throws Exception {
        Response<List<Currency2>> response = api.getCurrencies().execute();
        Assert.assertTrue(response.isSuccessful() && response.body().size() > 0);
    }

    @Test
    public void returnsPlaces() throws Exception {
        SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        Response<List<Place>> response = api.getPlaces(dateFormat.format(new Date(0)), 100).execute();
        Assert.assertTrue(response.isSuccessful() && response.body().size() > 0);
    }
}