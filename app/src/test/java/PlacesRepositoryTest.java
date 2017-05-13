import com.bubelov.coins.BuildConfig;
import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.data.repository.place.PlacesDataSourceDb;
import com.bubelov.coins.data.repository.place.PlacesDataSourceMemory;
import com.bubelov.coins.data.repository.place.PlacesDataSourceNetwork;
import com.bubelov.coins.data.repository.place.PlacesRepository;
import com.bubelov.coins.domain.Place;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

/**
 * @author Igor Bubelov
 */

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 23)
public class PlacesRepositoryTest {
    private PlacesRepository placesRepository;

    private PlacesDataSourceNetwork networkSource;

    private PlacesDataSourceDb dbSource;

    private PlacesDataSourceMemory memorySource;

    @Before
    public void setUp() {
        Injector.INSTANCE.initMainComponent(RuntimeEnvironment.application);
        placesRepository = Injector.INSTANCE.mainComponent().placesRepository();
    }

    @Test
    public void placesRepository_FetchNewPlaces_NotEmpty() {
        Assert.assertFalse(placesRepository.fetchNewPlaces().isEmpty());
    }

    @Test
    public void placesRepository_SavesNewPlaces() {
        List<Place> newPlaces = placesRepository.fetchNewPlaces();
        Assert.assertFalse(newPlaces.isEmpty());
        Assert.assertNotNull(placesRepository.getPlace(newPlaces.get(0).id()));
    }
}