import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.bubelov.coins.database.DatabaseHelper;

import java.util.Random;

/**
 * Author: Igor Bubelov
 * Date: 19/05/15 17:33
 */

public class DatabaseTest extends InstrumentationTestCase {
    @SmallTest
    public void testMerchantsRetrieval() {
        SQLiteOpenHelper databaseHelper = new DatabaseHelper(getInstrumentation().getTargetContext());
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        Random random = new Random(666);

        long start = System.currentTimeMillis();

        for (int i = 0; i < 500; i++) {
            Cursor cursor = db.rawQuery("select distinct m._id, m.latitude, m.longitude, m.name, m.description from merchants as m join currencies_merchants as mc on m._id = mc.merchant_id join currencies c on c._id = mc.currency_id where (latitude between ? and ?) and (longitude between ? and ?) and c.show_on_map = 1",
                    new String[] { String.valueOf(-180.0f + random.nextFloat() * 360.0f), String.valueOf(-180.0f + random.nextFloat() * 360.0f), String.valueOf(-180.0f + random.nextFloat() * 360.0f), String.valueOf(-180.0f + random.nextFloat() * 360.0f) });

            cursor.moveToNext();
            cursor.close();
        }

        long execTime = System.currentTimeMillis() - start;
        //Log.d("Test", "Execution time: " + execTime);
        //Log.d("Test", "Avg query: " + (float) execTime / 10000.0f);

        assertTrue(true);
    }
}
