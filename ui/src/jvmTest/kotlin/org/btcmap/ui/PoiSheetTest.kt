package org.btcmap.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.btcmap.ui.map.PoiInfo

/**
 * The basemap POI sheet: a tapped OpenStreetMap feature shows its name, its OSM
 * category and its coordinates, and tapping a name copies it.
 */
@OptIn(ExperimentalTestApi::class)
class PoiSheetTest {

    private val labels = PoiSheetLabels(copied = "Copied to clipboard")

    @Test
    fun showsTheNameCategoryAndCoordinates() {
        runComposeUiTest {
            setContent {
                AppTheme {
                    PoiSheet(
                        poi = PoiInfo(
                            name = "Harry's Bar",
                            classId = "bar",
                            subclass = "bar",
                            lat = 39.93412,
                            lon = 116.44235,
                        ),
                        labels = labels,
                        onDismiss = {},
                    )
                }
            }
            onNodeWithTag(POI_SHEET_NAME_TAG).assertIsDisplayed()
            onNodeWithText("Harry's Bar").assertIsDisplayed()
            onNodeWithTag(POI_SHEET_CATEGORY_TAG).assertIsDisplayed()
            onNodeWithText("Bar").assertIsDisplayed()
            onNodeWithTag(POI_SHEET_COORDINATES_TAG).assertIsDisplayed()
            onNodeWithText("39.93412, 116.44235").assertIsDisplayed()
        }
    }

    @Test
    fun withoutACategoryOnlyTheNameAndCoordinatesAreShown() {
        runComposeUiTest {
            setContent {
                AppTheme {
                    PoiSheet(
                        poi = PoiInfo(
                            name = "Somewhere",
                            classId = null,
                            subclass = null,
                            lat = 1.5,
                            lon = 2.5,
                        ),
                        labels = labels,
                        onDismiss = {},
                    )
                }
            }
            onNodeWithText("Somewhere").assertIsDisplayed()
            onNodeWithTag(POI_SHEET_LOCAL_NAME_TAG).assertDoesNotExist()
            onNodeWithTag(POI_SHEET_CATEGORY_TAG).assertDoesNotExist()
            onNodeWithTag(POI_SHEET_COORDINATES_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun showsTheLocalNameWhenTheFeatureHasOne() {
        runComposeUiTest {
            setContent {
                AppTheme {
                    PoiSheet(
                        poi = PoiInfo(
                            name = "JingZun Roast Duck",
                            classId = "restaurant",
                            subclass = "restaurant",
                            lat = 39.0,
                            lon = 116.0,
                            localName = "京尊烤鸭",
                        ),
                        labels = labels,
                        onDismiss = {},
                    )
                }
            }
            onNodeWithTag(POI_SHEET_NAME_TAG).assertIsDisplayed()
            onNodeWithText("JingZun Roast Duck").assertIsDisplayed()
            onNodeWithTag(POI_SHEET_LOCAL_NAME_TAG).assertIsDisplayed()
            // The Han name is drawn as per-character pinyin ruby, not one run.
            onNodeWithText("jing").assertIsDisplayed()
        }
    }

    @Test
    fun drawsAHanNameAsPinyinRuby() {
        runComposeUiTest {
            setContent {
                AppTheme {
                    PoiSheet(
                        poi = PoiInfo(
                            name = "北京饭店",
                            classId = "hotel",
                            subclass = "hotel",
                            lat = 39.9,
                            lon = 116.4,
                        ),
                        labels = labels,
                        onDismiss = {},
                    )
                }
            }
            onNodeWithTag(POI_SHEET_NAME_TAG).assertIsDisplayed()
            onNodeWithText("bei").assertIsDisplayed()
            onNodeWithText("jing").assertIsDisplayed()
            onNodeWithText("fan").assertIsDisplayed()
            onNodeWithText("dian").assertIsDisplayed()
        }
    }

    @Test
    fun tappingTheNameCopiesIt() {
        var copied: String? = null
        runComposeUiTest {
            setContent {
                AppTheme {
                    PoiSheet(
                        poi = PoiInfo(
                            name = "Watsons",
                            classId = "shop",
                            subclass = "chemist",
                            lat = 39.9,
                            lon = 116.4,
                            localName = "屈臣氏",
                        ),
                        labels = labels,
                        onDismiss = {},
                        onCopy = { copied = it },
                    )
                }
            }
            onNodeWithTag(POI_SHEET_NAME_TAG).performClick()
            waitForIdle()
        }
        assertEquals("Watsons", copied)
    }

    @Test
    fun tappingTheLocalNameCopiesTheHanziWithoutPinyin() {
        var copied: String? = null
        runComposeUiTest {
            setContent {
                AppTheme {
                    PoiSheet(
                        poi = PoiInfo(
                            name = "Watsons",
                            classId = "shop",
                            subclass = "chemist",
                            lat = 39.9,
                            lon = 116.4,
                            localName = "屈臣氏",
                        ),
                        labels = labels,
                        onDismiss = {},
                        onCopy = { copied = it },
                    )
                }
            }
            onNodeWithTag(POI_SHEET_LOCAL_NAME_TAG).performClick()
            waitForIdle()
        }
        assertEquals("屈臣氏", copied)
    }
}
