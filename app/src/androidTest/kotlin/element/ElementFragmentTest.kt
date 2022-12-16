package element

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import elements.Element
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.btcmap.R
import org.hamcrest.Matchers.not
import org.junit.Test
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.random.Random

class ElementFragmentTest {

    @Test
    fun launch() {
        launchFragmentInContainer<ElementFragment>(
            themeResId = R.style.Theme_Material3_DynamicColors_DayNight,
        ).use { scenario ->
            val tags = mutableMapOf<String, JsonPrimitive>()

            val element = Element(
                id = "${arrayOf("node", "way", "relation").random()}:${Random.nextLong()}",
                lat = Random.nextDouble(-90.0, 90.0),
                lon = Random.nextDouble(-180.0, 180.0),
                osmJson = JsonObject(mapOf("tags" to JsonObject(tags))),
                tags = JsonObject(emptyMap()),
                createdAt = ZonedDateTime.now(ZoneOffset.UTC)
                    .minusMinutes(Random.nextLong(60 * 24 * 30)),
                updatedAt = ZonedDateTime.now(ZoneOffset.UTC)
                    .minusMinutes(Random.nextLong(60 * 24 * 30)),
                deletedAt = null,
            )

            scenario.onFragment { it.setElement(element) }
            onView(withId(R.id.address)).check(matches(not(isDisplayed())))

            tags["addr:housenumber"] = JsonPrimitive("1")
            scenario.onFragment { it.setElement(element) }
            onView(withId(R.id.address)).check(matches(isDisplayed()))
        }
    }
}