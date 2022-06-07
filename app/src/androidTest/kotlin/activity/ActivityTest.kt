package activity

import kotlin.test.Test
import androidx.test.core.app.launchActivity

class ActivityTest {

    @Test
    fun launch() {
        launchActivity<Activity>()
    }
}