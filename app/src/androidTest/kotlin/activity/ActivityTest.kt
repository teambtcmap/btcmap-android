package activity

import androidx.test.core.app.launchActivity
import kotlin.test.Test

class ActivityTest {

    @Test
    fun launch() {
        launchActivity<Activity>()
    }
}