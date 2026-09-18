package org.btcmap.view

import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.btcmap.Activity
import org.btcmap.util.ApiRule
import org.btcmap.util.DatabaseRule
import org.btcmap.util.PreferencesRule
import org.btcmap.util.waitUntilOnMain
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IconButtonSpinTest {

    @JvmField
    @Rule
    val databaseRule = DatabaseRule()

    @JvmField
    @Rule
    val apiRule = ApiRule()

    @JvmField
    @Rule
    val preferencesRule = PreferencesRule()

    @Test
    fun spin_startsWhenVisible_andStopsWhenHidden() {
        ActivityScenario.launch(Activity::class.java).use { scenario ->
            lateinit var button: IconButton
            scenario.onActivity { activity ->
                button = IconButton(activity).apply {
                    spinning = true
                    visibility = View.GONE
                }
                (activity.window.decorView as ViewGroup).addView(button)
            }

            waitUntilOnMain { button.rotation == 0f }

            scenario.onActivity { button.visibility = View.VISIBLE }
            waitUntilOnMain { button.rotation != 0f }

            scenario.onActivity { button.visibility = View.GONE }
            waitUntilOnMain { button.rotation == 0f }

            Assert.assertEquals(0f, button.rotation, 0f)
        }
    }

    @Test
    fun animatedVisibility_fadesToShownAlpha_andHidesCompletely() {
        ActivityScenario.launch(Activity::class.java).use { scenario ->
            lateinit var button: IconButton
            scenario.onActivity { activity ->
                button = IconButton(activity).apply {
                    spinning = true
                    alpha = 0f
                    visibility = View.GONE
                }
                (activity.window.decorView as ViewGroup).addView(button)
            }

            scenario.onActivity { button.setVisibleAnimated(true) }
            waitUntilOnMain { button.visibility == View.VISIBLE && button.alpha == 1f }
            waitUntilOnMain { button.rotation != 0f }

            scenario.onActivity { button.setVisibleAnimated(false) }
            waitUntilOnMain { button.visibility == View.GONE }

            Assert.assertEquals(0f, button.alpha, 0f)
            Assert.assertEquals(0f, button.rotation, 0f)
        }
    }

    @Test
    fun animatedVisibility_survivesASyncShorterThanTheFade() {
        ActivityScenario.launch(Activity::class.java).use { scenario ->
            lateinit var button: IconButton
            scenario.onActivity { activity ->
                button = IconButton(activity).apply {
                    spinning = true
                    alpha = 0f
                    visibility = View.GONE
                }
                (activity.window.decorView as ViewGroup).addView(button)
            }

            // Ask to show, then hide a few milliseconds later as a very short
            // sync would. The queued fade must not deadlock or leave the button
            // stuck partway through the fade.
            scenario.onActivity { button.setVisibleAnimated(true) }
            Thread.sleep(10)
            scenario.onActivity { button.setVisibleAnimated(false) }

            waitUntilOnMain { button.visibility == View.GONE }

            Assert.assertEquals(0f, button.alpha, 0f)
            Assert.assertEquals(0f, button.rotation, 0f)
        }
    }
}
