package activity

import androidx.test.core.app.launchActivity
import org.junit.Test

class ActivityTest {

    @Test
    fun launch() {
        launchActivity<Activity>()
    }
}