//package area
//
//import androidx.core.os.bundleOf
//import androidx.fragment.app.testing.launchFragmentInContainer
//import androidx.test.espresso.Espresso.onView
//import androidx.test.espresso.assertion.ViewAssertions.matches
//import androidx.test.espresso.matcher.ViewMatchers.*
//import androidx.test.platform.app.InstrumentationRegistry
//import app.App
//import kotlinx.coroutines.runBlocking
//import kotlinx.serialization.json.Json
//import org.btcmap.R
//import org.junit.Test
//import org.koin.android.ext.android.get
//import java.time.ZonedDateTime
//
//class AreaFragmentTest {
//
//    @Test
//    fun launch() {
//        val app =
//            InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as App
//        val areaQueries = app.get<AreaQueries>()
//
//        val area = Area(
//            id = "test",
//            tags = AreaTags(
//                mapOf(
//                    "geo_json" to Json.Default.parseToJsonElement(
//                        """
//                    {
//                      "type": "FeatureCollection",
//                      "features": [
//                        {
//                          "type": "Feature",
//                          "properties": {},
//                          "geometry": {
//                            "coordinates": [
//                              [
//                                [
//                                  22.13883023642984,
//                                  3.2294073255228852
//                                ],
//                                [
//                                  22.17600937988118,
//                                  3.284691070754846
//                                ],
//                                [
//                                  22.069218223160163,
//                                  3.305487499546132
//                                ],
//                                [
//                                  21.994596254388597,
//                                  3.2986431532554406
//                                ],
//                                [
//                                  22.011471893969883,
//                                  3.224405313608429
//                                ],
//                                [
//                                  22.13883023642984,
//                                  3.2294073255228852
//                                ]
//                              ]
//                            ],
//                            "type": "Polygon"
//                          }
//                        }
//                      ]
//                    }
//                """.trimIndent()
//                    )
//                )
//            ),
//            createdAt = ZonedDateTime.now(),
//            updatedAt = ZonedDateTime.now(),
//            deletedAt = null,
//        )
//
//        runBlocking {
//            areaQueries.insertOrReplace(listOf(area))
//        }
//
//        launchFragmentInContainer<AreaFragment>(
//            themeResId = com.google.android.material.R.style.Theme_Material3_DynamicColors_DayNight,
//            fragmentArgs = bundleOf(Pair("area_id", area.id)),
//        ).use {
//            onView(withId(R.id.toolbar)).check(matches(isDisplayed()))
//            onView(withId(R.id.progress)).check(matches(isEnabled()))
//            onView(withId(R.id.list)).check(matches(isEnabled()))
//        }
//    }
//}