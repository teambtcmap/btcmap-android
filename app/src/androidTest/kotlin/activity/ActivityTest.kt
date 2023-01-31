package activity

import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers.*
import org.btcmap.R
import org.hamcrest.Matchers.allOf
import org.junit.Test

class ActivityTest {

    @Test
    fun launch() {
        launchActivity<Activity>().use {
            Espresso
                .onView(
                    allOf(
                        withId(R.id.nav_host_fragment),
                        withParent(withId(-1)),
                    )
                )
                .check(ViewAssertions.matches(isDisplayed()))
        }
    }
}