package element

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import db.Element
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.btcmap.R
import org.hamcrest.Matchers.not
import org.junit.Test
import java.time.ZonedDateTime

class ElementFragmentTest {

    @Test
    fun launch() {
        launchFragmentInContainer<ElementFragment>(
            themeResId = R.style.Theme_Material3_DynamicColors_DayNight,
        ).use { scenario ->
            val tags = mutableMapOf<String, JsonPrimitive>()

            val element = Element(
                type = "node",
                id = 1L,
                lat = 0.0,
                lon = 0.0,
                timestamp = ZonedDateTime.now().toString(),
                boundsMinLat = null,
                boundsMinLon = null,
                boundsMaxLat = null,
                boundsMaxLon = null,
                tags = JsonObject(tags),
            )

            scenario.onFragment { it.setElement(element) }
            onView(withId(R.id.address)).check(matches(not(isDisplayed())))

            tags["addr:housenumber"] = JsonPrimitive("1")
            scenario.onFragment { it.setElement(element) }
            onView(withId(R.id.address)).check(matches(isDisplayed()))
        }
    }
}