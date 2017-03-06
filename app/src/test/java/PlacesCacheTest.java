import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.SystemClock;

import com.bubelov.coins.BuildConfig;
import com.bubelov.coins.PlacesCache;
import com.bubelov.coins.database.AssetDbHelper;

import junit.framework.Assert;

import org.junit.After;
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
public class PlacesCacheTest {
    private SQLiteDatabase db;

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.application;
        SQLiteOpenHelper dbHelper = new AssetDbHelper(context);
        db = dbHelper.getWritableDatabase();
    }

    @Test
    public void testCacheInit() throws Exception {
        PlacesCache placesCache = new PlacesCache(db);

        while (!placesCache.isInitialized()) {
            SystemClock.sleep(100);
        }

        Assert.assertTrue(placesCache.getSize() > 0);
    }

    @After
    public void tearDown() {
        db.close();
    }
}